package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteModel;
import searchengine.model.SiteStatus;
import searchengine.repository.SiteRepository;

import java.time.Instant;
/**
 * После запуска приложения создает {@link SiteModel} из списка в файле-конфигурации
 * приложения и записывает в репозиторий
 */
@Service
@RequiredArgsConstructor
public class WriterSitesFromProfile {

    private final SiteRepository siteRepository;
    private final SitesList sites;

    @EventListener(ContextRefreshedEvent.class)
    public void writeSitesFromYamlToDB() {
        for (Site site : sites.getSites()) {
            Boolean isDbSiteExists = siteRepository.existsByUrl(site.getUrl());
            if (!isDbSiteExists) {
                SiteModel siteModel = new SiteModel();
                siteModel.setUrl(site.getUrl());
                siteModel.setName(site.getName());
                siteModel.setStatus(SiteStatus.INDEXED);
                siteModel.setLastError("just added");
                siteModel.setStatusTime(Instant.now());
                siteRepository.save(siteModel);
            }
        }
    }
}
