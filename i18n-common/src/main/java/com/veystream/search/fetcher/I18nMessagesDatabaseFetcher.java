package com.veystream.search.fetcher;

import com.veystream.dao.I18nMessageMapper;
import com.veystream.search.config.EntitySearchConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component; // Or @Service

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component // Register as a Spring bean
public class I18nMessagesDatabaseFetcher implements IdSourceFetcher {

    private static final Logger log = LoggerFactory.getLogger(I18nMessagesDatabaseFetcher.class);
    private static final String SOURCE_TYPE = "db_i18n_messages";

    private final I18nMessageMapper i18nMessageMapper;

    @Autowired
    public I18nMessagesDatabaseFetcher(I18nMessageMapper i18nMessageMapper) {
        this.i18nMessageMapper = i18nMessageMapper;
    }

    @Override
    public Set<Long> fetchIdsByKeyword(String keyword,
                                       EntitySearchConfig entityConfig,
                                       List<String> entityFieldNamesToSearch,
                                       SearchContext searchContext) {

        if (StringUtils.isBlank(keyword) || !entityConfig.isInternationalized() ||
            searchContext.getCurrentLanguage().equals(searchContext.getDefaultLanguage()) ||
            entityFieldNamesToSearch == null || entityFieldNamesToSearch.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> allMatchedIds = new HashSet<>();
        String currentLanguage = searchContext.getCurrentLanguage();
        String i18nDbType = searchContext.getI18nDbQueryType();

        for (String fieldName : entityFieldNamesToSearch) {
            if (StringUtils.isBlank(fieldName)) continue;

            String codePrefixPattern = entityConfig.getI18nPrefix() + "." + fieldName + ".";
            try {
                Set<Long> ids = i18nMessageMapper.findIdentityKeysByTextSearch(
                        "%" + keyword + "%", // Assuming mapper or SQL handles actual LIKE
                        currentLanguage,
                        i18nDbType,
                        codePrefixPattern
                );
                if (ids != null) {
                    allMatchedIds.addAll(ids);
                }
            } catch (Exception e) {
                log.error("Error fetching IDs by keyword from {} for entity {}, field {}, keyword '{}': {}",
                          SOURCE_TYPE, entityConfig.getEntityClass().getSimpleName(), fieldName, keyword, e.getMessage(), e);
            }
        }
        log.debug("Fetcher '{}' found IDs by keyword '{}' for entity {}: {}", 
                  SOURCE_TYPE, keyword, entityConfig.getEntityClass().getSimpleName(), allMatchedIds.size());
        return allMatchedIds;
    }

    @Override
    public Set<Long> fetchIdsByExactMatch(String valueToMatch,
                                          EntitySearchConfig entityConfig,
                                          String entityFieldName,
                                          SearchContext searchContext) {

        if (StringUtils.isBlank(valueToMatch) || StringUtils.isBlank(entityFieldName) ||
            !entityConfig.isInternationalized() ||
            searchContext.getCurrentLanguage().equals(searchContext.getDefaultLanguage())) {
            return Collections.emptySet();
        }

        String currentLanguage = searchContext.getCurrentLanguage();
        String i18nDbType = searchContext.getI18nDbQueryType();
        String codePrefixPattern = entityConfig.getI18nPrefix() + "." + entityFieldName + ".";
        Set<Long> matchedIds = new HashSet<>();

        try {
            Set<Long> ids = i18nMessageMapper.findIdentityKeysByExactTextAndCodePrefix(
                    valueToMatch,
                    currentLanguage,
                    i18nDbType,
                    codePrefixPattern
            );
            if (ids != null) {
                matchedIds.addAll(ids);
            }
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
