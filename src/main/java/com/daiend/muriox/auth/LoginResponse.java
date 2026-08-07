package com.daiend.muriox.auth;

public record LoginResponse(
        String token,
        Long expiresInSeconds) {
}
