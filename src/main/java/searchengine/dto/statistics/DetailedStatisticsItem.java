package searchengine.dto.statistics;

import lombok.Data;
import searchengine.model.SiteStatus;

/**
 * Детальная статистика по сайту
 */
@Data
public class DetailedStatisticsItem {
    private String url;
    private String name;
    private SiteStatus status;
    private long statusTime;
    private String error;
    private long pages;
    private long lemmas;
}
