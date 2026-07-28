package com.mushan.tucangbackend.utils;

import com.mushan.tucangbackend.exception.BusinessException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageContentValidatorTest {

    @Test
    void acceptsDecodedPngWithMatchingSuffix() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);

        assertDoesNotThrow(() -> ImageContentValidator.validate(
                new ByteArrayInputStream(output.toByteArray()), "png"));
    }

    @Test
    void rejectsTextRenamedAsImage() {
        byte[] content = "not an image".getBytes(StandardCharsets.UTF_8);

        assertThrows(BusinessException.class, () -> ImageContentValidator.validate(
                new ByteArrayInputStream(content), "png"));
    }

    @Test
    void rejectsMismatchedSuffixAndMagicNumber() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);

        assertThrows(BusinessException.class, () -> ImageContentValidator.validate(
                new ByteArrayInputStream(output.toByteArray()), "jpg"));
    }
}
