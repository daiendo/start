package com.daiend.muriox.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("app.authority-cache")
public record AuthorityCacheProperties(
        @NotNull Duration ttl
) {
    public AuthorityCacheProperties {
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException(
                    "权限缓存有效期必须大于0");
        }
    }
}