package com.veystream.dao;

import com.veystream.search.config.EntitySearchConfig;
// org.apache.ibatis.annotations.Mapper is not needed as per instruction
// org.apache.ibatis.annotations.Param is not needed here as this is an interface,
// implementations will use it if they are MyBatis mappers.

import java.util.List;
import java.util.Set;

public interface EntityDefaultLanguageSearchDAO {

    /**
     * Finds entity IDs by keyword search in specified fields of the entity's main table.
     *
     * @param entityConfig The configuration of the entity being searched, providing table/column names.
     * @param fieldNames List of physical field/column names in the main table to search.
     * @param keyword The keyword to search for (implementations should handle LIKE logic).
     * @return A set of matching entity IDs.
     */
    Set<Long> findIdsByKeywordInDefaultLanguage(EntitySearchConfig entityConfig,
                                                List<String> fieldNames,
                                                String keyword);

    /**
     * Finds entity IDs by an exact match on a specified field in the entity's main table.
     *
     * @param entityConfig The configuration of the entity being searched.
     * @param fieldName The physical field/column name in the main table to match.
     * @param value The exact value to match.
     * @return A set of matching entity IDs.
     */
    Set<Long> findIdsByExactMatchInDefaultLanguage(EntitySearchConfig entityConfig,
                                                   String fieldName,
                                                   String value);
}
