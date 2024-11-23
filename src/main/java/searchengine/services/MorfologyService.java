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
import java.util.function.Function;
import java.util.stream.Collectors;

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
        indexRepository.deleteAll(indexesByPage);
        HashMap<String, Long> map = morphologyForms(page.getContent());

        if (map.isEmpty()) {
            return new ShortInfo(false, "Invalid content");
        }

        Set<Lemma> lemmasInDB = new HashSet<>(lemmaRepository.findAllByLemmaIn(map.keySet()));
        Map<String, Lemma> lemmasMap = lemmasInDB.stream()
                .collect(Collectors.toMap(Lemma::getLemma, Function.identity()));

        List<Index> newIndexes = new ArrayList<>();
        List<Lemma> updatedLemmas = new ArrayList<>();
        List<Lemma> newLemmas = new ArrayList<>();

        for (Map.Entry<String, Long> entry : map.entrySet()) {
            String lemmaKey = entry.getKey();
            Long rank = entry.getValue();

            Lemma lemma = lemmasMap.get(lemmaKey);
            if (lemma == null) {
                lemma = new Lemma();
                lemma.setLemma(lemmaKey);
                lemma.setFrequency(1L);

                Index newIndex = new Index(null, lemma, page, rank);
                newLemmas.add(lemma);
                newIndexes.add(newIndex);
            } else {
                lemma.setFrequency(lemma.getFrequency() + 1L);
                updatedLemmas.add(lemma);

                boolean indexExists = indexRepository.existsByPageAndLemma(page, lemma); //lemma.getIndexList().stream().anyMatch(index -> index.getPage().getId() == thePageId);

                if (!indexExists) {
                    Index newIndex = new Index(null, lemma, page, rank);
                    newIndexes.add(newIndex);
                } else {
                    List<Index> indexes = indexRepository.findAllByPageAndLemma(page, lemma).stream()
                            .peek(index -> index.setRank(rank))
                            .toList();
                    newIndexes.addAll(indexes);
                    updatedLemmas.add(lemma);
                }
            }
        }
        if (!newLemmas.isEmpty()) {
            lemmaRepository.saveAll(newLemmas);
        }
        if (!updatedLemmas.isEmpty()) {
            lemmaRepository.saveAll(updatedLemmas);
        }
        if (!newIndexes.isEmpty()) {
            indexRepository.saveAll(newIndexes);
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
    public ShortInfo call() {
        if (IndexingServiceImpl.getStopCheckLemmasOnSite().get()) {
            return new ShortInfo(false, "stopped");
        }
        siteForIndexing = pageRepository.findByPath(siteForIndexing.getPath());
        return checkSite(siteForIndexing);
    }
}
