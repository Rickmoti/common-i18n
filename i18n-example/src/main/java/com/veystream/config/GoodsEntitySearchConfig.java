package com.veystream.config;

import com.veystream.entity.Goods;
import com.veystream.search.config.EntitySearchConfig;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class GoodsEntitySearchConfig implements EntitySearchConfig {

    @Override
    public Class<?> getEntityClass() {
        return Goods.class;
    }

    @Override
    public String getEntityTableName() {
        return "t_goods";
    }

    @Override
    public String getIdField() {
        return "id";
    }

    @Override
    public List<String> getDefaultLanguageSearchableFields() {
        return Arrays.asList("name", "description");
    }

    @Override
    public boolean isInternationalized() {
        return true;
    }

    @Override
    public String getI18nPrefix() {
        return "Goods";
    }

    @Override
    public List<String> getInternationalizedSearchableFields() {
        return Arrays.asList("name", "description");
    }
}
