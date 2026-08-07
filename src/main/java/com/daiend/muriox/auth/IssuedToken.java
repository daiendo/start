package com.daiend.muriox.auth;

public record IssuedToken(
        String token,
        long expiresInSeconds) {
}