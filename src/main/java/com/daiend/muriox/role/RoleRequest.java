package com.daiend.muriox.role;

import jakarta.validation.constraints.*;

public record RoleRequest(
        @NotBlank(message = "角色名称不能为空")
        @Size(max = 64, message = "角色名称不能超过 64 个字符")
        String name,

        @NotBlank(message = "角色编码不能为空")
        @Size(max = 64, message = "角色编码不能超过 64 个字符")
        @Pattern(
                regexp = "^[a-z][a-z0-9_-]*$",
                message = "角色编码只能包含小写字母、数字、下划线和短横线，且必须以字母开头")
        String code,

        @NotNull(message = "角色状态不能为空")
        Boolean status,

        @NotNull(message = "排序不能为空")
        @PositiveOrZero(message = "排序不能小于 0")
        @Max(value = 999999, message = "排序不能大于 999999")
        Integer sortOrder,

        @Size(max = 500, message = "备注不能超过 500 个字符")
        String remark
) {
}