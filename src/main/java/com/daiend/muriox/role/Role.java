package com.daiend.muriox.role;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.daiend.muriox.common.persistence.BaseRemarkEntity;

@TableName("sys_role")
public class Role extends BaseRemarkEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private Boolean builtIn;
    private DataScopeType dataScopeType = DataScopeType.CURRENT_ORG;
    private Boolean status;
    private Integer sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getBuiltIn() {
        return builtIn;
    }

    public void setBuiltIn(Boolean builtIn) {
        this.builtIn = builtIn;
    }

    public DataScopeType getDataScopeType() {
        return dataScopeType;
    }

    public void setDataScopeType(DataScopeType dataScopeType) {
        this.dataScopeType = dataScopeType;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
