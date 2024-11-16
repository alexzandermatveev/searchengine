package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Index;
import searchengine.model.PageModel;

/**
 * Репозиторий по работе с сущностью {@link Index}
 */
public interface IndexRepository extends JpaRepository<Index, Long> {
    /**
     * Находит сущности {@link Index} в базе данных по связанному с {@link Index} странице {@link PageModel}
     *
     * @param pageModel Связанная страница, на которой найдена лемма
     * @return {@link Index} Сущность обозначающая индекс
     */
    Index getByPage(PageModel pageModel);
}
