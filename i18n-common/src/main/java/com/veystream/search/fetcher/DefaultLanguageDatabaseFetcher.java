package com.veystream.search.fetcher;

import com.veystream.dao.EntityDefaultLanguageSearchDAO;
import com.veystream.search.config.EntitySearchConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet; // Import not strictly needed if only passing through, but good for consistency
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class DefaultLanguageDatabaseFetcher implements IdSourceFetcher {

    private static final Logger log = LoggerFactory.getLogger(DefaultLanguageDatabaseFetcher.class);
    private static final String SOURCE_TYPE = "db_default_language";

    private final EntityDefaultLanguageSearchDAO entityDefaultLanguageSearchDAO;

    @Autowired
    public DefaultLanguageDatabaseFetcher(EntityDefaultLanguageSearchDAO entityDefaultLanguageSearchDAO) {
        this.entityDefaultLanguageSearchDAO = entityDefaultLanguageSearchDAO;
    }

    @Override
    public Set<Long> fetchIdsByKeyword(String keyword, 
                                       EntitySearchConfig entityConfig, 
                                       List<String> entityFieldNamesToSearch, 
                                       SearchContext searchContext) {
        // Filter the requested DTO search fields against what's actually configured 
        // as searchable in the default language for this entity.
        List<String> searchableFieldsInDefaultLang = entityConfig.getDefaultLanguageSearchableFields();
        if (searchableFieldsInDefaultLang == null || searchableFieldsInDefaultLang.isEmpty()) {
            log.debug("Fetcher '{}': No default language searchable fields configured for entity {}", SOURCE_TYPE, entityConfig.getEntityClass().getSimpleName());
            return Collections.emptySet();
        }

        List<String> fieldsToQuery = entityFieldNamesToSearch.stream()
                                     .filter(searchableFieldsInDefaultLang::contains)
                                     .collect(Collectors.toList());

        if (StringUtils.isBlank(keyword) || fieldsToQuery.isEmpty()) {
            return Collections.emptySet();
        }
        
        Set<Long> matchedIds = Collections.emptySet();
        try {
            matchedIds = entityDefaultLanguageSearchDAO.findIdsByKeywordInDefaultLanguage(
                    entityConfig,
                    fieldsToQuery,
                    keyword
            );
        } catch (Exception e) {
            log.error("Error fetching IDs by keyword from {} for entity {}, fields {}, keyword '{}': {}",
                      SOURCE_TYPE, entityConfig.getEntityClass().getSimpleName(), fieldsToQuery, keyword, e.getMessage(), e);
        }
        log.debug("Fetcher '{}' found IDs by keyword '{}' for entity {}: {}", 
                  SOURCE_TYPE, keyword, entityConfig.getEntityClass().getSimpleName(), matchedIds.size());
        return matchedIds;
    }

    @Override
    public Set<Long> fetchIdsByExactMatch(String valueToMatch, 
                                          EntitySearchConfig entityConfig, 
                                          String entityFieldName, 
                                          SearchContext searchContext) {

        // Check if the requested field is actually configured as searchable in default language.
        List<String> searchableFieldsInDefaultLang = entityConfig.getDefaultLanguageSearchableFields();
        if (searchableFieldsInDefaultLang == null || !searchableFieldsInDefaultLang.contains(entityFieldName)) {
             log.debug("Fetcher '{}': Field '{}' is not configured as a default language searchable field for entity {}", 
                SOURCE_TYPE, entityFieldName, entityConfig.getEntityClass().getSimpleName());
            return Collections.emptySet();
        }

        if (StringUtils.isBlank(valueToMatch) || StringUtils.isBlank(entityFieldName)) {
            return Collections.emptySet();
        }

        Set<Long> matchedIds = Collections.emptySet();
        try {
            matchedIds = entityDefaultLanguageSearchDAO.findIdsByExactMatchInDefaultLanguage(
                    entityConfig,
                    entityFieldName,
                    valueToMatch
            );
        } catch (Exception e) {
            log.error("Error fetching IDs by exact match from {} for entity {}, field {}, value '{}': {}",
                      SOURCE_TYPE, entityConfig.getEntityClass().getSimpleName(), entityFieldName, valueToMatch, e.getMessage(), e);
        }
        log.debug("Fetcher '{}' found IDs by exact match for entity {}, field '{}', value '{}': {}", 
                  SOURCE_TYPE, entityConfig.getEntityClass().getSimpleName(), entityFieldName, valueToMatch, matchedIds.size());
        return matchedIds;
    }

    @Override
    public String getSourceType() {
        return SOURCE_TYPE;
    }
}
