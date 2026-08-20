package com.daiend.muriox.user;

import com.daiend.muriox.common.exception.BusinessException;
import com.daiend.muriox.storage.ObjectStorageService;
import com.daiend.muriox.storage.StorageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;

@Service
public class AvatarService {

    private static final Logger LOG =
            LoggerFactory.getLogger(AvatarService.class);

    private static final long MAX_FILE_SIZE =
            2L * 1024 * 1024;

    private static final int MIN_IMAGE_DIMENSION = 128;
    private static final int MAX_IMAGE_DIMENSION = 4096;

    private static final DateTimeFormatter PATH_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy/MM");

    private final ObjectStorageService objectStorageService;

    public AvatarService(
            ObjectStorageService objectStorageService) {
        this.objectStorageService = objectStorageService;
    }

    public AvatarResponse upload(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException("头像文件不能为空");
        }

        if (multipartFile.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("头像大小不能超过2MB");
        }

        try {
            ImageType imageType =
                    detectImageType(multipartFile);

            ImageDimensions dimensions =
                    readImageDimensions(
                            multipartFile,
                            imageType);

            validateDimensions(dimensions);

            String objectKey =
                    createObjectKey(imageType.extension());

            StorageResult storageResult;

            try (InputStream inputStream =
                         multipartFile.getInputStream()) {
                storageResult = objectStorageService.upload(
                        objectKey,
                        inputStream,
                        multipartFile.getSize(),
                        imageType.contentType());
            }

            return new AvatarResponse(
                    storageResult.objectKey(),
                    storageResult.presignedUrl());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            LOG.error("读取头像文件失败", exception);
            throw new BusinessException("头像文件无法解析");
        }
    }

    private ImageType detectImageType(
            MultipartFile multipartFile) throws Exception {
        byte[] header;

        try (InputStream inputStream =
                     multipartFile.getInputStream()) {
            header = inputStream.readNBytes(30);
        }

        if (isJpeg(header)) {
            return new ImageType("image/jpeg", "jpg");
        }

        if (isPng(header)) {
            return new ImageType("image/png", "png");
        }

        if (isWebp(header)) {
            return new ImageType("image/webp", "webp");
        }

        throw new BusinessException(
                "头像必须为JPG、PNG或WebP格式");
    }

    private ImageDimensions readImageDimensions(
            MultipartFile multipartFile,
            ImageType imageType) throws Exception {
        if ("image/webp".equals(imageType.contentType())) {
            return readWebpDimensions(multipartFile);
        }

        try (InputStream inputStream =
                     multipartFile.getInputStream();
             ImageInputStream imageInputStream =
                     ImageIO.createImageInputStream(inputStream)) {

            if (imageInputStream == null) {
                throw new BusinessException("头像文件无法解析");
            }

            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(imageInputStream);

            if (!readers.hasNext()) {
                throw new BusinessException("头像文件无法解析");
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(imageInputStream, true, true);

                return new ImageDimensions(
                        reader.getWidth(0),
                        reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        }
    }

    private ImageDimensions readWebpDimensions(
            MultipartFile multipartFile) throws Exception {
        byte[] header;

        try (InputStream inputStream =
                     multipartFile.getInputStream()) {
            header = inputStream.readNBytes(30);
        }

        if (header.length < 25) {
            throw new BusinessException("WebP头像文件无法解析");
        }

        String chunkType =
                new String(
                        header,
                        12,
                        4,
                        StandardCharsets.US_ASCII);

        return switch (chunkType) {
            case "VP8X" -> readExtendedWebpDimensions(header);
            case "VP8L" -> readLosslessWebpDimensions(header);
            case "VP8 " -> readLossyWebpDimensions(header);
            default -> throw new BusinessException(
                    "WebP头像文件无法解析");
        };
    }

    private ImageDimensions readExtendedWebpDimensions(
            byte[] header) {
        if (header.length < 30) {
            throw new BusinessException("WebP头像文件无法解析");
        }

        int width = 1 + readLittleEndian24(header, 24);
        int height = 1 + readLittleEndian24(header, 27);

        return new ImageDimensions(width, height);
    }

    private ImageDimensions readLosslessWebpDimensions(
            byte[] header) {
        if (header.length < 25
                || (header[20] & 0xff) != 0x2f) {
            throw new BusinessException("WebP头像文件无法解析");
        }

        int first = header[21] & 0xff;
        int second = header[22] & 0xff;
        int third = header[23] & 0xff;
        int fourth = header[24] & 0xff;

        int width =
                1 + first + ((second & 0x3f) << 8);

        int height =
                1
                        + (second >> 6)
                        + (third << 2)
                        + ((fourth & 0x0f) << 10);

        return new ImageDimensions(width, height);
    }

    private ImageDimensions readLossyWebpDimensions(
            byte[] header) {
        if (header.length < 30
                || (header[23] & 0xff) != 0x9d
                || (header[24] & 0xff) != 0x01
                || (header[25] & 0xff) != 0x2a) {
            throw new BusinessException("WebP头像文件无法解析");
        }

        int width =
                readLittleEndian16(header, 26) & 0x3fff;

        int height =
                readLittleEndian16(header, 28) & 0x3fff;

        return new ImageDimensions(width, height);
    }

    private void validateDimensions(
            ImageDimensions dimensions) {
        if (dimensions.width() < MIN_IMAGE_DIMENSION
                || dimensions.height() < MIN_IMAGE_DIMENSION) {
            throw new BusinessException(
                    "头像尺寸不能小于128×128");
        }

        if (dimensions.width() > MAX_IMAGE_DIMENSION
                || dimensions.height() > MAX_IMAGE_DIMENSION) {
            throw new BusinessException(
                    "头像尺寸不能超过4096×4096");
        }
    }

    private String createObjectKey(String extension) {
        String datePath =
                LocalDate.now(ZoneOffset.UTC)
                        .format(PATH_DATE_FORMAT);

        return "avatar/"
                + datePath
                + "/"
                + UUID.randomUUID()
                + "."
                + extension;
    }

    private boolean isJpeg(byte[] header) {
        return header.length >= 3
                && (header[0] & 0xff) == 0xff
                && (header[1] & 0xff) == 0xd8
                && (header[2] & 0xff) == 0xff;
    }

    private boolean isPng(byte[] header) {
        byte[] signature = {
                (byte) 0x89,
                0x50,
                0x4e,
                0x47,
                0x0d,
                0x0a,
                0x1a,
                0x0a
        };

        return header.length >= signature.length
                && Arrays.equals(
                Arrays.copyOf(header, signature.length),
                signature);
    }

    private boolean isWebp(byte[] header) {
        return header.length >= 12
                && matchesAscii(header, 0, "RIFF")
                && matchesAscii(header, 8, "WEBP");
    }

    private boolean matchesAscii(
            byte[] bytes,
            int offset,
            String expected) {
        if (bytes.length < offset + expected.length()) {
            return false;
        }

        for (int index = 0;
             index < expected.length();
             index++) {
            if (bytes[offset + index]
                    != (byte) expected.charAt(index)) {
                return false;
            }
        }

        return true;
    }

    private int readLittleEndian16(
            byte[] bytes,
            int offset) {
        return (bytes[offset] & 0xff)
                | ((bytes[offset + 1] & 0xff) << 8);
    }

    private int readLittleEndian24(
            byte[] bytes,
            int offset) {
        return (bytes[offset] & 0xff)
                | ((bytes[offset + 1] & 0xff) << 8)
                | ((bytes[offset + 2] & 0xff) << 16);
    }

    private record ImageType(
            String contentType,
            String extension) {
    }

    private record ImageDimensions(
            int width,
            int height) {
    }
}