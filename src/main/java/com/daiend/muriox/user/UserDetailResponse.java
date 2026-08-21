package com.daiend.muriox.user;

import java.time.OffsetDateTime;
import java.util.List;

public record UserDetailResponse(
        Long id,
        String account,
        String username,
        Long orgId,
        String orgName,
        List<Long> postIds,
        String postNames,
        String avatar,
        String avatarUrl,
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