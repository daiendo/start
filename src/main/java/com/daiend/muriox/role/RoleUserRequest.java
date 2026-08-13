package com.daiend.muriox.role;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record RoleUserRequest(
        @NotNull(message = "角色 ID 不能为空")
        @Positive(message = "角色 ID 必须大于 0")
        Long roleId,

        @NotNull(message = "用户 ID 列表不能为空")
        List<
                        @NotNull(message = "用户 ID 不能为空")
                        @Positive(message = "用户 ID 必须大于 0")
                                Long> userIdList
) {
}
