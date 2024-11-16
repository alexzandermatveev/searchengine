package searchengine.dto.statistics;

import lombok.Data;

import java.util.List;

/**
 * Собирательная сущность для {@link DetailedStatisticsItem} и {@link TotalStatistics}
 */
@Data
public class StatisticsData {
    private TotalStatistics total;
    private List<DetailedStatisticsItem> detailed;
}
