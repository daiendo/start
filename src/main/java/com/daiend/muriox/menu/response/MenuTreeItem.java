package com.daiend.muriox.menu.response;

import java.time.OffsetDateTime;
import java.util.List;

public record MenuTreeItem(
        Long id,
        Long parentId,
        Integer menuType,
        String name,
        String title,
        String path,
        String component,
        String redirect,
        String icon,
        Boolean hidden,
        Boolean status,
        Integer sortOrder,
        String description,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<MenuTreeItem> children
) {
}
