package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.PageInfoAfterSearching;
import searchengine.dto.statistics.ResponseForSearching;
import searchengine.dto.statistics.ShortInfo;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.SiteModel;
import searchengine.services.IndexingServiceImpl;
import searchengine.services.StatisticsServiceImpl;

/**
 * Основной контроллер для обработки запросов клиента к эндпоинтам
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final StatisticsServiceImpl statisticsService;
    private final IndexingServiceImpl indexingService;

    /**
     * Возвращает статистику
     *
     * @return {@link ResponseEntity<StatisticsResponse>} Детальная статистика по всем сайтам
     */
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    /**
     * Запускает полную индексацию всех сайтов указанных в конфигурации
     *
     * @return {@link ResponseEntity<ShortInfo>} Короткий формат ответа
     */
    @GetMapping("/startIndexing")
    public ResponseEntity<ShortInfo> startIndexing() {
        ShortInfo info = indexingService.startIndexing();
        return new ResponseEntity<ShortInfo>(
                info,
                info.isResult() ? HttpStatus.OK : HttpStatus.BAD_REQUEST
        );
    }

    /**
     * Останавливает индексирование сайтов
     *
     * @return {@link ResponseEntity<ShortInfo>} Короткий формат ответа
     */
    @GetMapping("/stopIndexing")
    public ResponseEntity<ShortInfo> stopIndexing() {
        ShortInfo info = indexingService.stopIndexing();
        return new ResponseEntity<ShortInfo>(
                info,
                info.isResult() ? HttpStatus.OK : HttpStatus.BAD_REQUEST
        );
    }

    /**
     * Добавляет в индекс или обновляет отдельную страницу
     *
     * @param url Путь до страницы, которую нужно проиндексировать
     * @return {@link ResponseEntity<ShortInfo>} Короткий формат ответа
     */
    @PostMapping("/indexPage")
    public ResponseEntity<ShortInfo> indexPage(@RequestParam String url) {
        return new ResponseEntity<ShortInfo>(indexingService.indexingPage(url),
                HttpStatus.OK);
    }

    /**
     * Отдельный перезапуск индексирования лемм на всех страницах сайта или
     *
     * @param siteModel Сайт, который нужно проиндексировать
     * @return {@link ResponseEntity<ShortInfo>} Короткий формат ответа
     */
    @PostMapping("/indexAllLemmas")
    public ResponseEntity<ShortInfo> indexingLemmas(@RequestBody SiteModel siteModel) {
        return new ResponseEntity<ShortInfo>(indexingService.pagesForSiteIndexingLemmas(siteModel),
                HttpStatus.OK);
    }

    /**
     * Останавливает индексацию лем
     *
     * @return {@link ResponseEntity<ShortInfo>} Короткий формат ответа
     */
    @GetMapping("/stopIndexLemmas")
    public ResponseEntity<ShortInfo> stopIndexingLemmas() {
        return new ResponseEntity<ShortInfo>(indexingService.stopIndexingLemmas(), HttpStatus.OK);
    }

    /**
     * Производит поиск на страницах указанных сайтов по леммам из переданного запроса
     *
     * @param query  Запрос в виде слова или строки
     * @param site   url сайта в пределах которого необходимо найти страницы
     * @param offset Сдвиг от начала списка результатов
     * @param limit  Количество результатов, которое необходимо вывести
     * @return {@link ResponseEntity<ResponseForSearching>} Итоговый результат поиска со списком {@link PageInfoAfterSearching}
     */
    @GetMapping("/search")
    public ResponseEntity<ResponseForSearching> searchOnPages(
            @RequestParam String query,
            @RequestParam(defaultValue = "") String site,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "0") Integer limit) {
        return indexingService.findingOnPagesSmth(query, site, offset, limit);

    }

}


