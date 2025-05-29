package com.veystream.service;

import com.veystream.dto.annotation.ExactMatchField;
import com.veystream.dto.annotation.KeywordSearch;
import com.veystream.dto.annotation.MultilingualField;
import com.veystream.search.config.EntitySearchConfig;
import com.veystream.search.config.SearchConfigProvider;
import com.veystream.dao.GenericEntityDAO;
import com.veystream.search.dto.ExactQueryCondition;
import com.veystream.search.dto.PageQuery;
import com.veystream.search.fetcher.AggregatingIdFetcher; // Changed import
import com.veystream.search.fetcher.SearchContext;    // Added import

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value; // Not used in this version
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map; // For the return type
import java.util.Set;

@Service
public class QueryCoordinatorService {

    private static final Logger log = LoggerFactory.getLogger(QueryCoordinatorService.class);

    private final AggregatingIdFetcher aggregatingIdFetcher; // Changed from EnhancedMultilingualIdFetcher
    private final SearchConfigProvider searchConfigProvider;
    private final GenericEntityDAO genericEntityDAO;

    // Consider making these configurable
    private final String defaultLanguage = "zh_CN"; 
    private final String i18nTypeDb = "数据库表";

    @Autowired
    public QueryCoordinatorService(AggregatingIdFetcher aggregatingIdFetcher, // Changed
                                   SearchConfigProvider searchConfigProvider,
                                   GenericEntityDAO genericEntityDAO) {
        this.aggregatingIdFetcher = aggregatingIdFetcher; // Changed
        this.searchConfigProvider = searchConfigProvider;
        this.genericEntityDAO = genericEntityDAO;
    }

    public <T> List<Map<String, Object>> searchEntities(Object criteriaDTO, Class<T> entityClass, PageQuery pageQuery) {
        EntitySearchConfig entityConfig = searchConfigProvider.getRequiredConfig(entityClass);
        Locale currentLocale = LocaleContextHolder.getLocale();
        String currentLanguage = currentLocale.toString();

        // Create SearchContext
        SearchContext searchContext = new SearchContext(currentLanguage, defaultLanguage, i18nTypeDb);

        List<Set<Long>> allMultilingualIdSets = new ArrayList<>();
        List<ExactQueryCondition> exactConditions = new ArrayList<>();
        boolean multilingualConditionPresent = false;

        Field[] fields = criteriaDTO.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(criteriaDTO);
                if (value == null || (value instanceof String && StringUtils.isBlank((String) value))) {
                    continue;
                }

                if (field.isAnnotationPresent(KeywordSearch.class)) {
                    multilingualConditionPresent = true;
                    KeywordSearch ann = field.getAnnotation(KeywordSearch.class);
                    Set<Long> ids = aggregatingIdFetcher.aggregateIdsByKeyword( // Changed call
                            (String) value, entityConfig, Arrays.asList(ann.entityFieldNames()),
                            searchContext // Pass SearchContext
                    );
                    allMultilingualIdSets.add(ids);
                    log.debug("KeywordSearch on DTO field '{}' for entity fields '{}' with value '{}' aggregated IDs: {}", 
                              field.getName(), ann.entityFieldNames(), value, ids.size());
                } else if (field.isAnnotationPresent(MultilingualField.class)) {
                    multilingualConditionPresent = true;
                    MultilingualField ann = field.getAnnotation(MultilingualField.class);
                    Set<Long> ids = aggregatingIdFetcher.aggregateIdsByExactMatch( // Changed call
                            (String) value, entityConfig, ann.entityFieldName(),
                            searchContext // Pass SearchContext
                    );
                    allMultilingualIdSets.add(ids);
                     log.debug("MultilingualField on DTO field '{}' for entity field '{}' with value '{}' aggregated IDs: {}", 
                               field.getName(), ann.entityFieldName(), value, ids.size());
                } else if (field.isAnnotationPresent(ExactMatchField.class)) {
                    ExactMatchField ann = field.getAnnotation(ExactMatchField.class);
                    exactConditions.add(new ExactQueryCondition(ann.entityFieldName(), value));
                    log.debug("ExactMatchField on DTO field '{}' for entity field '{}' with value '{}'", 
                              field.getName(), ann.entityFieldName(), value);
                }
            } catch (IllegalAccessException e) {
                log.error("Error accessing field {} on DTO {}: {}", field.getName(), criteriaDTO.getClass().getSimpleName(), e.getMessage(), e);
                // Depending on policy, maybe rethrow as a runtime exception
            }
        }

        Set<Long> finalIdsToFilterBy = null; // Null means no ID filtering from multilingual search

        if (multilingualConditionPresent) {
            if (allMultilingualIdSets.isEmpty()) { 
                finalIdsToFilterBy = new HashSet<>(); 
            } else {
                finalIdsToFilterBy = new HashSet<>(allMultilingualIdSets.get(0));
                for (int i = 1; i < allMultilingualIdSets.size(); i++) {
                    finalIdsToFilterBy.retainAll(allMultilingualIdSets.get(i));
                }
            }
            
            if (finalIdsToFilterBy.isEmpty()) {
                log.debug("Intersection of multilingual ID sets is empty. No results for DTO: {}", criteriaDTO);
                return Collections.emptyList();
            }
            log.debug("Final intersected IDs from multilingual search: {}", finalIdsToFilterBy.size());
        }
        
        return genericEntityDAO.findEntitiesWithFiltersAndIds(
                entityConfig.getEntityTableName(),
                entityConfig.getIdField(),
                finalIdsToFilterBy, 
                exactConditions,
                pageQuery
        );
    }
}
