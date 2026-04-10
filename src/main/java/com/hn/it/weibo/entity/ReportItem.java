package com.hn.it.weibo.entity;

import lombok.Data;

@Data
public class ReportItem {
    private String name;
    private String value;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
}
