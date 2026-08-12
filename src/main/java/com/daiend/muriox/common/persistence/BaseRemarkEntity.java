package com.daiend.muriox.common.persistence;

public abstract  class BaseRemarkEntity extends BaseEntity {
    private String remark;

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
