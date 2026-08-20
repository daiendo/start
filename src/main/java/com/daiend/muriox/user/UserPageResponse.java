package com.daiend.muriox.user;

import java.time.OffsetDateTime;

public record UserPageResponse(
        Long id,
        Long orgId,
        String orgName,
        String account,
        String username,

        // MinIO 对象 Key
        String avatar,

        // 只用于前端展示的预签名地址
        String avatarUrl,

        String sex,
        Boolean enabled,
        String postNames,
        Boolean builtIn,
        String remark,
        OffsetDateTime createdAt
) {
}
