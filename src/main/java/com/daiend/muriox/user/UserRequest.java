package com.daiend.muriox.user;

import jakarta.validation.constraints.*;

import java.util.List;

public record UserRequest(
        @NotBlank(message = "登录账号不能为空")
        @Size(max = 64, message = "登录账号不能超过 64 个字符")
        String account,

        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名不能超过 64 个字符")
        String username,

        @NotNull(message = "所属组织不能为空")
        @Positive(message = "所属组织 ID 不合法")
        Long orgId,

        @NotNull(message = "岗位列表不能为空")
        @Size(max = 50, message = "岗位数量不能超过 50 个")
        List<
                @NotNull(message = "岗位 ID 不能为空")
                @Positive(message = "岗位 ID 不合法")
                        Long
                > postIds,

        @Size(max = 500, message = "头像对象 Key 不能超过 500 个字符")
        String avatar,

        @Pattern(
                regexp = "male|female|unknown",
                message = "性别值不合法"
        )
        String sex,

        @Email(message = "邮箱格式不正确")
        @Size(max = 254, message = "邮箱不能超过 254 个字符")
        String email,

        @Size(max = 32, message = "手机号不能超过 32 个字符")
        String mobile,

        @Size(max = 500, message = "备注不能超过 500 个字符")
        String remark
) {
}