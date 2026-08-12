package com.daiend.muriox.dict;

public class DictOptionRow {
    private Long id;
    private String label;
    private String value;
    private DictValueType valueType;
    private String tagType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public DictValueType getValueType() {
        return valueType;
    }

    public void setValueType(DictValueType valueType) {
        this.valueType = valueType;
    }

    public String getTagType() {
        return tagType;
    }

    public void setTagType(String tagType) {
        this.tagType = tagType;
    }
}
