package com.veystream.service;

import com.veystream.annotation.I18nField;
import com.veystream.annotation.I18nResource;
import com.veystream.dao.GoodsDao;
import com.veystream.entity.Goods;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.annotation.AnnotationUtils; // Correct import for AnnotationUtils

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// import java.util.Objects; // Not strictly needed with current logic but good for general use

@Service
public class GoodsService {

    @Resource
    private GoodsDao goodsDao;

    @Resource
    private I18nDataService i18nDataService; // Ensure this can be injected from i18n-common

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
}
