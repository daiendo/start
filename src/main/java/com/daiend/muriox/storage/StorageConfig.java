package com.daiend.muriox.storage;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class StorageConfig {

    @Bean
    public MinioClient minioClient(StorageProperties storageProperties) {
        return MinioClient.builder()
                .endpoint(storageProperties.endpoint().toString())
                .credentials(
                        storageProperties.accessKey(),
                        storageProperties.secretKey())
                .build();
    }
}
