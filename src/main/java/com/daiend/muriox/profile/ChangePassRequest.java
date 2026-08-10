package com.daiend.muriox.profile;

import jakarta.validation.constraints.NotBlank;

public record ChangePassRequest (
      @NotBlank(message = "旧密码不能为空") String oldPassword,
      @NotBlank(message = "新密码不能为空")  String newPassword,
      @NotBlank(message = "确认密码不能为空")  String confirmPassword
) {
}
