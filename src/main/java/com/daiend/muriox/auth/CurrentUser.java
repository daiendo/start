package com.daiend.muriox.auth;

public record CurrentUser(
        Long id,
        String sessionId
      ) {
}