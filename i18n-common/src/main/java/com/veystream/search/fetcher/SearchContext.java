package com.veystream.search.fetcher;

public class SearchContext {
    private final String currentLanguage;
    private final String defaultLanguage;
    private final String i18nDbQueryType; // For querying the 'type' field in i18n_message table

    public SearchContext(String currentLanguage, String defaultLanguage, String i18nDbQueryType) {
        this.currentLanguage = currentLanguage;
        this.defaultLanguage = defaultLanguage;
        this.i18nDbQueryType = i18nDbQueryType;
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public String getI18nDbQueryType() {
        return i18nDbQueryType;
    }
}
