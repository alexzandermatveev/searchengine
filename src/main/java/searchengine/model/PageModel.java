package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Index;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Сущность страницы сайта
 */
@Entity
@Table(name = "page", indexes = @Index(columnList = "path"))
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PageModel {
    /**
     * Идентификатор страницы
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private long id;

    /**
     * Материнский сайт
     */
    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "site", referencedColumnName = "id")
    private SiteModel site;

    /**
     * Путь до страницы
     */
    @Column(columnDefinition = "VARCHAR(255)", nullable = false, unique = true)
    private String path;

    /**
     * Код http полученный при попытке индексации
     */
    private int code;
    /**
     * Содержимое страницы
     */
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    /**
     * Список индексов страницы, связывающие {@link Lemma} и {@link PageModel}
     */
    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<searchengine.model.Index> indexList = new ArrayList<>();


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageModel pageModel = (PageModel) o;
        return Objects.equals(getPath(), pageModel.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getPath());
    }
}
