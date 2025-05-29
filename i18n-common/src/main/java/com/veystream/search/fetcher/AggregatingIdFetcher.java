package com.veystream.search.fetcher;

import com.veystream.search.config.EntitySearchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service; // Or @Component

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors; // Added for Collectors.toList()

@Service // Register as a Spring bean
public class AggregatingIdFetcher {

    private static final Logger log = LoggerFactory.getLogger(AggregatingIdFetcher.class);

    private final List<IdSourceFetcher> idFetchers;

    @Autowired
    public AggregatingIdFetcher(List<IdSourceFetcher> idFetchers) {
        // Spring will automatically inject all beans that implement IdSourceFetcher
        this.idFetchers = (idFetchers != null) ? idFetchers : Collections.emptyList();
        if (this.idFetchers.isEmpty()) {
            log.warn("No IdSourceFetcher implementations found. AggregatingIdFetcher will not be effective.");
        } else {
            log.info("AggregatingIdFetcher initialized with {} IdSourceFetcher(s): {}", 
                     this.idFetchers.size(), 
                     this.idFetchers.stream().map(IdSourceFetcher::getSourceType).collect(Collectors.toList()));
        }
    }

    public Set<Long> aggregateIdsByKeyword(String keyword,
                                           EntitySearchConfig entityConfig,
                                           List<String> entityFieldNamesToSearch,
                                           SearchContext searchContext) {
        Set<Long> allMatchedIds = new HashSet<>();
        if (idFetchers.isEmpty()) {
            return allMatchedIds;
        }

        log.debug("Aggregating IDs by keyword '{}' for entity {}, fields: {}", 
                  keyword, entityConfig.getEntityClass().getSimpleName(), entityFieldNamesToSearch);

        for (IdSourceFetcher fetcher : idFetchers) {
            try {
                log.trace("Calling fetcher: {} for keyword search", fetcher.getSourceType());
                Set<Long> idsFromSource = fetcher.fetchIdsByKeyword(keyword, entityConfig, entityFieldNamesToSearch, searchContext);
                if (idsFromSource != null && !idsFromSource.isEmpty()) {
                    allMatchedIds.addAll(idsFromSource);
                    log.trace("Fetcher {} found {} IDs", fetcher.getSourceType(), idsFromSource.size());
                }
            } catch (Exception e) {
                log.error("Error calling IdSourceFetcher '{}' for keyword search: {}", 
                          fetcher.getSourceType(), e.getMessage(), e);
            }
        }
        log.debug("Total IDs aggregated by keyword: {}", allMatchedIds.size());
        return allMatchedIds;
    }

    public Set<Long> aggregateIdsByExactMatch(String valueToMatch,
                                              EntitySearchConfig entityConfig,
                                              String entityFieldName,
                                              SearchContext searchContext) {
        Set<Long> allMatchedIds = new HashSet<>();
        if (idFetchers.isEmpty()) {
            return allMatchedIds;
        }
        
        log.debug("Aggregating IDs by exact match for entity {}, field '{}', value '{}'", 
                  entityConfig.getEntityClass().getSimpleName(), entityFieldName, valueToMatch);

        for (IdSourceFetcher fetcher : idFetchers) {
            try {
                log.trace("Calling fetcher: {} for exact match", fetcher.getSourceType());
                Set<Long> idsFromSource = fetcher.fetchIdsByExactMatch(valueToMatch, entityConfig, entityFieldName, searchContext);
                if (idsFromSource != null && !idsFromSource.isEmpty()) {
                    allMatchedIds.addAll(idsFromSource);
                    log.trace("Fetcher {} found {} IDs", fetcher.getSourceType(), idsFromSource.size());
                }
            } catch (Exception e) {
                log.error("Error calling IdSourceFetcher '{}' for exact match: {}", 
                          fetcher.getSourceType(), e.getMessage(), e);
            }
        }
        log.debug("Total IDs aggregated by exact match: {}", allMatchedIds.size());
        return allMatchedIds;
    }
}
