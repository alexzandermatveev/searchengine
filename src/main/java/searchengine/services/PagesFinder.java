package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatus;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;

/**
 * Класс {@link PagesFinder} отвечает за рекурсивный обход страниц сайта,
 * нахождение новых uri и последующей индексацией содержимого страниц
 */
public class PagesFinder extends RecursiveTask<Void> {

    private final boolean isRootTask;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    private final String SITE_ORIGINAL;
    private final SiteModel siteModel;
    private int pageCode;
    private String context;
    private final String url;

    private HashSet<String> hashLinks;
    /**
     * {@link ConcurrentHashMap}, в котором хранятся проверенные всеми экземплярами {@link PagesFinder} ссылки со страниц
     */
    private static ConcurrentHashMap<String, String> checkedLinks;

    private final LuceneMorphology luceneMorphEn;
    private final LuceneMorphology luceneMorphRu;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;


    private void init() {
        checkedLinks = new ConcurrentHashMap<>();
        hashLinks = getHashLinks();
    }

    public PagesFinder(String url, PageRepository pageRepository,
                       SiteRepository siteRepository, SiteModel siteModel, boolean isRootTask,
                       LuceneMorphology luceneMorphEn, LuceneMorphology luceneMorphRu,
                       IndexRepository indexRepository, LemmaRepository lemmaRepository) {
        this.pageRepository = pageRepository;
        this.siteRepository = siteRepository;
        this.url = url;
        this.isRootTask = isRootTask;
        this.siteModel = siteModel;
        SITE_ORIGINAL = siteModel.getUrl();

        this.luceneMorphEn = luceneMorphEn;
        this.luceneMorphRu = luceneMorphRu;
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
    }

    /**
     * Проверяет является ли переданная ссылка в {@link PagesFinder} принадлежащей материнскому сайту,
     * является ли ссылкой вообще и была ли уже рассмотрена данная ссылка в текущей сессии потоков
     *
     * @return {@code HashSet<String>} Список уникальных ссылок, полученных с просмотренной страницы
     */
    private HashSet<String> getHashLinks() {
        HashSet<String> hashLinks = new HashSet<>();
        if (IndexingServiceImpl.getStopFlag().get()) {
            return hashLinks;
        }

        try {
            Connection.Response response;
            if (checkedLinks.contains(url) || checkedLinks.contains(SITE_ORIGINAL.concat(url))) {
                throw new IllegalArgumentException("checked link");
            } else {
                response = Jsoup.connect(
                                !url.matches("http[s]*://[^,\\s]+") ? SITE_ORIGINAL.concat(url) : url)
                        .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .referrer("http://www.google.com")
                        .execute();
            }
            checkedLinks.put(url, url);
            pageCode = response.statusCode();

            Document page = response.parse();
            context = page.html();
            setPageModel();

            Thread.sleep((int) (Math.random() * 50 + 100));
            Elements links = page.select("a");
            hashLinks.addAll(links.stream()
                    .map(p -> p.attr("href"))
                    .distinct()
                    .filter(p -> !p.matches("http[s]*://[^,\\s]+") || !p.contains("/"))
//                    .filter(p -> !checkedLinks.contains(p))
                    .toList());
        } catch (Exception e) {
            checkedLinks.put(url, url);
            e.printStackTrace();
        }
        return hashLinks;
    }

    /**
     * Создается дочерний {@link PagesFinder}, каждый из которых будет рассматривать одну переданную ссылку
     *
     * @param linkList Список уникальных ссылок, полученных при парсинге страницы в {@link PagesFinder#getHashLinks()}
     * @return {@code List<PagesFinder>} Список экземпляров  {@link PagesFinder}
     */
    private List<PagesFinder> createSubtasks(HashSet<String> linkList) {
        return linkList.stream()
                .map(link -> new PagesFinder(link, pageRepository, siteRepository, siteModel, false,
                        luceneMorphEn, luceneMorphRu, indexRepository, lemmaRepository))
                .toList();
    }

    @Override
    protected Void compute() {
        if (IndexingServiceImpl.getStopFlag().get()) {
            return null;
        }
        init();
        List<PagesFinder> taskList = createSubtasks(hashLinks);
        for (PagesFinder finder : taskList) {
            if(IndexingServiceImpl.getStopFlag().get()){
                break;
            }
            finder.fork();
        }
        for (PagesFinder task : taskList) {
            if(IndexingServiceImpl.getStopFlag().get()){
                break;
            }
            task.join();
        }
        if (isRootTask && IndexingServiceImpl.getStopFlag().get()) {
            SiteModel site = siteRepository.findByUrl(url);
            site.setStatus(SiteStatus.FAILED);
            site.setLastError("interrupted by user");
            siteRepository.save(site);
        } else if (isRootTask && !IndexingServiceImpl.getStopFlag().get()) {
            SiteModel site = siteRepository.findByUrl(url);
            site.setStatus(SiteStatus.INDEXED);
            site.setLastError("");
            siteRepository.save(site);
        }
        return null;
    }

    @Transactional
    private void setPageModel() {
        PageModel page = pageRepository.findByPath(!url.matches("http[s]*://[^,\\s]+") ? SITE_ORIGINAL.concat(url) : url);
        if (page == null) {
            page = new PageModel();
            page.setCode(pageCode == 0 ? 404 : pageCode);
            page.setPath(!url.matches("http[s]*://[^,\\s]+") ? SITE_ORIGINAL.concat(url) : url);
            page.setContent(context);
            SiteModel site = siteRepository.findByUrl(SITE_ORIGINAL);
            page.setSite(site);
            page = pageRepository.saveAndFlush(page);
        } else {
            page.setContent(context);
            page.setCode(pageCode == 0 ? 404 : pageCode);
            page = pageRepository.saveAndFlush(page);
        }
        indexingLemmas(page);
    }

    @Transactional
    private void indexingLemmas(PageModel targetPage) {
        new MorfologyService(luceneMorphEn, luceneMorphRu, indexRepository,
                lemmaRepository, pageRepository, targetPage).indexPage(targetPage);
    }
}


