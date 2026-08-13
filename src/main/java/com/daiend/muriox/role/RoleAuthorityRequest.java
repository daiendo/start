package com.daiend.muriox.role;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record RoleAuthorityRequest(
        @NotNull(message = "角色 ID 不能为空")
        @Positive(message = "角色 ID 必须大于 0")
        Long roleId,

        @NotNull(message = "菜单 ID 列表不能为空")
        List<
                @NotNull(message = "菜单 ID 不能为空")
                @Positive(message = "菜单 ID 必须大于 0")
                        Long> menuIdList,

        @NotNull(message = "按钮权限 ID 列表不能为空")
        List<
                @NotNull(message = "按钮权限 ID 不能为空")
                @Positive(message = "按钮权限 ID 必须大于 0")
                        Long> resourceIdList
) {
}
