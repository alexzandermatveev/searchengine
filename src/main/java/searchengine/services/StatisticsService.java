package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.statistics.ShortInfo;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;

/**
 * Интерфейс для будущего сервиса по обработке запросов из контроллера
 */
public interface StatisticsService {
    /**
     * Возвращает данные статистики
     *
     * @return {@link StatisticsResponse}
     */
    StatisticsResponse getStatistics();


}
