package com.mushan.tucangbackend.utils;

import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public final class ImageContentValidator {

    private static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024;

    private ImageContentValidator() {
    }

    public static void validateFile(File file, String suffix) {
        try (InputStream input = new FileInputStream(file)) {
            validate(input, suffix);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法读取图片文件");
        }
    }

    public static void validate(InputStream input, String suffix) {
        byte[] bytes = readLimited(input);
        if (bytes.length == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片文件为空");
        }
        String normalizedSuffix = suffix == null ? "" : suffix.toLowerCase(Locale.ROOT);
        boolean jpeg = hasJpegHeader(bytes);
        boolean png = hasPngHeader(bytes);
        boolean webp = hasWebpHeader(bytes);
        if (!jpeg && !png && !webp) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件内容不是受支持的图片");
        }
        if (("jpg".equals(normalizedSuffix) || "jpeg".equals(normalizedSuffix)) && !jpeg
                || "png".equals(normalizedSuffix) && !png
                || "webp".equals(normalizedSuffix) && !webp) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片扩展名与文件内容不一致");
        }
        if (!webp) {
            try {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片文件无法解码");
                }
            } catch (IOException exception) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片文件无法解码");
            }
        }
    }

    private static byte[] readLimited(InputStream input) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_IMAGE_BYTES) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片大小不能超过 2MB");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无法读取图片内容");
        }
    }

    private static boolean hasJpegHeader(byte[] bytes) {
        return bytes.length >= 4
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[bytes.length - 2] & 0xFF) == 0xFF
                && (bytes[bytes.length - 1] & 0xFF) == 0xD9;
    }

    private static boolean hasPngHeader(byte[] bytes) {
        int[] header = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (bytes.length < header.length) {
            return false;
        }
        for (int i = 0; i < header.length; i++) {
            if ((bytes[i] & 0xFF) != header[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasWebpHeader(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }
}
