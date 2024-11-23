package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Lemma;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;
import searchengine.repository.LemmaRepository;
import searchengine.repository.SiteRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
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
            HashSet<String> lemmas = new HashSet<>();
            for (PageModel page : siteRepository.findByUrl(item.getUrl()).getPage()) {
                lemmas.addAll(page.getIndexList().stream()
                        .map(index -> index.getLemma().getLemma())
                        .distinct().toList());
            }

            item.setPages(pages);
            item.setLemmas(lemmas.stream().distinct().count());
            item.setStatus(repSite.getStatus());
            item.setError(repSite.getLastError());
            item.setStatusTime(repSite.getStatusTime().getEpochSecond());
            total.setPages(total.getPages() + pages);
            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        total.setLemmas(lemmaRepository.count());
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
