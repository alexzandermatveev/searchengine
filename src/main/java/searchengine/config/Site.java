package searchengine.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Класс для получения записи из списка сайтов из файла-конфигурации приложения
 */
@Setter
@Getter
public class Site {
    private String url;
    private String name;
}
