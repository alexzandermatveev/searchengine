package searchengine.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность леммы
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "lemma")
public class Lemma {
    /**
     * Идентификатор леммы
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * Нормализованная форма слова
     */
    @Column(name = "lemma", nullable = false)
    private String lemma;
    /**
     * Частота повторения среди всех страниц всех сайтов
     */
    @Column(name = "frequency", nullable = false)
    private Long frequency;
    /**
     * Список связанных индексов
     */
    @OneToMany(mappedBy = "lemma", cascade = CascadeType.ALL)
    private List<Index> indexList = new ArrayList<>();

}
