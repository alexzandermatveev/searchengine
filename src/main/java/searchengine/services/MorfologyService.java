package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.transaction.annotation.Transactional;
import searchengine.dto.statistics.ShortInfo;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.PageModel;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;

import java.util.*;
import java.util.concurrent.Callable;

/**
 * Сервис по лемматизации слов
 */
public class MorfologyService implements Callable<ShortInfo> {

    private final LuceneMorphology luceneMorphEn;
    private final LuceneMorphology luceneMorphRu;
    private final IndexRepository indexRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;

    private PageModel siteForIndexing;

    public MorfologyService(LuceneMorphology luceneMorphEn, LuceneMorphology luceneMorphRu,
                            IndexRepository indexRepository, LemmaRepository lemmaRepository,
                            PageRepository pageRepository, PageModel siteForIndexing) {
        this.luceneMorphEn = luceneMorphEn;
        this.luceneMorphRu = luceneMorphRu;
        this.indexRepository = indexRepository;
        this.lemmaRepository = lemmaRepository;
        this.pageRepository = pageRepository;
        this.siteForIndexing = siteForIndexing;
    }

    private final Set<String> stopPosTags = new HashSet<>(Arrays.asList(
            "ПРЕДЛ", "СОЮЗ", "МЕЖД", "ЧАСТ",
            "PRCL", "CONJ", "PREP", "INTJ"
    ));

    static List<String> prepareString(String someString) {
        someString = someString.replaceAll("[,.?{}'<>\\[\\](\")!—-]", "");
        someString = someString.replaceAll("</?[a-z]*>", "");
        someString = someString.replaceAll("[a-z]+=", "");
        List<String> words = List.of(someString.split("\\s"));
        return words.stream().filter(f -> !f.isEmpty()).toList();
    }

    static String prepareSingleWord(String someString) {
        someString = someString.replaceAll("[,.?{}'\\[\\](\")!—-]", "");
        return someString.substring(0, someString.indexOf("|"));
    }

    /**
     * Проверяем, является ли строка словом и
     * принадлежит язык слова выбранному словарю
     * если слово не подходит - возвращаем пустой список
     *
     * @param word Слово для проверки
     * @return {@code HashMap<String, Long>} Mao из Лемм и частоты их повторения
     */
    public HashMap<String, Long> morphologyForms(String word) {

        List<String> stringList = prepareString(word);
        HashMap<String, Long> targetWords = new HashMap<>();
        for (String underString : stringList) {
            boolean flag = false;

            if (!luceneMorphEn.checkString(underString)) {
                if (!luceneMorphRu.checkString(underString)) {
                } else {
                    String firstWord = luceneMorphRu.getMorphInfo(underString).get(0);
                    for (String notAimPartOfSpeech : stopPosTags) {
                        if (firstWord.contains(notAimPartOfSpeech)) {
                            flag = true;
                            break;
                        }
                    }
                    if (!flag) {
                        firstWord = prepareSingleWord(firstWord);
                        if (targetWords.containsKey(firstWord)) {
                            targetWords.put(firstWord, targetWords.get(firstWord) + 1);
                        } else {
                            targetWords.put(firstWord, 1L);
                        }
                    }
                }
            } else {
                try {
                    String firstWord = luceneMorphEn.getMorphInfo(underString).get(0);
                    for (String notAimPartOfSpeech : stopPosTags) {
                        if (firstWord.contains(notAimPartOfSpeech)) {
                            flag = true;
                            break;
                        }
                    }
                    if (!flag) {
                        firstWord = prepareSingleWord(firstWord);
                        if (targetWords.containsKey(firstWord)) {
                            targetWords.put(firstWord, targetWords.get(firstWord) + 1);
                        } else {
                            targetWords.put(firstWord, 1L);
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
        targetWords.remove("");
        return targetWords;
    }

    @Transactional
    public ShortInfo indexPage(PageModel page) {
        page = pageRepository.findById(page.getId()).orElseThrow(() -> new RuntimeException("no page"));
        List<Index> indexesByPage = page.getIndexList();
        List<Lemma> lemmasByIndex = new ArrayList<>();
        indexesByPage.forEach(index -> {

            Lemma innerLemma = index.getLemma();
            List<Index> innerIndexes = innerLemma.getIndexList();
            innerIndexes.remove(index);
            innerLemma.setIndexList(innerIndexes);
            if (!lemmasByIndex.contains(innerLemma)) {
                lemmasByIndex.add(innerLemma);
            }
        });
        indexRepository.deleteAll(indexesByPage);
        lemmaRepository.saveAll(lemmasByIndex.stream()
                .distinct()
                .peek(lemma -> lemma.setFrequency(lemma.getFrequency() - 1L))
                .toList());
        HashMap<String, Long> map = morphologyForms(page.getContent());
        for (String key : map.keySet()) {
            System.out.println("inner check");
            Lemma lemmaDB = lemmaRepository.findWithIndexListByLemma(key);
            if (lemmaDB == null) {
                lemmaDB = new Lemma();
                Index newIndex = new Index();
                lemmaDB.setLemma(key);
                lemmaDB.setFrequency(1L);
                newIndex.setLemma(lemmaDB);
                newIndex.setPage(page);
                newIndex.setRank(map.get(key));

                lemmaRepository.save(lemmaDB);
                indexRepository.save(newIndex);
            } else {
                boolean isIndexInLemmaList = false;
                for (Index i : lemmaDB.getIndexList()) {
                    if (i.getPage().getPath().equals(page.getPath())) {
                        i.setRank(map.get(key));
                        indexRepository.save(i);
                        isIndexInLemmaList = true;
                        break;
                    }
                }
                if (!isIndexInLemmaList) {
                    lemmaDB.setFrequency(lemmaDB.getFrequency() + 1L);
                    Index newI = new Index();
                    newI.setPage(page);
                    newI.setLemma(lemmaDB);
                    newI.setRank(map.get(key));
                    indexRepository.save(newI);
                    lemmaRepository.save(lemmaDB);
                }
            }
        }
        if (map.isEmpty()) {
            return new ShortInfo(false, "wrong content");
        }
        return new ShortInfo(true, "переиндексация завершена");
    }

    public ShortInfo checkSite(PageModel page) {
        PageModel pageDB = pageRepository.findByPath(page.getPath());
        if (pageDB == null) {
            return new ShortInfo(false, "Данная страница находится за пределами сайтов, \n" +
                    "указанных в конфигурационном файле\n");
        }
        return indexPage(pageDB);
    }

    @Override
    public ShortInfo call() throws Exception {
        System.out.println("вызов call");
        if (IndexingServiceImpl.getStopCheckLemmasOnSite().get()) {
            return new ShortInfo(false, "stopped");
        }
        siteForIndexing = pageRepository.findByPath(siteForIndexing.getPath());
        System.out.println("вызов call завершен");
        return checkSite(siteForIndexing);
    }
}
