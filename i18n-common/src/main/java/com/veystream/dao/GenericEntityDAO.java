package com.veystream.dao;

import com.veystream.search.dto.ExactQueryCondition;
import com.veystream.search.dto.PageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mapper
public interface GenericEntityDAO {

    /**
     * Fetches entities based on a list of IDs and a list of exact match conditions.
     *
     * @param tableName The name of the table to query.
     * @param idColumnName The name of the ID column in the table.
     * @param ids Set of IDs to filter by. If null, no ID-based filtering. 
     *            If empty, and it's the only substantive filter, should result in no entities.
     * @param conditions List of exact match conditions (field_name, value).
     * @param pageQuery Pagination information (page number, page size).
     * @return A list of maps, where each map represents an entity.
     *         Using List<Map<String, Object>> as a generic return type for now.
     *         Specific entity mapping would require more complex setup for a truly generic DAO.
     */
    List<Map<String, Object>> findEntitiesWithFiltersAndIds(
            @Param("tableName") String tableName,
            @Param("idColumnName") String idColumnName,
            @Param("ids") Set<Long> ids,
            @Param("conditions") List<ExactQueryCondition> conditions,
            @Param("pageQuery") PageQuery pageQuery
    );
}
