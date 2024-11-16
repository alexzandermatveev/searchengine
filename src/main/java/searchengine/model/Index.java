package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

/**
 * Сущность индекса
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "indexes")
public class Index {
    /**
     * Идентификатор индекса
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    /**
     * Связанная с индексом лемма
     */
    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "lemma", referencedColumnName = "id")
    private Lemma lemma;
    /**
     * Связанная с индексом страница
     */
    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "page", referencedColumnName = "id")
    private PageModel page;
    /**
     * Количество данной леммы для данной страницы
     */
    @Column(name = "rank_value", nullable = false)
    private Long rank;
}
