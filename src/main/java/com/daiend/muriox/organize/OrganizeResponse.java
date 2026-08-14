package com.daiend.muriox.organize;

import java.time.OffsetDateTime;

public record OrganizeResponse(
        Long id,
        String name,
        Long parentId,
        Boolean status,
        Integer sortOrder,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
