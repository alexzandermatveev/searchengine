package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.PageInfoAfterSearching;
import searchengine.dto.statistics.ResponseForSearching;
import searchengine.dto.statistics.ShortInfo;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Сервис по индексации страниц
 */
@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final LuceneMorphology luceneMorphEn;
    private final LuceneMorphology luceneMorphRu;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final SitesList sites;

    private ForkJoinPool forkJoinPool = new ForkJoinPool();
    private ExecutorService executorService = Executors.newCachedThreadPool();

    @Getter
    private static final AtomicBoolean stopFlag = new AtomicBoolean(false);
    @Getter
    private static final AtomicBoolean stopCheckLemmasOnSite = new AtomicBoolean(false);

    @Override
    public ShortInfo indexingPage(String reindexPage) {
        reindexPage = URLDecoder.decode(reindexPage, StandardCharsets.UTF_8).replace("url=", "");
        Pattern pattern = Pattern.compile("https?://(?:www\\.)?[^/]+");
        Matcher matcher = pattern.matcher(reindexPage);

        if (matcher.find()) {
            String mainSiteName = matcher.group(); // Название сайта с www и http(s)://
            Optional<SiteModel> potentionalSite = siteRepository.findAll().stream()
                    .filter(site -> site.getUrl().contains(normalizeUrl(mainSiteName)))
                    .findFirst();
            if (potentionalSite.isPresent()) {
                if (pageRepository.findByPath(reindexPage) != null) {
                    PageModel p = pageRepository.findByPath(reindexPage);
                    try {
                        Connection.Response response = Jsoup.connect(p.getPath())
                                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                                .referrer("http://www.google.com")
                                .execute();
                        p.setCode(response.statusCode());
                        p.setContent(response.parse().html());
                    } catch (IOException e) {
                        return new ShortInfo(false, "Страница не доступна");
                    }
                    pageRepository.save(p);
                    return new ShortInfo(true, "Страница проиндексирована");
                } else {
                    PageModel p = new PageModel();
                    p.setSite(potentionalSite.get());
                    p.setPath(reindexPage);
                    try {
                        Connection.Response response = Jsoup.connect(p.getPath())
                                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                                .referrer("http://www.google.com")
                                .execute();
                        p.setCode(response.statusCode());
                        p.setContent(response.parse().html());
                        SiteModel pSite = p.getSite();
                        List<PageModel> pageModels = pSite.getPage();
                        pageModels.add(p);
                        pSite.setPage(pageModels);
                        siteRepository.save(pSite);
                    } catch (IOException e) {
                        return new ShortInfo(false, "Страница не доступна");
                    }
                    pageRepository.save(p);
                    return new ShortInfo(true, "Страница проиндексирована");
                }
            }
        }
        return new ShortInfo(false, "Данная страница находится за пределами сайтов, \n" +
                "указанных в конфигурационном файле");
    }

    @Override
    @Transactional
    public ShortInfo startIndexing() {

        if (forkJoinPool.getActiveThreadCount() > 0) {
            return new ShortInfo(false, "Индексация уже запущена");
        }

        for (Site site : sites.getSites()) {
            SiteModel dbSite = siteRepository.findByUrl(site.getUrl());
            if (dbSite == null) {
                SiteModel siteModel = new SiteModel();
                siteModel.setUrl(site.getUrl());
                siteModel.setName(site.getName());
                siteModel.setStatus(SiteStatus.INDEXING);
                siteModel.setLastError("");
                siteModel.setStatusTime(Instant.now());
                siteRepository.save(siteModel);
                forkJoinPool.execute(new PagesFinder(siteModel.getUrl(), pageRepository,
                        siteRepository, siteModel, true,
                        luceneMorphEn, luceneMorphRu, indexRepository, lemmaRepository));
            } else {
                dbSite.setStatus(SiteStatus.INDEXING);
                dbSite.setLastError("");
                dbSite.setStatusTime(Instant.now());
                siteRepository.save(dbSite);
                forkJoinPool.execute(new PagesFinder(dbSite.getUrl(), pageRepository,
                        siteRepository, dbSite, true,
                        luceneMorphEn, luceneMorphRu, indexRepository, lemmaRepository));
            }
        }
        return new ShortInfo(true, "indexing started");
    }

    /**
     * Регулярное выражение для извлечения только домена (например, example.com)
     * @param url
     * @return {@link String}
     */
    private String normalizeUrl(String url) {
        Pattern pattern = Pattern.compile("^(https?://)?(www\\.)?([a-zA-Z0-9-]+\\.[a-zA-Z]{2,})");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(3);
        }
        return url;
    }


    @Override
    public ShortInfo stopIndexing() {
        stopFlag.set(true);
        return new ShortInfo(true, "stop indexing by user");
    }

    @Override
    public ShortInfo stopIndexingLemmas() {
        stopCheckLemmasOnSite.set(true);
        return new ShortInfo(true, "stop indexing lemmas on site by user");
    }

    @Override
    public ShortInfo pagesForSiteIndexingLemmas(SiteModel targetSite) {

        if (siteRepository.findByUrl(targetSite.getUrl()).getPage().isEmpty()) {
            return new ShortInfo(false, "У указанного сайта нет проиндексированных страниц");
        }
        for (PageModel page : siteRepository.findByUrl(targetSite.getUrl()).getPage()) {
            Future<ShortInfo> future = executorService.submit(new MorfologyService(luceneMorphEn, luceneMorphRu, indexRepository,
                    lemmaRepository, pageRepository, page));
            try {
                System.out.println("Future output: ");
                System.out.println(future.isDone());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return new ShortInfo(true, "started lemma indexing on " + targetSite.getUrl());
    }

    @Override
    public ResponseEntity<ResponseForSearching> findingOnPagesSmth(String query, String site, Integer offset, Integer limit) {
        List<PageModel> pagesWhereToFind;
        HashMap<String, Long> lemmasFromQuery;
        final int frequencyLimit = 10000;
        if (site != null && !site.isEmpty()) {
            SiteModel siteModel = siteRepository.findByUrl(site);
            if (siteModel == null) {
                return new ResponseEntity<>(new ResponseForSearching(false, 0L, List.of()), HttpStatus.NOT_FOUND);
            }
            pagesWhereToFind = siteModel.getPage();
        } else {
            pagesWhereToFind = pageRepository.findAll();
        }

        MorfologyService morfologyService = new MorfologyService(luceneMorphEn, luceneMorphRu, indexRepository, lemmaRepository, pageRepository, new PageModel());
        lemmasFromQuery = morfologyService.morphologyForms(query);

        List<Lemma> qLemmas = lemmasFromQuery.keySet().stream()
                .map(lemmaRepository::findByLemma)
                .filter(lemma -> lemma != null && lemma.getFrequency() <= frequencyLimit)
                .sorted(Comparator.comparingLong(Lemma::getFrequency))
                .toList();

        if (qLemmas.isEmpty()) {
            return new ResponseEntity<>(new ResponseForSearching(false, 0L, List.of()), HttpStatus.NOT_FOUND);
        }

        Set<PageModel> pagesWhereLemmasAre = new HashSet<>(pagesWhereToFind);

        for (Lemma lemma : qLemmas) {
            Set<PageModel> lastNotEmptyList = new HashSet<>(pagesWhereLemmasAre);
            List<PageModel> pagesForLemma = lemma.getIndexList().stream()
                    .map(Index::getPage)
                    .filter(pagesWhereLemmasAre::contains)
                    .collect(Collectors.toList());
            pagesWhereLemmasAre.retainAll(pagesForLemma);
            if (pagesWhereLemmasAre.isEmpty()) {
                pagesWhereLemmasAre = lastNotEmptyList;
                break;
            }
        }

        Map<PageModel, Double> absRelevance = pagesWhereLemmasAre.stream()
                .collect(Collectors.toMap(
                        page -> page,
                        page -> page.getIndexList().stream()
                                .mapToDouble(Index::getRank)
                                .sum()
                ));

        double maxRank = absRelevance.values().stream().max(Double::compare).orElse(0.0);
        absRelevance.replaceAll((page, rank) -> rank / maxRank);

        List<PageInfoAfterSearching> pageInfoList = absRelevance.entrySet().stream()
                .sorted(Map.Entry.<PageModel, Double>comparingByValue().reversed())
                .map(entry -> {
                    PageModel page = entry.getKey();
                    PageInfoAfterSearching pageInfo = new PageInfoAfterSearching();
                    pageInfo.setUri(page.getPath().replace(page.getSite().getUrl(), ""));
                    pageInfo.setSite(page.getSite().getUrl());
                    pageInfo.setRelevance(entry.getValue());
                    pageInfo.setTitle(Jsoup.parse(page.getContent()).title());
                    pageInfo.setSnippet(makeSnippet(page.getContent(), query));
                    pageInfo.setSiteName(page.getSite().getName());
                    return pageInfo;
                })
                .collect(Collectors.toList());

        if (pageInfoList.isEmpty()) {
            return new ResponseEntity<>(new ResponseForSearching(false, 0L, List.of()), HttpStatus.OK);
        }

        ResponseForSearching response = new ResponseForSearching();
        response.setResult(true);
        response.setCount((long) pageInfoList.size());

        if (offset != null && limit != null && offset >= 0 && limit > 0 && offset < pageInfoList.size()) {
            int toIndex = Math.min(offset + limit, pageInfoList.size());
            response.setData(pageInfoList.subList(offset, toIndex));
        } else {
            response.setData(pageInfoList);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    public String makeSnippet(String html, String originalQuery) {
        String[] splitedQuery = originalQuery.split("\\s+");
        Document document = Jsoup.parse(html);
        int snippetLength = 200;
        Map<String, String> queryLemmas = new HashMap<>();
        for (String queryWord : splitedQuery) {
            if (luceneMorphRu.checkString(queryWord)) {
                queryLemmas.put(queryWord, luceneMorphRu.getNormalForms(queryWord).get(0));
            } else if (luceneMorphEn.checkString(queryWord)) {
                queryLemmas.put(queryWord, luceneMorphEn.getNormalForms(queryWord).get(0));
            } else {
                queryLemmas.put(queryWord, queryWord);
            }
        }

        for (Element element : document.getAllElements()) {
            String elementText = element.ownText();
            String[] wordsInText = elementText.split("\\s+");
            for (String wordInText : wordsInText) {
                if (wordInText.isEmpty()) {
                    continue;
                }
                String normalizedWord = wordInText.toLowerCase();
                String lemma;

                if (luceneMorphRu.checkString(normalizedWord)) {
                    lemma = luceneMorphRu.getNormalForms(normalizedWord).get(0);
                } else if (luceneMorphEn.checkString(normalizedWord)) {
                    lemma = luceneMorphEn.getNormalForms(normalizedWord).get(0);
                } else {
                    lemma = normalizedWord;
                }

                if (lemma != null && queryLemmas.containsValue(lemma)) {
                    String highlightedText = elementText.replaceAll("(?i)\\b" + wordInText + "\\b", "<b>" + wordInText + "</b>");

                    int wordIndex = highlightedText.toLowerCase().indexOf(wordInText.toLowerCase());
                    int start = Math.max(0, wordIndex - snippetLength / 2);
                    int end = Math.min(elementText.length(), wordIndex + wordInText.length() + snippetLength / 2);

                    return highlightedText.substring(start, end);
                }
            }
        }
        return null;
    }

    @PreDestroy
    public void shutdownExecutor() {
        executorService.shutdown();
    }

}
