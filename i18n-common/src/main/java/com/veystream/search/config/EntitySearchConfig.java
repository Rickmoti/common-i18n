package com.veystream.search.config;

import java.util.List;

public interface EntitySearchConfig {
    Class<?> getEntityClass();
    String getEntityTableName();
    String getIdField(); // e.g., "id"
    // Fields in the main entity table used for default language fuzzy search
    List<String> getDefaultLanguageSearchableFields(); 
    boolean isInternationalized();
    String getI18nPrefix(); // Null or empty if not internationalized
    // Entity field names that are internationalized and searchable (e.g., "name", "description")
    List<String> getInternationalizedSearchableFields(); 
}
