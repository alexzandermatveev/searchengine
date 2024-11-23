package searchengine.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;

import java.util.List;
import java.util.Set;

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

    List<Lemma> findAllByLemmaIn(Set<String> lemma);


    boolean existsByLemma(String lemma);

    /**
     * Находит сущность {@link Lemma} по нормализованному слову
     * и жадно подгружает связанный список {@code indexList}.
     *
     * @param lemma Нормализованное слово
     * @return {@link Lemma}
     */
    @EntityGraph(attributePaths = {"indexList", "indexList.page"})
    Lemma findWithIndexListByLemma(String lemma);
}
