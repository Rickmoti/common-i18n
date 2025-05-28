package com.veystream.service;

import com.veystream.annotation.I18nField;
import com.veystream.annotation.I18nResource;
import com.veystream.dao.GoodsDao;
import com.veystream.entity.Goods;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.annotation.AnnotationUtils; // Keep - used by other methods
import org.springframework.context.i18n.LocaleContextHolder;
import com.veystream.dao.I18nMessageMapper;
// import com.veystream.dao.I18nMessage; // No longer directly used in searchGoods
import com.veystream.helpers.I18nResourceHelper; // Added
import com.veystream.dto.I18nResourceInfo; // Added

import javax.annotation.Resource;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang3.StringUtils;
import java.util.HashMap; // Keep - used by other methods
import java.util.List;
import java.util.Map; // Keep - used by other methods
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Arrays; // Added for fallback search fields
// import java.util.stream.Collectors; // No longer directly used in searchGoods

// Note: I18nDataService is still injected but not used in the refactored searchGoods.
// It might be used by other methods (createGoods, updateGoods), so keep the injection for now.

@Service
public class GoodsService {

    @Resource
    private GoodsDao goodsDao;

    @Resource
    private I18nDataService i18nDataService;

    @Resource
    private I18nMessageMapper i18nMessageMapper; // Added

    /**
     * Saves a new Goods entity and its translations.
     * The default language values should be set directly on the goods object.
     *
     * @param goods The Goods entity to save (ID should be null or not set if auto-generated).
     * @param allFieldTranslations A map where:
     *                             - key is the field name (e.g., "name", "description")
     *                             - value is another map where:
     *                               - key is the language code (e.g., "en_US", "zh_CN")
     *                               - value is the translated text.
     * @return The saved Goods entity with its ID populated.
     */
    @Transactional
    public Goods createGoods(Goods goods, Map<String, Map<String, String>> allFieldTranslations) {
        // Save the main entity first to get its ID (if auto-generated)
        goodsDao.insert(goods); // Assumes GoodsDao extends BaseMapper or has an insert method

        // Check if the entity is annotated for I18N and if an ID is available
        I18nResource i18nResource = AnnotationUtils.findAnnotation(Goods.class, I18nResource.class);
        if (i18nResource == null || goods.getId() == null) {
            // No I18nResource annotation or ID not available, skip translation saving
            return goods;
        }

        String prefix = i18nResource.prefix();
        // The identityKey for Goods is "id", so its value is goods.getId()
        String identityValue = goods.getId().toString(); 

        if (allFieldTranslations != null) {
            for (Map.Entry<String, Map<String, String>> entry : allFieldTranslations.entrySet()) {
                String fieldName = entry.getKey();
                Map<String, String> translationsForField = entry.getValue();
                // The 'type' for I18nMessage can be defaulted in I18nDataService.
                // Passing null for 'type' to use the default in I18nDataService.
                i18nDataService.saveOrUpdateTranslations(identityValue, prefix, fieldName, translationsForField, null);
            }
        }
        return goods;
    }

    /**
     * Updates an existing Goods entity and its translations.
     * The default language values should be set directly on the goods object.
     *
     * @param goods The Goods entity to update (must have a valid ID).
     * @param allFieldTranslations A map structured the same as in createGoods.
     * @return The updated Goods entity.
     */
    @Transactional
    public Goods updateGoods(Goods goods, Map<String, Map<String, String>> allFieldTranslations) {
        if (goods.getId() == null) {
            throw new IllegalArgumentException("Goods ID cannot be null for update.");
        }
        goodsDao.updateById(goods); // Assumes GoodsDao extends BaseMapper or has an updateById method

        I18nResource i18nResource = AnnotationUtils.findAnnotation(Goods.class, I18nResource.class);
        if (i18nResource == null) {
            // Not an I18N resource, no translations to process
            return goods;
        }

        String prefix = i18nResource.prefix();
        String identityValue = goods.getId().toString();

        if (allFieldTranslations != null) {
            for (Map.Entry<String, Map<String, String>> entry : allFieldTranslations.entrySet()) {
                String fieldName = entry.getKey();
                Map<String, String> translationsForField = entry.getValue();
                i18nDataService.saveOrUpdateTranslations(identityValue, prefix, fieldName, translationsForField, null);
            }
        }
        return goods;
    }

    /**
     * Finds a Goods entity by its ID.
     *
     * @param id The ID of the Goods entity.
     * @return The Goods entity, or null if not found.
     */
    public Goods findGoodsById(Long id) {
        return goodsDao.selectById(id); // Assumes GoodsDao extends BaseMapper
    }

    /**
     * Retrieves a Goods entity by its ID, along with all its translations for internationalized fields.
     *
     * @param id The ID of the Goods entity.
     * @return A Map where the key "entity" holds the Goods object, and
     *         the key "translations" holds a map of its translations.
     *         The translations map is structured as: fieldName -> {languageCode -> translatedText}.
     *         Returns null if the entity is not found.
     */
    public Map<String, Object> getGoodsWithAllTranslations(Long id) {
        Goods goods = findGoodsById(id);
        if (goods == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("entity", goods);

        I18nResource i18nResource = AnnotationUtils.findAnnotation(Goods.class, I18nResource.class);
        if (i18nResource == null || goods.getId() == null) { // also check goods.getId() here
            result.put("translations", new HashMap<>()); // No translations if not I18N resource or no ID
            return result;
        }

        String prefix = i18nResource.prefix();
        String identityValue = goods.getId().toString(); 

        Map<String, Map<String, String>> allTranslations = new HashMap<>();
        // Iterate over the fields defined in @I18nResource to ensure we only fetch for configured fields
        for (I18nField i18nField : i18nResource.i18nFields()) {
            String fieldName = i18nField.fieldName();
            Map<String, String> fieldTrans = i18nDataService.getTranslationsForField(identityValue, prefix, fieldName);
            if (fieldTrans != null && !fieldTrans.isEmpty()) {
                allTranslations.put(fieldName, fieldTrans);
            }
        }
        result.put("translations", allTranslations);
        return result;
    }

    /**
     * Fetches all Goods entities.
     *
     * @return A list of all Goods entities.
     */
    public List<Goods> getAllGoods() {
        // Assuming goodsDao.selectList(null) fetches all records.
        // If using a more specific query (e.g., QueryWrapper), adjust accordingly.
        return goodsDao.selectList(null);
    }

    /**
     * Searches for Goods entities by keyword in name or description.
     *
     * @param keyword The keyword to search for.
     * @return A list of Goods entities matching the keyword.
     */
    public List<Goods> searchGoods(String keyword) {
        // 2. Handle blank keyword
        if (StringUtils.isBlank(keyword)) {
            return goodsDao.selectList(null); // Return all goods
        }

        // 3. Get internationalization resource information
        I18nResourceInfo resourceInfo = I18nResourceHelper.getResourceInfo(Goods.class);

        if (!resourceInfo.isInternationalizedResource()) {
            // Fallback to simple search on name and description fields in t_goods
            // This uses the new default method in GoodsDao
            Set<Long> ids = goodsDao.findIdsByKeywordAndFields(keyword, Arrays.asList("name", "description"));
            if (ids.isEmpty()) {
                return Collections.emptyList();
            }
            return goodsDao.selectList(new LambdaQueryWrapper<Goods>().in(Goods::getId, ids));
        }

        // 4. Get language settings and constants
        String currentLanguage = LocaleContextHolder.getLocale().toString();
        String defaultLanguage = "zh_CN"; // Consider making this configurable
        String i18nType = "数据库表"; // This corresponds to I18nDataService.DEFAULT_TYPE_DB_FIELD

        // 5. Initialize ID set
        Set<Long> matchedGoodsIds = new HashSet<>();

        // 6. Branch 1: Search i18n_message table (current language translations)
        String prefix = resourceInfo.getPrefix();
        List<String> i18nActualFieldNames = resourceInfo.getI18nActualFieldNames();

        // Condition to search i18n_message: if fields are configured and current lang is not default
        if (!i18nActualFieldNames.isEmpty() && !currentLanguage.equals(defaultLanguage)) {
            for (String fieldName : i18nActualFieldNames) {
                String codePrefixPattern = prefix + "." + fieldName + ".";
                Set<Long> idsFromTranslations = i18nMessageMapper.findIdentityKeysByTextSearch(
                        keyword, currentLanguage, i18nType, codePrefixPattern);
                if (idsFromTranslations != null) { // findIdentityKeysByTextSearch returns empty set, not null
                    matchedGoodsIds.addAll(idsFromTranslations);
                }
            }
        }

        // 7. Branch 2: Search t_goods table (default language texts)
        // searchableFieldsInGoodsTable are the same as i18nActualFieldNames for this entity
        if (!i18nActualFieldNames.isEmpty()) {
            Set<Long> idsFromDefaultLang = goodsDao.findIdsByKeywordAndFields(keyword, i18nActualFieldNames);
            if (idsFromDefaultLang != null) { // findIdsByKeywordAndFields returns empty set, not null
                matchedGoodsIds.addAll(idsFromDefaultLang);
            }
        }

        // 8. Return results
        if (matchedGoodsIds.isEmpty()) {
            return Collections.emptyList();
        }
        return goodsDao.selectList(new LambdaQueryWrapper<Goods>().in(Goods::getId, matchedGoodsIds));
    }
}
