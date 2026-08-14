package com.daiend.muriox.organize;

import jakarta.validation.constraints.*;

public record OrganizeRequest(
        @NotBlank(message = "组织名称不能为空")
        @Size(max = 64, message = "组织名称不能超过64个字符")
        String name,

        @PositiveOrZero(message = "上级组织ID不能小于0")
        Long parentId,

        @NotNull(message = "组织状态不能为空")
        Boolean status,

        @NotNull(message = "排序不能为空")
        @PositiveOrZero(message = "排序不能小于0")
        @Max(value = 999999, message = "排序不能大于999999")
        Integer sortOrder,

        @Size(max = 500, message = "备注不能超过500个字符")
        String remark
) {
}
