package com.veystream.search.config;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
// import java.util.Optional; // Optional import, not used in the provided code.

@Service
public class SearchConfigProvider {

    private final Map<Class<?>, EntitySearchConfig> configMap = new HashMap<>();

    // Constructor injection for all EntitySearchConfig beans
    public SearchConfigProvider(List<EntitySearchConfig> configs) {
        if (configs != null) {
            for (EntitySearchConfig config : configs) {
                if (config.getEntityClass() != null) {
                    this.configMap.put(config.getEntityClass(), config);
                }
            }
        }
    }

    public EntitySearchConfig getConfig(Class<?> entityClass) {
        return configMap.get(entityClass);
    }

    // Optional: A method that throws an exception if config is not found
    public EntitySearchConfig getRequiredConfig(Class<?> entityClass) {
        EntitySearchConfig config = configMap.get(entityClass);
        if (config == null) {
            throw new IllegalArgumentException("No EntitySearchConfig found for class: " + entityClass.getName());
        }
        return config;
    }
}
