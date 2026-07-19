package com.mushan.tucangbackend.utils;

import com.mushan.tucangbackend.model.color.ColorAnalysisResult;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorPaletteUtilsTest {

    @Test
    void shouldNormalizeSupportedHexFormats() {
        assertEquals("#D94B4B", ColorPaletteUtils.normalizeHex("#d94b4b"));
        assertEquals("#D94B4B", ColorPaletteUtils.normalizeHex("0xD94B4B"));
        assertEquals("#D94B4B", ColorPaletteUtils.normalizeHex("D94B4B"));
    }

    @Test
    void shouldMatchPublishedCiede2000ReferencePair() {
        double distance = ColorPaletteUtils.deltaE2000(
                new double[]{50.0000, 2.6772, -79.7751},
                new double[]{50.0000, 0.0000, -82.7485}
        );
        assertEquals(2.0425, distance, 0.0001);
    }

    @Test
    void shouldScoreCanonicalSolidColor() {
        BufferedImage image = solidImage(new Color(0xD9, 0x4B, 0x4B));

        ColorAnalysisResult result = ColorPaletteUtils.analyze(image);

        assertEquals("#D94B4B", result.getDominantColor());
        assertTrue(result.getColorTags().contains("red"));
        assertTrue(result.getColorScores().getRed() > 0.99D);
    }

    @Test
    void shouldRetainMultipleSignificantColors() {
        BufferedImage image = new BufferedImage(80, 40, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, x < 40 ? 0xFFD94B4B : 0xFF4D72CF);
            }
        }

        ColorAnalysisResult result = ColorPaletteUtils.analyze(image);

        assertTrue(result.getColorTags().contains("red"));
        assertTrue(result.getColorTags().contains("blue"));
        assertTrue(result.getPalette().size() >= 2);
    }

    private BufferedImage solidImage(Color color) {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, color.getRGB());
            }
        }
        return image;
    }
}
