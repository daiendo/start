package com.daiend.muriox.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePassRequest(
        @NotBlank(message = "旧密码不能为空")
        @Size(max = 64, message = "旧密码不能超过 64 位")
        String oldPassword,

        @NotBlank(message = "新密码不能为空")
        @Size(
                min = 6,
                max = 64,
                message = "新密码长度必须为 6 至 64 位")
        String newPassword,

        @NotBlank(message = "确认密码不能为空")
        @Size(
                min = 6,
                max = 64,
                message = "确认密码长度必须为 6 至 64 位")
        String confirmPassword
) {
}