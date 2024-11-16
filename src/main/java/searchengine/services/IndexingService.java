package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.statistics.ShortInfo;
import searchengine.model.SiteModel;

/**
 * Интерфейс сервиса индексации страниц
 */
public interface IndexingService {

    /**
     * Запускает полную индексацию всех сайтов
     *
     * @return {@link ShortInfo} Короткий формат ответа
     */
    ShortInfo startIndexing();

    /**
     * Индексирует переданную страницу
     *
     * @param reindexPage url страницы для индексирования/переиндексирования
     * @return {@link ShortInfo} Короткий формат ответа
     */
    ShortInfo indexingPage(String reindexPage);

    /**
     * Останавливает полное индексирование
     *
     * @return {@link ShortInfo} Короткий формат ответа
     */
    ShortInfo stopIndexing();

    /**
     * Останавливает индексацию лемм
     *
     * @return {@link ShortInfo} Короткий формат ответа
     */
    ShortInfo stopIndexingLemmas();

    /**
     * Запуск индексации лемм для определенной страницы
     *
     * @param targetSite Сайт, на котором требуется проиндексировать леммы
     * @return {@link ShortInfo} Короткий формат ответа
     */
    ShortInfo pagesForSiteIndexingLemmas(SiteModel targetSite);

    /**
     * Осуществляет поиск страниц по переданному поисковому запросу
     *
     * @param query  Что нужно найти
     * @param site   Сайт, где нужно найти
     * @param offset Сдвиг от начала списка результатов
     * @param limit  Количество результатов, которое необходимо вывести
     * @return {@link ResponseEntity}
     */
    ResponseEntity<?> findingOnPagesSmth(String query, String site, Integer offset,
                                         Integer limit);
}
