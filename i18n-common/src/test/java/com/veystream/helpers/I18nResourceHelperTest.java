package com.veystream.helpers;

import com.veystream.annotation.I18nField;
import com.veystream.annotation.I18nResource;
import com.veystream.dto.I18nResourceInfo;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class I18nResourceHelperTest {

    // Helper entities defined as static inner classes

    static class NonI18nEntity_Test {}

    @I18nResource(prefix = "TestWithEmptyIdKey", identityKey = "", i18nFields = {
        @I18nField(fieldName = "name")
    })
    static class InvalidI18nEntityEmptyIdKey_Test {}

    // This class will be used to test the scenario where identityKey might be null,
    // although @I18nResource makes identityKey non-null.
    // The helper's check for null identityKey is more of a defensive programming measure.
    // We cannot directly simulate a null identityKey via annotation if it's marked @NonNull.
    // So, InvalidI18nEntityEmptyIdKey_Test covers the .trim().isEmpty() part.

    @I18nResource(prefix = "TestNoFields", identityKey = "id")
    static class I18nEntityNoFields_Test {}

    @I18nResource(prefix = "TestBlankField", identityKey = "id", i18nFields = {
        @I18nField(fieldName = "  "),
        @I18nField(fieldName = "") // another blank field
    })
    static class I18nEntityAllBlankFieldNames_Test {}

    @I18nResource(
        prefix = "ValidPrefix", 
        identityKey = "entityId", 
        i18nFields = {
            @I18nField(fieldName = "name"),
            @I18nField(fieldName = "description"),
            @I18nField(fieldName = "") // This one should be ignored
        }
    )
    static class FullyAnnotatedEntity_Test {}

    @I18nResource(
        prefix = "", // Empty prefix
        identityKey = "id", 
        i18nFields = { @I18nField(fieldName = "title") }
    )
    static class EntityWithEmptyPrefix_Test {}

    @I18nResource(
        // No prefix defined, should default to class name
        identityKey = "id", 
        i18nFields = { @I18nField(fieldName = "detail") }
    )
    static class EntityWithNoPrefix_Test {}


    // Test Methods

    @Test
    void getResourceInfo_withNullClass_shouldReturnNonInternationalized() {
        I18nResourceInfo info = I18nResourceHelper.getResourceInfo(null);
        assertFalse(info.isInternationalizedResource(), "Info for null class should not be internationalized.");
        assertNull(info.getPrefix(), "Prefix should be null for non-internationalized result from null class.");
        assertNull(info.getIdentityKeyField(), "IdentityKeyField should be null for non-internationalized result from null class.");
        assertTrue(info.getI18nActualFieldNames().isEmpty(), "Field names should be empty for non-internationalized result from null class.");
    }

    @Test
    void getResourceInfo_withNoAnnotation_shouldReturnNonInternationalized() {
        I18nResourceInfo info = I18nResourceHelper.getResourceInfo(NonI18nEntity_Test.class);
        assertFalse(info.isInternationalizedResource(), "Info for class without annotation should not be internationalized.");
    }

    @Test
    void getResourceInfo_withAnnotationButEmptyIdentityKey_shouldReturnNonInternationalized() {
        // This tests the helper's check: i18nResource.identityKey().trim().isEmpty()
        I18nResourceInfo info = I18nResourceHelper.getResourceInfo(InvalidI18nEntityEmptyIdKey_Test.class);
        assertFalse(info.isInternationalizedResource(), "Info for class with empty identityKey should not be internationalized.");
    }
    
    @Test
    void getResourceInfo_withAnnotationButNoFields_shouldReturnNonInternationalized() {
        I18nResourceInfo info = I18nResourceHelper.getResourceInfo(I18nEntityNoFields_Test.class);
        assertFalse(info.isInternationalizedResource(), "Info for class with @I18nResource but no i18nFields should not be internationalized.");
    }

    @Test
    void getResourceInfo_withAnnotationButOnlyBlankFieldNames_shouldReturnNonInternationalized() {
        I18nResourceInfo info = I18nResourceHelper.getResourceInfo(I18nEntityAllBlankFieldNames_Test.class);
        assertFalse(info.isInternationalizedResource(), "Info for class with only blank fieldNames in i18nFields should not be internationalized.");
    }

    @Test
    void getResourceInfo_withFullAnnotation_shouldExtractCorrectInfo() {
        I18nResourceInfo info = I18nResourceHelper.getResourceInfo(FullyAnnotatedEntity_Test.class);
        assertTrue(info.isInternationalizedResource(), "Info for fully annotated class should be internationalized.");
        assertEquals("ValidPrefix", info.getPrefix(), "Prefix should match the annotation.");
        assertEquals("entityId", info.getIdentityKeyField(), "IdentityKeyField should match the annotation.");
        
        List<String> expectedFieldNames = Arrays.asList("name", "description");
        assertEquals(expectedFieldNames, info.getI18nActualFieldNames(), "Field names should match valid ones from annotation, excluding blank ones.");
    }

    @Test
    void getResourceInfo_withEmptyPrefixInAnnotation_shouldDefaultToClassName() {
        I18nResourceInfo info = I18nResourceHelper.getResourceInfo(EntityWithEmptyPrefix_Test.class);
        assertTrue(info.isInternationalizedResource(), "Info for entity with empty prefix should be internationalized.");
        assertEquals(EntityWithEmptyPrefix_Test.class.getName(), info.getPrefix(), "Prefix should default to class name when annotation prefix is empty.");
        assertEquals("id", info.getIdentityKeyField(), "IdentityKeyField should match.");
        assertEquals(Collections.singletonList("title"), info.getI18nActualFieldNames(), "Field names should match.");
    }
    
    @Test
    void getResourceInfo_withNoPrefixInAnnotation_shouldDefaultToClassName() {
        // This test assumes that if 'prefix' element is entirely absent from annotation usage,
        // it behaves same as prefix="". Spring's AnnotationUtils.findAnnotation might return null for prefix()
        // or the annotation's default value if defined (which is "" for @I18nResource).
        // The helper logic `if (prefix == null || prefix.trim().isEmpty())` covers both.
        I18nResourceInfo info = I18nResourceHelper.getResourceInfo(EntityWithNoPrefix_Test.class);
        assertTrue(info.isInternationalizedResource(), "Info for entity with no prefix attribute set should be internationalized.");
        assertEquals(EntityWithNoPrefix_Test.class.getName(), info.getPrefix(), "Prefix should default to class name when prefix attribute is not set.");
        assertEquals("id", info.getIdentityKeyField());
        assertEquals(Collections.singletonList("detail"), info.getI18nActualFieldNames());
    }
}
