package com.daiend.muriox.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record UserRoleRequest(
        @NotNull(message = "角色列表不能为空")
        List<
                @NotNull(message = "角色 ID 不能为空")
                @Positive(message = "角色 ID 不合法")
                        Long
                > roleIds
) {
}