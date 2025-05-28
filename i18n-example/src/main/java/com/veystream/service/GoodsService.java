package com.veystream.service;

import com.veystream.annotation.I18nField;
import com.veystream.annotation.I18nResource;
import com.veystream.dao.GoodsDao;
import com.veystream.entity.Goods;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.context.i18n.LocaleContextHolder; // Added
import com.veystream.dao.I18nMessageMapper; // Added
import com.veystream.dao.I18nMessage; // Added

import javax.annotation.Resource;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.commons.lang3.StringUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set; // Added
import java.util.HashSet; // Added
import java.util.Collections; // Added
import java.util.stream.Collectors; // Added for parsing IDs safely

// import java.util.Objects; // Not strictly needed with current logic but good for general use

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
        // 2. Get language settings
        String currentLanguage = LocaleContextHolder.getLocale().toString();
        String defaultLanguage = "zh_CN"; // Default language

        // 3. Handle blank keyword
        if (StringUtils.isBlank(keyword)) {
            return goodsDao.selectList(null); // Return all goods or Collections.emptyList()
        }

        // 4. Get @I18nResource annotation information
        I18nResource i18nResource = AnnotationUtils.findAnnotation(Goods.class, I18nResource.class);

        if (i18nResource == null) {
            // Fallback to original simple search logic
            LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.like(Goods::getName, keyword)
                        .or()
                        .like(Goods::getDescription, keyword);
            return goodsDao.selectList(queryWrapper);
        }

        String prefix = i18nResource.prefix();
        I18nField[] i18nFields = i18nResource.i18nFields();
        // String identityKeyName = i18nResource.identityKey(); // Not directly used for parsing here

        // 5. Initialize ID set
        Set<Long> matchedGoodsIds = new HashSet<>();

        // 6. Branch 1: Search i18n_message table (current language translations)
        // Only search i18n_message if current language is not default, to avoid duplicate searching
        // if default language texts are also in i18n_message.
        // This condition can be adjusted based on specific requirements.
        if (i18nFields.length > 0 && !currentLanguage.equals(defaultLanguage)) {
            for (I18nField field : i18nFields) {
                String fieldName = field.fieldName();
                LambdaQueryWrapper<I18nMessage> i18nQuery = new LambdaQueryWrapper<>();
                i18nQuery.eq(I18nMessage::getLanguage, currentLanguage);
                // I18nDataService.DEFAULT_TYPE_DB_FIELD is private, using string literal
                i18nQuery.eq(I18nMessage::getType, "数据库表"); 
                // Search by code prefix: "Goods.name."
                String codePrefixToSearch = prefix + "." + fieldName + ".";
                i18nQuery.likeRight(I18nMessage::getCode, codePrefixToSearch);
                i18nQuery.like(I18nMessage::getText, keyword);

                List<I18nMessage> messages = i18nMessageMapper.selectList(i18nQuery);
                for (I18nMessage message : messages) {
                    String code = message.getCode();
                    try {
                        // Robust ID parsing: extract substring after the last dot.
                        String idStr = code.substring(code.lastIndexOf('.') + 1);
                        if (StringUtils.isNotBlank(idStr) && idStr.matches("\\d+")) { // Check if it's a number
                            matchedGoodsIds.add(Long.parseLong(idStr));
                        }
                    } catch (Exception e) {
                        // Log error or handle parsing exception if necessary
                        System.err.println("Error parsing ID from code: " + code + " - " + e.getMessage());
                    }
                }
            }
        }

        // 7. Branch 2: Search t_goods table (default language texts)
        if (i18nFields.length > 0) {
            LambdaQueryWrapper<Goods> goodsQuery = new LambdaQueryWrapper<>();
            goodsQuery.and(wrapper -> {
                boolean firstField = true;
                for (I18nField field : i18nFields) {
                    String entityFieldName = field.fieldName();
                    if (!firstField) {
                        wrapper.or();
                    }
                    if ("name".equals(entityFieldName)) {
                        wrapper.like(Goods::getName, keyword);
                    } else if ("description".equals(entityFieldName)) {
                        wrapper.like(Goods::getDescription, keyword);
                    } else if ("image".equals(entityFieldName)) {
                        // Assuming 'image' field is also searchable, if specified in @I18nField
                         wrapper.like(Goods::getImage, keyword);
                    }
                    // Add more 'else if' for other fields if they are configured in @I18nField
                    // and are present as actual columns in the Goods entity
                    firstField = false;
                }
            });
            List<Goods> defaultLangGoods = goodsDao.selectList(goodsQuery);
            if (defaultLangGoods != null) {
                defaultLangGoods.forEach(g -> matchedGoodsIds.add(g.getId()));
            }
        }


        // 8. Return results
        if (matchedGoodsIds.isEmpty()) {
            return Collections.emptyList();
        }
        return goodsDao.selectList(new LambdaQueryWrapper<Goods>().in(Goods::getId, matchedGoodsIds));
    }
}
