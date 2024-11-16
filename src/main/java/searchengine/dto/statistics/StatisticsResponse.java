package searchengine.dto.statistics;

import lombok.*;

/**
 * Собирательная сущность статистики для ответа клиенту
 */
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class StatisticsResponse {
    private boolean result;
    private StatisticsData statistics;
}
