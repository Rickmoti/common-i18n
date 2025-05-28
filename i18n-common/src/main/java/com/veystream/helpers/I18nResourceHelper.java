package com.veystream.helpers;

import com.veystream.annotation.I18nField;
import com.veystream.annotation.I18nResource;
import com.veystream.dto.I18nResourceInfo;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class I18nResourceHelper {

    /**
     * Retrieves internationalization resource information from a class.
     *
     * @param clazz The class to inspect for @I18nResource annotation.
     * @return I18nResourceInfo containing the extracted data, or a non-internationalized representation
     *         (where isInternationalizedResource() is false) if the annotation is not present
     *         or if essential parts of the annotation are missing.
     */
    public static I18nResourceInfo getResourceInfo(Class<?> clazz) {
        if (clazz == null) {
            return I18nResourceInfo.notAnI18nResource();
        }

        I18nResource i18nResource = AnnotationUtils.findAnnotation(clazz, I18nResource.class);

        if (i18nResource == null || i18nResource.identityKey() == null || i18nResource.identityKey().trim().isEmpty()) {
            // If no annotation or no identityKey, it's not a valid I18N resource for our purposes.
            return I18nResourceInfo.notAnI18nResource();
        }

        String prefix = i18nResource.prefix();
        if (prefix == null || prefix.trim().isEmpty()) {
            // Default prefix to class name if not specified, as per original annotation intent
            prefix = clazz.getName(); 
        }

        String identityKeyField = i18nResource.identityKey();
        List<String> i18nActualFieldNames = new ArrayList<>();
        I18nField[] i18nFields = i18nResource.i18nFields();

        if (i18nFields != null) {
            for (I18nField i18nField : i18nFields) {
                if (i18nField.fieldName() != null && !i18nField.fieldName().trim().isEmpty()) {
                    i18nActualFieldNames.add(i18nField.fieldName());
                }
            }
        }
        
        // It's only a valid resource if there are fields to internationalize.
        if (i18nActualFieldNames.isEmpty()) {
            return I18nResourceInfo.notAnI18nResource();
        }

        return new I18nResourceInfo(prefix, identityKeyField, i18nActualFieldNames);
    }
}
