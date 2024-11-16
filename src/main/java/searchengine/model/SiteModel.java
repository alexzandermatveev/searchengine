package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность сайта
 */
@Entity
@Table(name = "site")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SiteModel {
    /**
     * Идентификатор сайта
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    /**
     * Статус индексирования
     */
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private SiteStatus status;

    /**
     * Время последнего обновления
     */
    @Column(name = "status_time", nullable = false)
    @CreationTimestamp
    private Instant statusTime;
    /**
     * Сообщение последней ошибки
     */
    @Column(name = "last_error", columnDefinition = "VARCHAR(255)")
    private String lastError;
    /**
     * Полный путь до сайта
     */
    @Column(columnDefinition = "VARCHAR(255)", nullable = false, unique = true)
    private String url;
    /**
     * Наименование сайта
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String name;

    /**
     * Дочерние страницы
     */
    @OneToMany(mappedBy = "site", cascade = CascadeType.ALL)
    private List<PageModel> page = new ArrayList<>();
}
