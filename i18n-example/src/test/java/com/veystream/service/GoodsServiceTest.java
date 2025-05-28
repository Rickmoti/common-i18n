package com.veystream.service;

import com.veystream.dao.GoodsDao;
import com.veystream.entity.Goods;
import com.veystream.annotation.I18nField; // Required for I18nResource
import com.veystream.annotation.I18nResource; // Required for simulating annotation presence
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder; // Added for locale mocking
import org.springframework.core.annotation.AnnotationUtils;

import com.veystream.dao.I18nMessageMapper; // Added
import com.veystream.dao.I18nMessage; // Added

import java.util.ArrayList;
import java.util.HashSet; // Added
import java.util.Locale; // Added for Locale
import java.util.Set; // Added
import java.util.stream.Collectors; // Added
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper; // Added for searchGoods tests

// Simulate the @I18nResource annotation for Goods
@I18nResource(
    prefix = "Goods", 
    identityKey = "id", 
    i18nFields = {
        @I18nField(fieldName = "name"),
        @I18nField(fieldName = "description")
    }
)
class MockedGoods {} // Used to provide annotations for testing

@ExtendWith(MockitoExtension.class)
public class GoodsServiceTest {

    @Mock
    private GoodsDao goodsDao;

    @Mock
    private I18nDataService i18nDataService;

    @Mock
    private I18nMessageMapper i18nMessageMapper; // Added

    @InjectMocks
    private GoodsService goodsService;

    private Goods testGoods;
    private static final String DEFAULT_LANGUAGE = "zh_CN";
    private static final String I18N_MESSAGE_TYPE_DB = "数据库表";
    private static final String TEST_KEYWORD = "test keyword";
    private I18nResource goodsI18nResource;

    @BeforeEach
    void setUp() {
        testGoods = new Goods();
        testGoods.setId(1L);
        testGoods.setName("Default Name");
        testGoods.setDescription("Default Description");
        
        // Get the annotation from our mocked class
        goodsI18nResource = AnnotationUtils.findAnnotation(MockedGoods.class, I18nResource.class);
    }

    @Test
    void createGoods_shouldSaveGoodsAndTranslations() {
        Goods newGoods = new Goods(); // No ID initially
        newGoods.setName("Test Name");
        
        Map<String, Map<String, String>> allTranslations = new HashMap<>();
        Map<String, String> nameTranslations = new HashMap<>();
        nameTranslations.put("en_US", "English Name");
        allTranslations.put("name", nameTranslations);

        // Mock AnnotationUtils.findAnnotation to return our sample annotation
        // Mock the behavior of goodsDao.insert to set an ID on newGoods
        doAnswer(invocation -> {
            Goods g = invocation.getArgument(0);
            g.setId(2L); // Simulate ID generation
            return null;
        }).when(goodsDao).insert(any(Goods.class));

        try (MockedStatic<AnnotationUtils> mockedAnnotationUtils = Mockito.mockStatic(AnnotationUtils.class)) {
            mockedAnnotationUtils.when(() -> AnnotationUtils.findAnnotation(eq(Goods.class), eq(I18nResource.class)))
                                 .thenReturn(goodsI18nResource);

            goodsService.createGoods(newGoods, allTranslations);

            verify(goodsDao).insert(newGoods);
            assertNotNull(newGoods.getId()); // Check if ID was set

            // Verify I18nDataService was called for "name" field
            verify(i18nDataService).saveOrUpdateTranslations(
                eq(newGoods.getId().toString()), // ID from the saved goods
                eq(goodsI18nResource.prefix()),
                eq("name"),
                eq(nameTranslations),
                isNull() // Default type
            );
        }
    }
    
    @Test
    void createGoods_shouldNotSaveTranslationsIfNoAnnotation() {
        Goods newGoods = new Goods();
        newGoods.setName("Test Name");
        Map<String, Map<String, String>> allTranslations = new HashMap<>(); // Empty or null

        try (MockedStatic<AnnotationUtils> mockedAnnotationUtils = Mockito.mockStatic(AnnotationUtils.class)) {
            mockedAnnotationUtils.when(() -> AnnotationUtils.findAnnotation(eq(Goods.class), eq(I18nResource.class)))
                                 .thenReturn(null); // Simulate no annotation

            goodsService.createGoods(newGoods, allTranslations);
            
            verify(goodsDao).insert(newGoods);
            verify(i18nDataService, never()).saveOrUpdateTranslations(any(), any(), any(), any(), any());
        }
    }


    @Test
    void updateGoods_shouldUpdateGoodsAndTranslations() {
        Map<String, Map<String, String>> allTranslations = new HashMap<>();
        Map<String, String> descriptionTranslations = new HashMap<>();
        descriptionTranslations.put("fr_FR", "Description en français");
        allTranslations.put("description", descriptionTranslations);
        
        testGoods.setDescription("New Default Description"); // Simulate an update to the main entity

        try (MockedStatic<AnnotationUtils> mockedAnnotationUtils = Mockito.mockStatic(AnnotationUtils.class)) {
            mockedAnnotationUtils.when(() -> AnnotationUtils.findAnnotation(eq(Goods.class), eq(I18nResource.class)))
                                 .thenReturn(goodsI18nResource);

            goodsService.updateGoods(testGoods, allTranslations);

            verify(goodsDao).updateById(testGoods);
            verify(i18nDataService).saveOrUpdateTranslations(
                eq(testGoods.getId().toString()),
                eq(goodsI18nResource.prefix()),
                eq("description"),
                eq(descriptionTranslations),
                isNull()
            );
        }
    }
    
    @Test
    void updateGoods_shouldThrowExceptionIfIdIsNull() {
        Goods goodsWithoutId = new Goods();
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            goodsService.updateGoods(goodsWithoutId, new HashMap<>());
        });
        assertEquals("Goods ID cannot be null for update.", exception.getMessage());
    }

    @Test
    void findGoodsById_shouldReturnGoods() {
        when(goodsDao.selectById(1L)).thenReturn(testGoods);
        Goods found = goodsService.findGoodsById(1L);
        assertSame(testGoods, found);
    }

    @Test
    void getGoodsWithAllTranslations_shouldReturnEntityAndTranslations() {
        when(goodsDao.selectById(1L)).thenReturn(testGoods);
        
        Map<String, String> nameTranslations = Collections.singletonMap("en_US", "English Name");
        Map<String, String> descTranslations = Collections.singletonMap("en_US", "English Description");

        try (MockedStatic<AnnotationUtils> mockedAnnotationUtils = Mockito.mockStatic(AnnotationUtils.class)) {
            mockedAnnotationUtils.when(() -> AnnotationUtils.findAnnotation(eq(Goods.class), eq(I18nResource.class)))
                                 .thenReturn(goodsI18nResource);
            
            // Mock calls for each field defined in MockedGoods's @I18nResource
            when(i18nDataService.getTranslationsForField(eq(testGoods.getId().toString()), eq(goodsI18nResource.prefix()), eq("name")))
                .thenReturn(nameTranslations);
            when(i18nDataService.getTranslationsForField(eq(testGoods.getId().toString()), eq(goodsI18nResource.prefix()), eq("description")))
                .thenReturn(descTranslations);

            Map<String, Object> result = goodsService.getGoodsWithAllTranslations(1L);

            assertNotNull(result);
            assertSame(testGoods, result.get("entity"));
            Map<String, Map<String, String>> translations = (Map<String, Map<String, String>>) result.get("translations");
            assertNotNull(translations);
            assertEquals(nameTranslations, translations.get("name"));
            assertEquals(descTranslations, translations.get("description"));
            
            // Ensure it iterated through the fields in the annotation
            verify(i18nDataService).getTranslationsForField(anyString(), anyString(), eq("name"));
            verify(i18nDataService).getTranslationsForField(anyString(), anyString(), eq("description"));
        }
    }
    
    @Test
    void getGoodsWithAllTranslations_shouldReturnNullIfEntityNotFound() {
        when(goodsDao.selectById(anyLong())).thenReturn(null);
        Map<String, Object> result = goodsService.getGoodsWithAllTranslations(99L);
        assertNull(result);
    }
    
    @Test
    void getGoodsWithAllTranslations_shouldReturnEmptyTranslationsIfNotI18nResource() {
         when(goodsDao.selectById(1L)).thenReturn(testGoods);
         
        try (MockedStatic<AnnotationUtils> mockedAnnotationUtils = Mockito.mockStatic(AnnotationUtils.class)) {
            mockedAnnotationUtils.when(() -> AnnotationUtils.findAnnotation(eq(Goods.class), eq(I18nResource.class)))
                                 .thenReturn(null); // Simulate no annotation

            Map<String, Object> result = goodsService.getGoodsWithAllTranslations(1L);
            
            assertNotNull(result);
            assertSame(testGoods, result.get("entity"));
            Map<String, Map<String, String>> translations = (Map<String, Map<String, String>>) result.get("translations");
            assertTrue(translations.isEmpty());
        }
    }

    @Test
    void getAllGoods_shouldReturnListOfGoods() {
        List<Goods> goodsList = Arrays.asList(testGoods, new Goods());
        when(goodsDao.selectList(null)).thenReturn(goodsList);

        List<Goods> result = goodsService.getAllGoods();

        assertEquals(2, result.size());
        assertSame(goodsList, result);
        verify(goodsDao).selectList(null);
    }

    @Test
    // Helper to create mock I18nMessage
    private I18nMessage createMockI18nMessage(String code, String text, String language) {
        I18nMessage msg = new I18nMessage();
        msg.setCode(code);
        msg.setText(text);
        msg.setLanguage(language);
        msg.setType(I18N_MESSAGE_TYPE_DB);
        return msg;
    }

    @Test
    void searchGoods_whenKeywordIsBlank_shouldReturnAllGoods() {
        List<Goods> allGoods = Arrays.asList(new Goods(), new Goods());
        when(goodsDao.selectList(isNull())).thenReturn(allGoods); // selectList(null) for all goods

        // Test with null keyword
        List<Goods> resultNull = goodsService.searchGoods(null);
        assertSame(allGoods, resultNull);
        verify(goodsDao).selectList(isNull());
        verifyNoInteractions(i18nMessageMapper);

        // Test with empty keyword
        List<Goods> resultEmpty = goodsService.searchGoods("");
        assertSame(allGoods, resultEmpty);
        verify(goodsDao, times(2)).selectList(isNull());
        verifyNoInteractions(i18nMessageMapper); // Still no interaction

        // Test with blank keyword
        List<Goods> resultBlank = goodsService.searchGoods("   ");
        assertSame(allGoods, resultBlank);
        verify(goodsDao, times(3)).selectList(isNull());
        verifyNoInteractions(i18nMessageMapper); // Still no interaction
    }

    @Test
    void searchGoods_whenNoI18nAnnotation_shouldFallBackToSimpleSearchOnGoodsTable() {
        List<Goods> expectedGoods = Arrays.asList(new Goods());
        String keyword = "simple search";

        try (MockedStatic<AnnotationUtils> mockedAnnotationUtils = Mockito.mockStatic(AnnotationUtils.class);
             MockedStatic<LocaleContextHolder> mockedLocaleHolder = Mockito.mockStatic(LocaleContextHolder.class)) {
            
            mockedLocaleHolder.when(LocaleContextHolder::getLocale).thenReturn(Locale.forLanguageTag(DEFAULT_LANGUAGE));
            mockedAnnotationUtils.when(() -> AnnotationUtils.findAnnotation(eq(Goods.class), eq(I18nResource.class)))
                                 .thenReturn(null); // No annotation

            when(goodsDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(expectedGoods);

            List<Goods> actualGoods = goodsService.searchGoods(keyword);

            assertSame(expectedGoods, actualGoods);
            ArgumentCaptor<LambdaQueryWrapper<Goods>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            verify(goodsDao).selectList(captor.capture());
            // Verify the wrapper contains LIKE for name and description (simplified check)
            String sql = captor.getValue().getCustomSqlSegment();
            assertTrue(sql.toLowerCase().contains("name like") && sql.toLowerCase().contains("description like"));
            verifyNoInteractions(i18nMessageMapper);
        }
    }

    @Test
    void searchGoods_findsInCurrentLanguageTranslationsOnly() {
        String currentLang = "en_US";
        String keyword = "english text";
        Goods g1 = new Goods(); g1.setId(1L);
        Goods g2 = new Goods(); g2.setId(2L);
        List<Goods> expectedGoods = Arrays.asList(g1, g2);
        
        I18nMessage msg1 = createMockI18nMessage("Goods.name.1", keyword, currentLang);
        I18nMessage msg2 = createMockI18nMessage("Goods.description.2", keyword, currentLang);

        try (MockedStatic<AnnotationUtils> mockedAnnotationUtils = Mockito.mockStatic(AnnotationUtils.class);
             MockedStatic<LocaleContextHolder> mockedLocaleHolder = Mockito.mockStatic(LocaleContextHolder.class)) {

            mockedLocaleHolder.when(LocaleContextHolder::getLocale).thenReturn(Locale.forLanguageTag(currentLang));
            mockedAnnotationUtils.when(() -> AnnotationUtils.findAnnotation(eq(Goods.class), eq(I18nResource.class)))
                                 .thenReturn(goodsI18nResource);

            // Branch 1: i18n messages for current language
            when(i18nMessageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> {
                    LambdaQueryWrapper<I18nMessage> wrapper = invocation.getArgument(0);
                    // Simple mock: if it queries for currentLang, return msg1 & msg2
                    // A more robust mock would check wrapper.getCustomSqlSegment() for language, type, code prefix, and keyword.
                    // This requires more complex argument matching. For this test, we assume the service builds the query correctly for this branch.
                    return Arrays.asList(msg1, msg2);
                });
            
            // Branch 2: t_goods for default language - return empty
            when(goodsDao.selectList(argThat(w -> w != null && w.getCustomSqlSegment() != null && w.getCustomSqlSegment().contains(DEFAULT_LANGUAGE))))
                .thenReturn(Collections.emptyList());
            // More specific for default goods search:
             when(goodsDao.selectList(argThat(w -> {
                String sql = w.getCustomSqlSegment();
                return sql != null && (sql.contains("name LIKE") || sql.contains("description LIKE")); // Check if it's the default goods search
            }))).thenReturn(Collections.emptyList());


            // Final retrieval by IDs
            when(goodsDao.selectList(argThat(w -> w.getCustomSqlSegment().contains("id IN (1,2)") || w.getCustomSqlSegment().contains("id IN (2,1)"))))
                .thenReturn(expectedGoods);


            List<Goods> actualGoods = goodsService.searchGoods(keyword);

            assertEquals(2, actualGoods.size());
            assertTrue(actualGoods.containsAll(expectedGoods));

            verify(i18nMessageMapper).selectList(any(LambdaQueryWrapper.class)); // Called for current lang
             // Called for default lang goods search (should be empty)
            verify(goodsDao, times(1)).selectList(argThat(w -> w.getCustomSqlSegment() != null && (w.getCustomSqlSegment().contains("name LIKE") || w.getCustomSqlSegment().contains("description LIKE"))));
            verify(goodsDao, times(1)).selectList(argThat(w -> w.getCustomSqlSegment().contains("id IN"))); // Final call
        }
    }

    @Test
    void searchGoods_findsInDefaultLanguageGoodsTableOnly() {
        String currentLang = "en_US"; // Non-default
        Goods g3 = new Goods(); g3.setId(3L); g3.setName(TEST_KEYWORD);
        Goods g4 = new Goods(); g4.setId(4L); g4.setDescription(TEST_KEYWORD);
        List<Goods> goodsFromDefaultTable = Arrays.asList(g3, g4);
        
        try (MockedStatic<AnnotationUtils> mockedAnnotationUtils = Mockito.mockStatic(AnnotationUtils.class);
             MockedStatic<LocaleContextHolder> mockedLocaleHolder = Mockito.mockStatic(LocaleContextHolder.class)) {
            
            mockedLocaleHolder.when(LocaleContextHolder::getLocale).thenReturn(Locale.forLanguageTag(currentLang));
            mockedAnnotationUtils.when(() -> AnnotationUtils.findAnnotation(eq(Goods.class), eq(I18nResource.class)))
                                 .thenReturn(goodsI18nResource);

            // Branch 1: i18n messages (current lang) - return empty
            when(i18nMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            
            // Branch 2: t_goods (default lang) - return g3, g4
            // This ArgumentMatcher checks if the query is for Goods and has LIKE conditions
            when(goodsDao.selectList(argThat(w -> {
                String sql = w.getCustomSqlSegment();
                return sql != null && (sql.contains("name LIKE") || sql.contains("description LIKE"));
            }))).thenReturn(goodsFromDefaultTable);

            // Final retrieval by IDs {3, 4}
            Set<Long> expectedIds = new HashSet<>(Arrays.asList(3L, 4L));
            when(goodsDao.selectList(argThat(w -> {
                 // A simple way to check if the IN clause contains the expected IDs.
                 // This is a simplified check. A full SQL parsing is too complex for a unit test.
                 String sql = w.getCustomSqlSegment().toLowerCase();
                 return sql.contains("in") && expectedIds.stream().allMatch(id -> sql.contains(id.toString()));
            }))).thenReturn(goodsFromDefaultTable);


            List<Goods> actualGoods = goodsService.searchGoods(TEST_KEYWORD);

            assertEquals(2, actualGoods.size());
            assertTrue(actualGoods.containsAll(goodsFromDefaultTable));

            verify(i18nMessageMapper).selectList(any(LambdaQueryWrapper.class)); // Called for current lang
            verify(goodsDao, times(1)).selectList(argThat(w -> w.getCustomSqlSegment() != null && (w.getCustomSqlSegment().contains("name LIKE") || w.getCustomSqlSegment().contains("description LIKE")))); // Default goods search
            verify(goodsDao, times(1)).selectList(argThat(w -> w.getCustomSqlSegment().contains("id IN"))); // Final call
        }
    }
    
    @Test
    void searchGoods_findsInBothSources_shouldMergeAndDeduplicateIds() {
        String currentLang = "en_US";
        Goods g1 = new Goods(); g1.setId(1L); // From i18n
        Goods g2 = new Goods(); g2.setId(2L); // From i18n & default
        Goods g3 = new Goods(); g3.setId(3L); // From default
        List<Goods> expectedFinalGoods = Arrays.asList(g1, g2, g3);
        Set<Long> expectedFinalIds = expectedFinalGoods.stream().map(Goods::getId).collect(Collectors.toSet());

        I18nMessage msg1 = createMockI18nMessage("Goods.name.1", TEST_KEYWORD, currentLang);
        I18nMessage msg2 = createMockI18nMessage("Goods.description.2", TEST_KEYWORD, currentLang);
        List<I18nMessage> i18nResults = Arrays.asList(msg1, msg2);

        Goods g2Default = new Goods(); g2Default.setId(2L); g2Default.setName(TEST_KEYWORD);
        Goods g3Default = new Goods(); g3Default.setId(3L); g3Default.setDescription(TEST_KEYWORD);
        List<Goods> defaultTableResults = Arrays.asList(g2Default, g3Default);

        try (MockedStatic<AnnotationUtils> mockedAnnotationUtils = Mockito.mockStatic(AnnotationUtils.class);
             MockedStatic<LocaleContextHolder> mockedLocaleHolder = Mockito.mockStatic(LocaleContextHolder.class)) {

            mockedLocaleHolder.when(LocaleContextHolder::getLocale).thenReturn(Locale.forLanguageTag(currentLang));
            mockedAnnotationUtils.when(() -> AnnotationUtils.findAnnotation(eq(Goods.class), eq(I18nResource.class)))
                                 .thenReturn(goodsI18nResource);
            
            // Branch 1: i18n
            when(i18nMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(i18nResults);
            // Branch 2: default goods
            when(goodsDao.selectList(argThat(w -> w.getCustomSqlSegment()!=null && (w.getCustomSqlSegment().contains("name LIKE")||w.getCustomSqlSegment().contains("description LIKE")) )))
                .thenReturn(defaultTableResults);

            // Final retrieval
            ArgumentCaptor<LambdaQueryWrapper<Goods>> finalQueryCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
            when(goodsDao.selectList(finalQueryCaptor.capture())).thenAnswer(inv -> {
                 // Based on the captured IDs, return the corresponding goods
                 LambdaQueryWrapper<Goods> capturedWrapper = inv.getArgument(0);
                 String sql = capturedWrapper.getCustomSqlSegment();
                 // Simplified check for IN clause content
                 if (sql.contains("id IN (1,2,3)") || sql.contains("id IN (1,3,2)") || sql.contains("id IN (2,1,3)") || sql.contains("id IN (2,3,1)") || sql.contains("id IN (3,1,2)") || sql.contains("id IN (3,2,1)")) {
                     return expectedFinalGoods;
                 }
                 return Collections.emptyList();
            });


            List<Goods> actualGoods = goodsService.searchGoods(TEST_KEYWORD);

            assertEquals(expectedFinalIds.size(), actualGoods.size());
            assertTrue(actualGoods.stream().map(Goods::getId).collect(Collectors.toSet()).containsAll(expectedFinalIds));
            
            // Verify the final selectList call captures a wrapper with an IN clause for the merged, deduplicated IDs
            LambdaQueryWrapper<Goods> capturedFinalWrapper = finalQueryCaptor.getValue();
            String finalSql = capturedFinalWrapper.getCustomSqlSegment().toLowerCase();
            assertTrue(finalSql.contains("id in"));
            // Check if all expected IDs are in the IN clause. This is a simplified check.
            assertTrue(expectedFinalIds.stream().allMatch(id -> finalSql.contains(id.toString())));
        }
    }

    @Test
    void searchGoods_findsNothingInEitherSource_shouldReturnEmptyList() {
         try (MockedStatic<AnnotationUtils> mockedAnnotationUtils = Mockito.mockStatic(AnnotationUtils.class);
             MockedStatic<LocaleContextHolder> mockedLocaleHolder = Mockito.mockStatic(LocaleContextHolder.class)) {

            mockedLocaleHolder.when(LocaleContextHolder::getLocale).thenReturn(Locale.forLanguageTag("en_US"));
            mockedAnnotationUtils.when(() -> AnnotationUtils.findAnnotation(eq(Goods.class), eq(I18nResource.class)))
                                 .thenReturn(goodsI18nResource);

            when(i18nMessageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(goodsDao.selectList(argThat(w -> w.getCustomSqlSegment()!=null && (w.getCustomSqlSegment().contains("name LIKE")||w.getCustomSqlSegment().contains("description LIKE")) )))
                .thenReturn(Collections.emptyList());

            List<Goods> actualGoods = goodsService.searchGoods(TEST_KEYWORD);

            assertTrue(actualGoods.isEmpty());
            // Verify that the final goodsDao.selectList (with IN clause) is NOT called if matchedGoodsIds is empty
            verify(goodsDao, never()).selectList(argThat(w -> w.getCustomSqlSegment()!=null && w.getCustomSqlSegment().contains("id IN")));
        }
    }
}
