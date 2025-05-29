package com.veystream.dto;

import com.veystream.dto.annotation.ExactMatchField;
import com.veystream.dto.annotation.KeywordSearch;
import com.veystream.dto.annotation.MultilingualField;

public class GoodsSearchCriteriaDTO {

    @KeywordSearch(entityFieldNames = {"name", "description"})
    private String generalKeyword;

    @MultilingualField(entityFieldName = "name")
    private String specificName;

    @ExactMatchField(entityFieldName = "category_id")
    private Long categoryId;

    @ExactMatchField(entityFieldName = "status")
    private Integer status;

    private int page = 0;
    private int size = 10;

    // Getters and Setters
    public String getGeneralKeyword() {
        return generalKeyword;
    }

    public void setGeneralKeyword(String generalKeyword) {
        this.generalKeyword = generalKeyword;
    }

    public String getSpecificName() {
        return specificName;
    }

    public void setSpecificName(String specificName) {
        this.specificName = specificName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
