package com.veystream.service;

import com.veystream.dao.I18nMessage;
import com.veystream.dao.I18nMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class I18nDataServiceTest {

    @Mock
    private I18nMessageMapper i18nMessageMapper;

    @InjectMocks
    private I18nDataService i18nDataService;

    private String testPrefix;
    private String testFieldName;
    private String testIdentityKey;
    private String testBaseCode;
    private String defaultTypeDbField;

    @BeforeEach
    void setUp() {
        testPrefix = "TestEntity";
        testFieldName = "name";
        testIdentityKey = "123";
        testBaseCode = testPrefix + "." + testFieldName + "." + testIdentityKey;
        // Reflecting the constant in I18nDataService
        defaultTypeDbField = "数据库表"; 
    }

    @Test
    void saveOrUpdateTranslations_shouldInsertNewTranslations() {
        Map<String, String> translations = new HashMap<>();
        translations.put("en_US", "English Name");
        translations.put("zh_CN", "中文名称");

        when(i18nMessageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        i18nDataService.saveOrUpdateTranslations(testIdentityKey, testPrefix, testFieldName, translations, defaultTypeDbField);

        ArgumentCaptor<I18nMessage> messageCaptor = ArgumentCaptor.forClass(I18nMessage.class);
        verify(i18nMessageMapper, times(2)).insert(messageCaptor.capture());

        List<I18nMessage> capturedMessages = messageCaptor.getAllValues();
        assertEquals(2, capturedMessages.size());

        assertTrue(capturedMessages.stream().anyMatch(m -> 
            m.getCode().equals(testBaseCode) && 
            m.getLanguage().equals("en_US") && 
            m.getText().equals("English Name") &&
            m.getType().equals(defaultTypeDbField)
        ));
        assertTrue(capturedMessages.stream().anyMatch(m -> 
            m.getCode().equals(testBaseCode) && 
            m.getLanguage().equals("zh_CN") && 
            m.getText().equals("中文名称") &&
            m.getType().equals(defaultTypeDbField)
        ));
    }

    @Test
    void saveOrUpdateTranslations_shouldUpdateExistingTranslations() {
        Map<String, String> translations = new HashMap<>();
        translations.put("en_US", "Updated English Name");

        I18nMessage existingMessage = new I18nMessage();
        existingMessage.setId(1L);
        existingMessage.setCode(testBaseCode);
        existingMessage.setLanguage("en_US");
        existingMessage.setText("Old English Name");
        existingMessage.setType(defaultTypeDbField);

        // Mock selectOne to return the existing message for the "en_US" language
        when(i18nMessageMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenAnswer(invocation -> {
                LambdaQueryWrapper<I18nMessage> wrapper = invocation.getArgument(0);
                // This is a simplified mock. A real test might need more sophisticated argument matching
                // to correctly simulate the behavior of the LambdaQueryWrapper.
                // For this test, we assume it finds the existingMessage for "en_US".
                // A more robust mock might check the wrapper's SQL for specific conditions.
                // For instance, if the wrapper is checking for code=testBaseCode and language="en_US".
                // However, directly inspecting LambdaQueryWrapper internals for specific criteria
                // without executing it against a test database or a more complex mock setup is tricky.
                // This simplified approach relies on the test's specific setup: only one language is being processed.
                return existingMessage; 
            });

        i18nDataService.saveOrUpdateTranslations(testIdentityKey, testPrefix, testFieldName, translations, defaultTypeDbField);

        ArgumentCaptor<I18nMessage> messageCaptor = ArgumentCaptor.forClass(I18nMessage.class);
        verify(i18nMessageMapper, times(1)).updateById(messageCaptor.capture());
        verify(i18nMessageMapper, never()).insert(any(I18nMessage.class));

        I18nMessage updatedMessage = messageCaptor.getValue();
        assertEquals("Updated English Name", updatedMessage.getText());
        assertEquals(existingMessage.getId(), updatedMessage.getId()); // Ensure it's an update
        assertNotNull(updatedMessage.getUpdatedTime());
    }
    
    @Test
    void saveOrUpdateTranslations_shouldUseCustomTypeWhenProvided() {
        Map<String, String> translations = new HashMap<>();
        translations.put("en_US", "English Name");
        String customType = "CUSTOM_TYPE";

        when(i18nMessageMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        i18nDataService.saveOrUpdateTranslations(testIdentityKey, testPrefix, testFieldName, translations, customType);

        ArgumentCaptor<I18nMessage> messageCaptor = ArgumentCaptor.forClass(I18nMessage.class);
        verify(i18nMessageMapper).insert(messageCaptor.capture());
        assertEquals(customType, messageCaptor.getValue().getType());
    }


    @Test
    void getTranslationsForField_shouldReturnMapOfTranslations() {
        I18nMessage enMessage = new I18nMessage();
        enMessage.setLanguage("en_US");
        enMessage.setText("English Text");
        enMessage.setCode(testBaseCode);

        I18nMessage zhMessage = new I18nMessage();
        zhMessage.setLanguage("zh_CN");
        zhMessage.setText("中文文本");
        zhMessage.setCode(testBaseCode);

        List<I18nMessage> messages = Arrays.asList(enMessage, zhMessage);
        when(i18nMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(messages);

        Map<String, String> translations = i18nDataService.getTranslationsForField(testIdentityKey, testPrefix, testFieldName);

        assertEquals(2, translations.size());
        assertEquals("English Text", translations.get("en_US"));
        assertEquals("中文文本", translations.get("zh_CN"));
    }

    @Test
    void getTranslationsForField_shouldReturnEmptyMapWhenNoTranslations() {
        when(i18nMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        Map<String, String> translations = i18nDataService.getTranslationsForField(testIdentityKey, testPrefix, testFieldName);
        assertTrue(translations.isEmpty());
    }

    @Test
    void getTranslationMessagesForField_shouldReturnListOfMessages() {
        I18nMessage enMessage = new I18nMessage();
        enMessage.setLanguage("en_US");
        enMessage.setText("English Text");
        List<I18nMessage> messages = Collections.singletonList(enMessage);

        when(i18nMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(messages);

        List<I18nMessage> result = i18nDataService.getTranslationMessagesForField(testIdentityKey, testPrefix, testFieldName);

        assertEquals(1, result.size());
        assertSame(enMessage, result.get(0));
    }

    @Test
    void getTranslationMessagesForField_shouldReturnEmptyListWhenNoMessages() {
        when(i18nMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        List<I18nMessage> result = i18nDataService.getTranslationMessagesForField(testIdentityKey, testPrefix, testFieldName);
        assertTrue(result.isEmpty());
    }
}
