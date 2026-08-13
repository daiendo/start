package com.daiend.muriox.user;

public record UserListResponse(
        Long id,
        String account,
        String username,
        Boolean enabled
) {
}
