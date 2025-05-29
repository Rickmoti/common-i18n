package com.veystream.service;

import com.fasterxml.jackson.databind.ObjectMapper; // Import ObjectMapper
import com.veystream.dao.GoodsDao;
// import com.veystream.dao.I18nMessageMapper; // No longer directly used in searchGoods
import com.veystream.dto.GoodsSearchCriteriaDTO; // Import new DTO
import com.veystream.entity.Goods;
// import com.veystream.helpers.I18nResourceHelper; // No longer directly used in searchGoods
// import com.veystream.dto.I18nResourceInfo; // No longer directly used in searchGoods
import com.veystream.search.dto.PageQuery; // Import PageQuery

// import org.apache.commons.lang3.StringUtils; // No longer needed for keyword check here
// import org.springframework.context.i18n.LocaleContextHolder; // No longer needed here
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Keep if other methods need it
import org.springframework.beans.factory.annotation.Autowired; // For constructor injection

// import javax.annotation.Resource; // Can be replaced by Autowired constructor -> Removed @Resource
import java.util.ArrayList; // For new List
import java.util.Collections; // For Collections.emptyList
import java.util.List;
import java.util.Map;
// import java.util.Set; // No longer directly used
// import java.util.HashSet; // No longer directly used
// import java.util.Arrays; // No longer directly used

@Service
public class GoodsService {

    private final GoodsDao goodsDao;
    private final I18nDataService i18nDataService; 
    // private final I18nMessageMapper i18nMessageMapper; // Removed
    
    private final QueryCoordinatorService queryCoordinatorService; // Added
    private final ObjectMapper objectMapper; // Added

    @Autowired // Use constructor injection
    public GoodsService(GoodsDao goodsDao, 
                        I18nDataService i18nDataService, 
                        QueryCoordinatorService queryCoordinatorService, 
                        ObjectMapper objectMapper) {
        this.goodsDao = goodsDao;
        this.i18nDataService = i18nDataService;
        this.queryCoordinatorService = queryCoordinatorService;
        this.objectMapper = objectMapper;
    }

    // ... createGoods, updateGoods, findGoodsById, getGoodsWithAllTranslations, getAllGoods ...
    // These methods remain unchanged for now.

    /**
     * Searches for Goods entities based on criteria, handling multilingual fields.
     *
     * @param criteria The search criteria DTO.
     * @return A list of Goods entities matching the criteria.
     */
    public List<Goods> searchGoods(GoodsSearchCriteriaDTO criteria) {
        if (criteria == null) {
            // Or handle as per application's error handling policy
            return getAllGoods(); // Fallback to returning all goods if criteria is null
        }

        PageQuery pageQuery = new PageQuery(criteria.getPage(), criteria.getSize());
        
        List<Map<String, Object>> resultsAsMaps = queryCoordinatorService.searchEntities(
                criteria, 
                Goods.class, 
                pageQuery
        );

        if (resultsAsMaps == null || resultsAsMaps.isEmpty()) {
            return Collections.emptyList();
        }

        List<Goods> goodsList = new ArrayList<>();
        for (Map<String, Object> map : resultsAsMaps) {
            try {
                // Ensure your Goods entity has a no-arg constructor and setters
                // that Jackson can use.
                Goods good = objectMapper.convertValue(map, Goods.class);
                goodsList.add(good);
            } catch (IllegalArgumentException e) {
                // Log error during mapping, or handle as per policy
                // This might happen if map keys don't match Goods properties
                // or if there are type mismatches that Jackson can't handle.
                System.err.println("Error converting map to Goods: " + map + "; Error: " + e.getMessage());
            }
        }
        return goodsList;
    }
    
    // Other existing methods (createGoods, updateGoods, etc.)
    // Ensure these methods' dependencies are still met.
    // For example, createGoods and updateGoods still use i18nDataService.
     @Transactional
    public Goods createGoods(Goods goods, Map<String, Map<String, String>> allFieldTranslations) {
        goodsDao.insert(goods); 
        // ... (rest of createGoods logic using i18nDataService as before) ...
        // This part does not need to change for the search refactoring.
        // Ensure I18nResource and AnnotationUtils are imported if used here.
        com.veystream.annotation.I18nResource i18nResource = 
            org.springframework.core.annotation.AnnotationUtils.findAnnotation(Goods.class, com.veystream.annotation.I18nResource.class);
        if (i18nResource == null || goods.getId() == null) {
            return goods;
        }
        String prefix = i18nResource.prefix();
        String identityValue = goods.getId().toString(); 
        if (allFieldTranslations != null) {
            for (Map.Entry<String, Map<String, String>> entry : allFieldTranslations.entrySet()) {
                i18nDataService.saveOrUpdateTranslations(identityValue, prefix, entry.getKey(), entry.getValue(), null);
            }
        }
        return goods;
    }

    @Transactional
    public Goods updateGoods(Goods goods, Map<String, Map<String, String>> allFieldTranslations) {
        if (goods.getId() == null) {
            throw new IllegalArgumentException("Goods ID cannot be null for update.");
        }
        goodsDao.updateById(goods);
        // ... (rest of updateGoods logic using i18nDataService as before) ...
        com.veystream.annotation.I18nResource i18nResource = 
            org.springframework.core.annotation.AnnotationUtils.findAnnotation(Goods.class, com.veystream.annotation.I18nResource.class);
        if (i18nResource == null) {
            return goods;
        }
        String prefix = i18nResource.prefix();
        String identityValue = goods.getId().toString();
        if (allFieldTranslations != null) {
            for (Map.Entry<String, Map<String, String>> entry : allFieldTranslations.entrySet()) {
                i18nDataService.saveOrUpdateTranslations(identityValue, prefix, entry.getKey(), entry.getValue(), null);
            }
        }
        return goods;
    }
    
    public Goods findGoodsById(Long id) {
        return goodsDao.selectById(id);
    }

    public Map<String, Object> getGoodsWithAllTranslations(Long id) {
        Goods goods = findGoodsById(id);
        if (goods == null) return null;
        Map<String, Object> result = new java.util.HashMap<>(); // Use HashMap
        result.put("entity", goods);
        com.veystream.annotation.I18nResource i18nResource = 
            org.springframework.core.annotation.AnnotationUtils.findAnnotation(Goods.class, com.veystream.annotation.I18nResource.class);
        if (i18nResource == null || goods.getId() == null) {
            result.put("translations", Collections.emptyMap());
            return result;
        }
        String prefix = i18nResource.prefix();
        String identityValue = goods.getId().toString();
        Map<String, Map<String, String>> allTranslations = new java.util.HashMap<>();
        for (com.veystream.annotation.I18nField i18nField : i18nResource.i18nFields()) {
            Map<String, String> fieldTrans = i18nDataService.getTranslationsForField(identityValue, prefix, i18nField.fieldName());
            if (fieldTrans != null && !fieldTrans.isEmpty()) {
                allTranslations.put(i18nField.fieldName(), fieldTrans);
            }
        }
        result.put("translations", allTranslations);
        return result;
    }

    public List<Goods> getAllGoods() {
        return goodsDao.selectList(null);
    }
}
