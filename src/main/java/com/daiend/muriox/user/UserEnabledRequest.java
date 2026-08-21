package com.daiend.muriox.user;

import jakarta.validation.constraints.NotNull;

public record UserEnabledRequest(
        @NotNull(message = "用户状态不能为空")
        Boolean enabled
) {
}