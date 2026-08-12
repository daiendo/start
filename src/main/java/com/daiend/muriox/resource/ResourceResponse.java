package com.daiend.muriox.resource;

import java.time.OffsetDateTime;

public record  ResourceResponse (
        Long id,
        Long menuId,
        String name,
        String code,
        Boolean status,
        Integer sortOrder,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
