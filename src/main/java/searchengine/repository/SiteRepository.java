package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteModel;

import java.util.List;

/**
 * Репозиторий для работы с сущностью {@link SiteModel}
 */
@Repository
public interface SiteRepository extends JpaRepository<SiteModel, Long> {
    /**
     * Находит сайт по имени
     *
     * @param name Имя сайта
     * @return {@link SiteModel}  Найденный сайт по имени, иначе {@code null}
     */
    SiteModel findByName(String name);

    /**
     * Находит все сайты по наименованию
     *
     * @param name Имя сайта
     * @return {@code List<SiteModel>} Список найденных сайтов
     */
    List<SiteModel> findAllByName(String name);

    /**
     * Удаляет все сайты по наименованию
     *
     * @param name Имя сайта
     */
    void deleteAllByName(String name);

    /**
     * Находит сайты по url
     *
     * @param url
     * @return {@link SiteModel}
     */
    SiteModel findByUrl(String url);

    /**
     * Удаляет все сайты из базы данных по url
     *
     * @param url
     */
    void deleteAllByUrl(String url);

    /**
     * Проверяет наличие в базе данных сайта по url
     *
     * @param url
     * @return {@link Boolean} Принимает {@code true} если существует, иначе {@code false}
     */
    Boolean existsByUrl(String url);
}
