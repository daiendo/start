package com.daiend.muriox.resource;

import jakarta.validation.constraints.*;

public record ResourceUpdateRequest (
        @NotNull(message = "按钮权限 ID 不能为空")
        @Positive(message = "按钮权限 ID 必须大于 0")
        Long id,

        @NotNull(message = "所属菜单不能为空")
        @Positive(message = "所属菜单 ID 必须大于 0")
        Long menuId,

        @NotBlank(message = "按钮名称不能为空")
        @Size(max = 64, message = "按钮名称不能超过 64 个字符")
        String name,

        @NotBlank(message = "权限编码不能为空")
        @Size(max = 128, message = "权限编码不能超过 128 个字符")
        @Pattern(
                regexp = "^[a-z][a-z0-9_-]*(?::[a-z][a-z0-9_-]*)+$",
                message = "权限编码格式不正确")
        String code,

        @NotNull(message = "资源状态不能为空")
        Boolean status,

        @NotNull(message = "排序不能为空")
        @PositiveOrZero(message = "排序不能小于 0")
        @Max(value = 999999, message = "排序不能大于 999999")
        Integer sortOrder,

        @Size(max = 500, message = "备注不能超过 500 个字符")
        String remark
){
}
