package com.daiend.muriox.organize;

import java.time.OffsetDateTime;
import java.util.List;

public record OrganizeTreeItem(
        Long id,
        String name,
        Long parentId,
        Boolean status,
        Integer sortOrder,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<OrganizeTreeItem> children

) {
}
