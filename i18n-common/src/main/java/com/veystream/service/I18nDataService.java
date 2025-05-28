package com.veystream.service;

import com.veystream.dao.I18nMessage;
import com.veystream.dao.I18nMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class I18nDataService {

    @Resource
    private I18nMessageMapper i18nMessageMapper;

    // TODO: Review if a central constant/enum exists for I18nMessage types
    // For example, in I18nMessage.java, the comment for 'type' mentions:
    // "类型：常量；枚举；错误码；数据库表；" - "数据库表" seems to be the relevant one.
    private static final String DEFAULT_TYPE_DB_FIELD = "数据库表"; 

    /**
     * Saves or updates translations for a specific field of an entity.
     *
     * @param identityKey  The unique identifier of the entity instance (e.g., product ID).
     * @param prefix       The prefix for the translation code (e.g., "Goods" from @I18nResource).
     * @param fieldName    The name of the field being translated (e.g., "name" from @I18nField).
     * @param translations A map where keys are language codes (e.g., "en_US", "zh_CN")
     *                     and values are the translated texts.
     * @param type         The type of the i18n message (e.g., "数据库表"). If null or empty, 
     *                     a default type (DEFAULT_TYPE_DB_FIELD) will be used.
     */
    @Transactional
    public void saveOrUpdateTranslations(String identityKey, String prefix, String fieldName, Map<String, String> translations, String type) {
        if (translations == null || translations.isEmpty()) {
            return;
        }

        // Construct the base code, e.g., "Goods.name.123"
        String baseCode = prefix + "." + fieldName + "." + identityKey;
        
        // Determine the message type
        String messageType = (type != null && !type.trim().isEmpty()) ? type.trim() : DEFAULT_TYPE_DB_FIELD;

        for (Map.Entry<String, String> entry : translations.entrySet()) {
            String language = entry.getKey();
            String text = entry.getValue();

            // Check if a translation already exists for this code and language
            LambdaQueryWrapper<I18nMessage> queryWrapper = new LambdaQueryWrapper<I18nMessage>()
                    .eq(I18nMessage::getCode, baseCode)
                    .eq(I18nMessage::getLanguage, language);
            I18nMessage existingMessage = i18nMessageMapper.selectOne(queryWrapper);

            if (existingMessage != null) {
                // Update existing translation
                existingMessage.setText(text);
                existingMessage.setType(messageType); // Ensure type is also updated if it changes or was different
                existingMessage.setUpdatedTime(LocalDateTime.now());
                i18nMessageMapper.updateById(existingMessage);
            } else {
                // Create new translation entry
                I18nMessage newMessage = new I18nMessage();
                newMessage.setCode(baseCode);
                newMessage.setLanguage(language);
                newMessage.setText(text);
                newMessage.setType(messageType);
                newMessage.setCreatedTime(LocalDateTime.now());
                newMessage.setUpdatedTime(LocalDateTime.now());
                i18nMessageMapper.insert(newMessage);
            }
        }
    }

    /**
     * Retrieves all translations for a specific field of an entity instance 
     * as a map of language code to translated text.
     *
     * @param identityKey The unique identifier of the entity instance.
     * @param prefix      The prefix for the translation code.
     * @param fieldName   The name of the field.
     * @return A Map where keys are language codes and values are the translated texts.
     *         Returns an empty map if no translations are found.
     */
    public Map<String, String> getTranslationsForField(String identityKey, String prefix, String fieldName) {
        String baseCode = prefix + "." + fieldName + "." + identityKey;

        LambdaQueryWrapper<I18nMessage> queryWrapper = new LambdaQueryWrapper<I18nMessage>()
                .eq(I18nMessage::getCode, baseCode);
        List<I18nMessage> messages = i18nMessageMapper.selectList(queryWrapper);

        if (messages == null || messages.isEmpty()) {
            return Collections.emptyMap(); // Return an empty map if no messages are found
        }

        // Convert the list of messages to a map of language -> text
        return messages.stream()
                .collect(Collectors.toMap(I18nMessage::getLanguage, I18nMessage::getText));
    }

    /**
     * Retrieves all I18nMessage objects for a specific field of an entity instance.
     * This method can be useful if the caller needs more than just the text, e.g., created/updated times.
     *
     * @param identityKey The unique identifier of the entity instance.
     * @param prefix      The prefix for the translation code.
     * @param fieldName   The name of the field.
     * @return A List of I18nMessage objects. Returns an empty list if none are found.
     */
    public List<I18nMessage> getTranslationMessagesForField(String identityKey, String prefix, String fieldName) {
        String baseCode = prefix + "." + fieldName + "." + identityKey;
        LambdaQueryWrapper<I18nMessage> queryWrapper = new LambdaQueryWrapper<I18nMessage>()
                .eq(I18nMessage::getCode, baseCode);
        List<I18nMessage> messages = i18nMessageMapper.selectList(queryWrapper);
        
        return messages != null ? messages : Collections.emptyList(); // Return an empty list if messages is null
    }
}
