package com.daiend.muriox.storage;

import com.daiend.muriox.common.exception.BusinessException;
import io.minio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Objects;

@Service
public class MinioObjectStorageService implements ObjectStorageService {

    private static final Logger LOG =
            LoggerFactory.getLogger(MinioObjectStorageService.class);

    private final MinioClient minioClient;
    private final StorageProperties storageProperties;

    public MinioObjectStorageService(
            MinioClient minioClient,
            StorageProperties storageProperties) {
        this.minioClient = minioClient;
        this.storageProperties = storageProperties;
    }

    @Override
    public StorageResult upload(
            String objectKey,
            InputStream inputStream,
            long size,
            String contentType) {
        validateObjectKey(objectKey);

        if (Objects.isNull(inputStream)) {
            throw new BusinessException("上传文件内容不能为空");
        }

        if (size <= 0) {
            throw new BusinessException("上传文件不能为空");
        }

        String resolvedContentType =
                contentType == null || contentType.isBlank()
                        ? "application/octet-stream"
                        : contentType;
        String presignedUrl = createPresignedGetUrl(objectKey);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(storageProperties.bucket())
                            .object(objectKey)
                            .stream(inputStream, size, -1L)
                            .contentType(resolvedContentType)
                            .build());

            return new StorageResult(
                    objectKey,
                    presignedUrl);
        } catch (Exception exception) {
            LOG.error(
                    "上传对象存储失败，objectKey={}",
                    objectKey,
                    exception);
            throw new BusinessException("文件上传失败");
        }
    }

    @Override
    public String createPresignedGetUrl(String objectKey) {
        validateObjectKey(objectKey);

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(storageProperties.bucket())
                            .object(objectKey)
                            .expiry(Math.toIntExact(
                                    storageProperties
                                            .presignedUrlTtl()
                                            .toSeconds()))
                            .build());
        } catch (Exception exception) {
            LOG.error(
                    "生成对象访问地址失败，objectKey={}",
                    objectKey,
                    exception);
            throw new BusinessException("生成文件访问地址失败");
        }
    }

    @Override
    public void delete(String objectKey) {
        validateObjectKey(objectKey);

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(storageProperties.bucket())
                            .object(objectKey)
                            .build());
        } catch (Exception exception) {
            LOG.error(
                    "删除对象存储文件失败，objectKey={}",
                    objectKey,
                    exception);
            throw new BusinessException("删除文件失败");
        }
    }

    private void validateObjectKey(String objectKey) {
        if (objectKey == null
                || objectKey.isBlank()
                || objectKey.startsWith("/")
                || objectKey.contains("../")) {
            throw new BusinessException("文件对象标识不合法");
        }
    }
}
