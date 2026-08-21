package com.daiend.muriox.role;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record RoleDataScopeRequest(
        @NotNull(message = "角色 ID 不能为空")
        @Positive(message = "角色 ID 必须大于 0")
        Long roleId,

        @NotNull(message = "数据范围类型不能为空")
        DataScopeType dataScopeType,

        @NotNull(message = "自定义组织 ID 列表不能为空")
        List<
                @NotNull(message = "组织 ID 不能为空")
                @Positive(message = "组织 ID 必须大于 0")
                        Long> orgIdList
) {
}