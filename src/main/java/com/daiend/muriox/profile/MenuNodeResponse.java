package com.daiend.muriox.profile;

import java.util.List;

public record MenuNodeResponse(Long id,
                               String name,
                               String path,
                               String component,
                               List<MenuNodeResponse> children) {
}
