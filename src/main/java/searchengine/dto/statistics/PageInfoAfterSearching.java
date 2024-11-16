package searchengine.dto.statistics;

import lombok.Data;

/**
 * Информация о странице после поиска по леммам
 */
@Data
public class PageInfoAfterSearching {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    /**
     * Фрагмент текста, в котором найдены совпадения, <b>выделенные жирным</b>, в формате HTML
     */
    private String snippet;
    private Double relevance;
}
