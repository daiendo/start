package com.daiend.muriox.role;

import java.time.OffsetDateTime;

public record RoleResponse(
        Long id,
        String code,
        String name,
        Boolean builtIn,
        Boolean status,
        Integer sortOrder,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}