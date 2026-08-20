package com.daiend.muriox.storage;

public record StorageResult(
        String objectKey,
        String presignedUrl) {
}