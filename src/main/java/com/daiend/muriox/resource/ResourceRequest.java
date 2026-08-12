package com.daiend.muriox.resource;

import jakarta.validation.constraints.*;

public record ResourceRequest(
        @NotNull(message = "所属菜单不能为空")
        @Positive(message = "所属菜单ID必须大于0")
        Long menuId,

        @NotBlank(message = "按钮名称不能为空")
        @Size(max = 64, message = "按钮名称不能超过64个字符")
        String name,

        @NotBlank(message = "权限编码不能为空")
        @Size(max = 128, message = "权限编码不能超过128个字符")
        @Pattern(
                regexp = "^[a-z][a-z0-9_-]*(?::[a-z][a-z0-9_-]*)+$",
                message = "权限编码格式不正确")
        String code,

        @NotNull(message = "资源状态不能为空")
        Boolean status,

        @NotNull(message = "排序不能为空")
        @PositiveOrZero(message = "排序不能小于0")
        @Max(value = 999999, message = "排序不能大于999999")
        Integer sortOrder,

        @Size(max = 500, message = "备注不能超过500个字符")
        String remark

) {
}
