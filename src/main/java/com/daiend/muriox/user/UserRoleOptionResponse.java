package com.daiend.muriox.user;

public record UserRoleOptionResponse(
        Long id,
        String code,
        String name,
        Boolean status,
        Boolean builtIn
) {
}