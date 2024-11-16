package searchengine.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

/**
 * Репозиторий по работе с сущностями {@link Lemma}
 */
@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Long> {
    /**
     * Находит сущность {@link Lemma} по нормализованному слову
     *
     * @param lemma Нормализованное слово
     * @return {@link Lemma}
     */
    Lemma findByLemma(String lemma);

    /**
     * Находит сущность {@link Lemma} по нормализованному слову
     * и жадно подгружает связанный список {@code indexList}.
     *
     * @param lemma Нормализованное слово
     * @return {@link Lemma}
     */
    @EntityGraph(attributePaths = "indexList")
    Lemma findWithIndexListByLemma(String lemma);
}
