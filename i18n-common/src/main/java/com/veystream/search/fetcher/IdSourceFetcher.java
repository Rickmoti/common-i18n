package com.veystream.search.fetcher;

import com.veystream.search.config.EntitySearchConfig;
import java.util.List;
import java.util.Set;

public interface IdSourceFetcher {
    /**
     * Fetches entity IDs from this source based on a keyword query.
     *
     * @param keyword The keyword to search for.
     * @param entityConfig Configuration of the entity being searched.
     * @param entityFieldNamesToSearch Specific entity field names to target with the keyword.
     * @param searchContext Contextual information for the search (e.g., languages).
     * @return A set of matching entity IDs, or an empty set if none found.
     */
    Set<Long> fetchIdsByKeyword(String keyword,
                                EntitySearchConfig entityConfig,
                                List<String> entityFieldNamesToSearch,
                                SearchContext searchContext);

    /**
     * Fetches entity IDs from this source based on an exact match for a specific field value.
     *
     * @param valueToMatch The exact value to match.
     * @param entityConfig Configuration of the entity being searched.
     * @param entityFieldName The specific entity field name to match against.
     * @param searchContext Contextual information for the search.
     * @return A set of matching entity IDs, or an empty set if none found.
     */
    Set<Long> fetchIdsByExactMatch(String valueToMatch,
                                   EntitySearchConfig entityConfig,
                                   String entityFieldName,
                                   SearchContext searchContext);

    /**
     * Returns a string identifier for the type of this fetcher.
     * Useful for debugging or potentially for conditional logic if some fetchers
     * should only be applied to certain types of entities or fields.
     * Example: "db_i18n_messages", "db_default_language_goods", "elasticsearch_products"
     * @return A string representing the source type.
     */
    String getSourceType();
}
