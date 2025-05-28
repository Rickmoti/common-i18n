package com.veystream.dto;

import com.veystream.entity.Goods;
import java.util.Map;

public class GoodsUpdatePayload {
    private Goods goods;
    private Map<String, Map<String, String>> translations;

    // Getters and Setters
    public Goods getGoods() {
        return goods;
    }

    public void setGoods(Goods goods) {
        this.goods = goods;
    }

    public Map<String, Map<String, String>> getTranslations() {
        return translations;
    }

    public void setTranslations(Map<String, Map<String, String>> translations) {
        this.translations = translations;
    }
}
