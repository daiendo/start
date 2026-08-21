package com.daiend.muriox.user;

import java.time.OffsetDateTime;

public record UserDetailRow(
        Long id,
        String account,
        String username,
        Long orgId,
        String orgName,
        String avatar,
        String sex,
        String email,
        String mobile,
        Boolean enabled,
        Boolean builtIn,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}