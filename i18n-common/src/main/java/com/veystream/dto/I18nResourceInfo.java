package com.veystream.dto;

import java.util.List;
import java.util.Collections;

public class I18nResourceInfo {

    private final String prefix;
    private final String identityKeyField; // Field name in the entity that acts as the ID
    private final List<String> i18nActualFieldNames; // List of actual entity field names to be internationalized

    public I18nResourceInfo(String prefix, String identityKeyField, List<String> i18nActualFieldNames) {
        this.prefix = prefix;
        this.identityKeyField = identityKeyField;
        this.i18nActualFieldNames = i18nActualFieldNames != null ? Collections.unmodifiableList(i18nActualFieldNames) : Collections.emptyList();
    }

    public String getPrefix() {
        return prefix;
    }

    public String getIdentityKeyField() {
        return identityKeyField;
    }

    public List<String> getI18nActualFieldNames() {
        return i18nActualFieldNames;
    }

    // Optional: A static factory or a representation for "not an I18N resource"
    public static I18nResourceInfo notAnI18nResource() {
        return new I18nResourceInfo(null, null, Collections.emptyList());
    }

    public boolean isInternationalizedResource() {
        return this.prefix != null && this.identityKeyField != null && !this.i18nActualFieldNames.isEmpty();
    }
}
