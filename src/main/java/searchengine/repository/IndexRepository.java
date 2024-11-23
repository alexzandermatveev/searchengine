package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.PageModel;

import java.util.List;

/**
 * Репозиторий по работе с сущностью {@link Index}
 */
public interface IndexRepository extends JpaRepository<Index, Long> {
    /**
     * Находит сущности {@link Index} в базе данных по связанному с {@link Index} странице {@link PageModel}
     *
     * @param page Связанная страница, на которой найдена лемма
     * @return {@link Index} Сущность обозначающая индекс
     */
    Index getByPage(PageModel page);
    List<Index> findAllByPageAndLemma(PageModel page, Lemma lemma);
    boolean existsByPageAndLemma(PageModel page, Lemma lemma);
    Boolean existsByLemma(Lemma lemma);
    List<Index> findAllByLemma(Lemma lemma);
}
