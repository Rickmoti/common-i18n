package com.veystream.search.dto;

public class ExactQueryCondition {
    String fieldName; 
    Object value;

    public ExactQueryCondition(String fieldName, Object value) { 
        this.fieldName = fieldName; 
        this.value = value; 
    }

    public String getFieldName() { 
        return fieldName; 
    }

    public Object getValue() { 
        return value; 
    }

    @Override 
    public String toString() { 
        return "ExactQueryCondition{fieldName='" + fieldName + "', value=" + value + '}';
    }
}
