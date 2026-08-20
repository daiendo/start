package com.daiend.muriox.storage;

import java.io.InputStream;

public interface ObjectStorageService {

    StorageResult upload(
            String objectKey,
            InputStream inputStream,
            long size,
            String contentType);

    String createPresignedGetUrl(String objectKey);

    void delete(String objectKey);
}