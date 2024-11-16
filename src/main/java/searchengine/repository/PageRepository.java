package searchengine.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageModel;

import java.util.Optional;

/**
 * Репозиторий для работы с сущностью {@link PageModel}.
 */
@Repository
public interface PageRepository extends JpaRepository<PageModel, Long> {
    PageModel findByPath(String path);

    /**
     * Удаляет все записи из таблицы в БД по ссылке на страницу
     *
     * @param path Ссылка на страницу, которую нужно удалить
     */
    void deleteAllByPath(String path);

    /**
     * Находит страницу по id сайта, по отношению к которому текущая страница считается дочерней
     *
     * @param id Идентификатор материнского сайта
     * @return {@link PageModel} Страница с указанным id, иначе {@code null}
     */
    PageModel findBySiteId(Long id);


    /**
     * Находит страницу по ее идентификатору и подгружает связанные индексы.
     * Использует {@link EntityGraph} для загрузки коллекции {@code indexList}, чтобы избежать проблемы с ленивой загрузкой.
     *
     * @param id Идентификатор страницы, которую нужно найти.
     * @return {@link Optional<PageModel>} Страница с указанным идентификатором, если найдена, иначе пустой {@link Optional}.
     */
    @EntityGraph(attributePaths = {"indexList", "indexList.lemma", "indexList.lemma.indexList"})
    Optional<PageModel> findById(Long id);
}
