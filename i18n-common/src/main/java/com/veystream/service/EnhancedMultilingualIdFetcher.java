package com.veystream.service;

import com.veystream.dao.GoodsDao; // Placeholder for actual entity DAO access
import com.veystream.dao.I18nMessageMapper;
import com.veystream.entity.Goods; // Import Goods
import com.veystream.search.config.EntitySearchConfig;
import com.veystream.search.config.SearchConfigProvider;
import org.apache.commons.lang3.StringUtils; // For StringUtils.isBlank
import org.slf4j.Logger; // Add logger
import org.slf4j.LoggerFactory; // Add logger
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashSet; // For HashSet
import java.util.List;
import java.util.Set;

@Service
public class EnhancedMultilingualIdFetcher {

    private static final Logger log = LoggerFactory.getLogger(EnhancedMultilingualIdFetcher.class); // Logger

    private final I18nMessageMapper i18nMessageMapper;
    private final SearchConfigProvider searchConfigProvider; // Not used in this specific method directly, but good to have for the class
    private final GoodsDao goodsDao;

    @Autowired
    public EnhancedMultilingualIdFetcher(I18nMessageMapper i18nMessageMapper,
                                         SearchConfigProvider searchConfigProvider,
                                         GoodsDao goodsDao) { // GoodsDao injected for now
        this.i18nMessageMapper = i18nMessageMapper;
        this.searchConfigProvider = searchConfigProvider;
        this.goodsDao = goodsDao;
    }

    /**
     * Fetches entity IDs by keyword, searching specified multilingual fields.
     *
     * @param keyword Keyword to search.
     * @param entityConfig Configuration for the entity being searched.
     * @param entityFieldNamesToSearch List of entity field names (e.g., "name", "description") to search.
     * @param currentLanguage Current request language (e.g., "en_US").
     * @param defaultLanguage Default application language (e.g., "zh_CN").
     * @param i18nType The 'type' field in i18n_message for entity translations (e.g., "数据库表").
     * @return A set of matching entity IDs.
     */
    public Set<Long> fetchIdsByKeyword(String keyword,
                                       EntitySearchConfig entityConfig,
                                       List<String> entityFieldNamesToSearch,
                                       String currentLanguage,
                                       String defaultLanguage,
                                       String i18nType) {
        if (StringUtils.isBlank(keyword) || entityFieldNamesToSearch == null || entityFieldNamesToSearch.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Long> allMatchedIds = new HashSet<>();

        for (String fieldName : entityFieldNamesToSearch) {
            if (StringUtils.isBlank(fieldName)) continue;

            // Search in i18n_message table for translations in the current language
            if (entityConfig.isInternationalized() && !currentLanguage.equals(defaultLanguage)) {
                String codePrefixPattern = entityConfig.getI18nPrefix() + "." + fieldName + ".";
                try {
                    // Assuming mapper handles wildcards, or they should be added here.
                    // The existing README example implies LIKE is used.
                    Set<Long> idsFromTranslations = i18nMessageMapper.findIdentityKeysByTextSearch(
                            "%" + keyword + "%",
                            currentLanguage,
                            i18nType,
                            codePrefixPattern
                    );
                    if (idsFromTranslations != null) {
                        allMatchedIds.addAll(idsFromTranslations);
                    }
                } catch (Exception e) {
                    log.error("Error searching translations for entity {}, field {}, keyword '{}': {}",
                              entityConfig.getEntityClass().getSimpleName(), fieldName, keyword, e.getMessage());
                }
            }

            // Search in the entity's main table for default language values
            // For now, specific to Goods. This part needs generalization for other entities.
            if (entityConfig.getEntityClass().equals(Goods.class)) {
                try {
                    // GoodsDao is expected to handle wildcards if necessary
                    Set<Long> idsFromDefaultLang = goodsDao.findIdsByKeywordAndFields(
                            keyword,
                            Collections.singletonList(fieldName)
                    );
                    if (idsFromDefaultLang != null) {
                        allMatchedIds.addAll(idsFromDefaultLang);
                    }
                } catch (Exception e) {
                    log.error("Error searching default language for Goods entity, field {}, keyword '{}': {}",
                              fieldName, keyword, e.getMessage());
                }
            } else {
                // Placeholder for other entities - highlights the need for a generic DAO strategy
                // Log a warning if default language search is expected (either internationalized or only default lang fields)
                List<String> defaultLangSearchableFields = entityConfig.getDefaultLanguageSearchableFields();
                if (defaultLangSearchableFields != null && defaultLangSearchableFields.contains(fieldName)) {
                     log.warn("Default language search for entity {} and field {} is not implemented in this iteration. " +
                             "A generic DAO or specific DAO handling is required.",
                             entityConfig.getEntityClass().getSimpleName(), fieldName);
                }
            }
        }
        log.debug("Fetched IDs by keyword '{}' for entity {}: {}", keyword, entityConfig.getEntityClass().getSimpleName(), allMatchedIds);
        return allMatchedIds;
    }

    /**
     * Fetches entity IDs by an exact value for a specific multilingual field.
     *
     * @param valueToMatch The exact value to match.
     * @param entityConfig Configuration for the entity being searched.
     * @param entityFieldName The single entity field name (e.g., "name") to match.
     * @param currentLanguage Current request language.
     * @param defaultLanguage Default application language.
     * @param i18nType The 'type' field in i18n_message for entity translations.
     * @return A set of matching entity IDs.
     */
    public Set<Long> fetchIdsByExactMultilingualField(String valueToMatch,
                                                      EntitySearchConfig entityConfig,
                                                      String entityFieldName,
                                                      String currentLanguage,
                                                      String defaultLanguage,
                                                      String i18nType) {
        if (StringUtils.isBlank(valueToMatch) || StringUtils.isBlank(entityFieldName)) {
            return Collections.emptySet();
        }

        Set<Long> allMatchedIds = new HashSet<>();

        // Search in i18n_message table for exact text match in the current language
        if (entityConfig.isInternationalized() && !currentLanguage.equals(defaultLanguage)) {
            String codePrefixPattern = entityConfig.getI18nPrefix() + "." + entityFieldName + ".";
            try {
                Set<Long> idsFromTranslations = i18nMessageMapper.findIdentityKeysByExactTextAndCodePrefix(
                        valueToMatch, currentLanguage, i18nType, codePrefixPattern);
                if (idsFromTranslations != null) {
                    allMatchedIds.addAll(idsFromTranslations);
                }
            } catch (Exception e) {
                log.error("Error searching exact translations for entity {}, field {}, value '{}': {}", 
                          entityConfig.getEntityClass().getSimpleName(), entityFieldName, valueToMatch, e.getMessage(), e);
            }
        }

        // Search in the entity's main table for exact text match in the default language
        // This part needs generalization for other entities. For now, specific to Goods.
        if (entityConfig.getEntityClass().equals(Goods.class)) {
            try {
                Set<Long> idsFromDefaultLang = goodsDao.findIdsByExactFieldMatch(valueToMatch, entityFieldName);
                if (idsFromDefaultLang != null) {
                    allMatchedIds.addAll(idsFromDefaultLang);
                }
            } catch (Exception e) {
                log.error("Error searching exact default language for Goods entity, field {}, value '{}': {}", 
                          entityFieldName, valueToMatch, e.getMessage(), e);
            }
        } else {
            // Placeholder for other entities
            if (entityConfig.getDefaultLanguageSearchableFields().contains(entityFieldName) ||
                (entityConfig.isInternationalized() && entityConfig.getInternationalizedSearchableFields().contains(entityFieldName))) { // Corrected: was missing a )
                 log.warn("Exact default language search for entity {} and field {} is not implemented in this iteration. " +
                         "A generic DAO or specific DAO handling is required.",
                         entityConfig.getEntityClass().getSimpleName(), entityFieldName);
            }
        }
        log.debug("Fetched IDs by exact field '{}'='{}' for entity {}: {}", entityFieldName, valueToMatch, entityConfig.getEntityClass().getSimpleName(), allMatchedIds);
        return allMatchedIds;
    }
}
