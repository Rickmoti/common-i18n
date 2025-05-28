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
import org.springframework.core.annotation.AnnotationUtils; // Static mock for this

import java.util.ArrayList;
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

    @InjectMocks
    private GoodsService goodsService;

    private Goods testGoods;
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
}
