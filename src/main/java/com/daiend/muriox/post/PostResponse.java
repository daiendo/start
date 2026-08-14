package com.daiend.muriox.post;

import java.time.OffsetDateTime;

public record PostResponse(
        Long id,
        Long orgId,
        String orgName,
        String code,
        String name,
        Integer sortOrder,
        Boolean status,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
