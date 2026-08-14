package com.daiend.muriox.post;

import jakarta.validation.constraints.*;

public record PostUpdateRequest(

        @NotNull(message = "岗位 ID 不能为空")
        @Positive(message = "岗位 ID 必须大于 0")
        Long id,

        @NotNull(message = "所属组织不能为空")
        @Positive(message = "组织 ID 必须大于 0")
        Long orgId,

        @NotBlank(message = "岗位编码不能为空")
        @Size(max = 64, message = "岗位编码不能超过 64 个字符")
        @Pattern(
                regexp = "^[a-z][a-z0-9_-]*$",
                message = "岗位编码只能包含小写字母、数字、下划线和短横线，且必须以字母开头")
        String code,

        @NotBlank(message = "岗位名称不能为空")
        @Size(max = 100, message = "岗位名称不能超过 100 个字符")
        String name,

        @NotNull(message = "岗位状态不能为空")
        Boolean status,

        @NotNull(message = "排序不能为空")
        @PositiveOrZero(message = "排序不能小于 0")
        @Max(value = 999999, message = "排序不能大于 999999")
        Integer sortOrder,

        @Size(max = 500, message = "备注不能超过 500 个字符")
        String remark
) {
}
