package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repository.SiteRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final SitesList sites;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics();
        total.setSites(sites.getSites().size());
        total.setIndexing(true);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            SiteModel repSite = siteRepository.findByUrl(item.getUrl());
            int pages = repSite.getPage().size();
            long lemmas = 0;
            for (PageModel page : siteRepository.findByUrl(item.getUrl()).getPage()) {
                lemmas += page.getIndexList().stream()
                        .map(index -> index.getLemma().getLemma())
                        .parallel()
                        .distinct()
                        .count();
            }
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(repSite.getStatus());
            item.setError(repSite.getLastError());
            item.setStatusTime(repSite.getStatusTime().getEpochSecond());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}