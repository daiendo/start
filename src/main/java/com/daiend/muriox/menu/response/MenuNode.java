package com.daiend.muriox.menu.response;

import java.util.List;

public record MenuNode(
        Long id,
        String name,
        String path,
        String component,
        String redirect,
        MenuMeta meta,
        List<MenuNode> children
) {
}
