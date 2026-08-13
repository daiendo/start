package com.daiend.muriox.menu;

import jakarta.validation.constraints.*;

public record MenuUpdateRequest(
        @NotNull(message = "菜单 ID 不能为空")
        @Positive(message = "菜单 ID 必须大于 0")
        Long id,

        @Positive(message = "上级菜单 ID 必须大于 0")
        Long parentId,

        @NotNull(message = "菜单类型不能为空")
        @Min(value = 1, message = "菜单类型不正确")
        @Max(value = 2, message = "菜单类型不正确")
        Integer menuType,

        @NotBlank(message = "路由名称不能为空")
        @Size(max = 64, message = "路由名称不能超过64个字符")
        String name,

        @NotBlank(message = "菜单标题不能为空")
        @Size(max = 100, message = "菜单标题不能超过100个字符")
        String title,

        @NotBlank(message = "路由地址不能为空")
        @Size(max = 255, message = "路由地址不能超过255个字符")
        @Pattern(regexp = "^/.*", message = "路由地址必须以 / 开头")
        String path,

        @Size(max = 255, message = "组件路径不能超过255个字符")
        String component,

        @Size(max = 255, message = "重定向地址不能超过255个字符")
        String redirect,

        @Size(max = 100, message = "图标名称不能超过100个字符")
        String icon,

        @NotNull(message = "隐藏状态不能为空")
        Boolean hidden,

        @NotNull(message = "菜单状态不能为空")
        Boolean status,

        @NotNull(message = "排序不能为空")
        @PositiveOrZero(message = "排序不能小于0")
        @Max(value = 999999, message = "排序不能大于999999")
        Integer sortOrder,

        @Size(max = 255, message = "描述不能超过255个字符")
        String description,

        @Size(max = 500, message = "备注不能超过500个字符")
        String remark
) {
}
