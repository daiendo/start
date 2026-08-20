package com.daiend.muriox.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties("app.storage")
public record StorageProperties(
        @NotNull URI endpoint,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @NotBlank String bucket,
        @NotNull Duration presignedUrlTtl) {

    public StorageProperties {
        if (endpoint != null
                && !"http".equalsIgnoreCase(endpoint.getScheme())
                && !"https".equalsIgnoreCase(endpoint.getScheme())) {
            throw new IllegalArgumentException(
                    "对象存储服务地址必须使用HTTP或HTTPS协议");
        }

        if (presignedUrlTtl != null
                && (presignedUrlTtl.isZero()
                || presignedUrlTtl.isNegative()
                || presignedUrlTtl.compareTo(Duration.ofDays(7)) > 0)) {
            throw new IllegalArgumentException(
                    "对象存储预签名地址有效期必须大于0且不能超过7天");
        }
    }
}
