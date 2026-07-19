package com.mushan.tucangbackend.utils;

import com.mushan.tucangbackend.model.color.ColorAnalysisResult;
import com.mushan.tucangbackend.model.color.ColorPaletteItem;
import com.mushan.tucangbackend.model.color.PictureColorScores;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 使用 CIELAB 和 CIEDE2000 提取图片调色板并计算产品标准色分数。
 */
public final class ColorPaletteUtils {

    public static final int ALGORITHM_VERSION = 1;

    private static final int SAMPLE_EDGE = 128;
    private static final int PALETTE_SIZE = 5;
    private static final int MAX_KMEANS_ITERATIONS = 20;
    private static final int MIN_ALPHA = 32;
    private static final double TAG_SCORE_THRESHOLD = 0.32D;
    private static final double SCORE_SIGMA = 22D;

    private static final List<CanonicalColor> CANONICAL_COLORS = Collections.unmodifiableList(Arrays.asList(
            new CanonicalColor("red", "#D94B4B"),
            new CanonicalColor("orange", "#E88738"),
            new CanonicalColor("yellow", "#D9B83F"),
            new CanonicalColor("green", "#4D9867"),
            new CanonicalColor("cyan", "#3D9997"),
            new CanonicalColor("blue", "#4D72CF"),
            new CanonicalColor("purple", "#8069C7"),
            new CanonicalColor("pink", "#D85F8D"),
            new CanonicalColor("black", "#242524"),
            new CanonicalColor("white", "#F2F2EF")
    ));

    private ColorPaletteUtils() {
    }

    public static ColorAnalysisResult analyze(File imageFile) throws IOException {
        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            throw new IOException("无法解析图片像素");
        }
        return analyze(image);
    }

    public static ColorAnalysisResult analyze(BufferedImage image) {
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            throw new IllegalArgumentException("图片不能为空");
        }
        List<double[]> samples = sampleLabPixels(image);
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("图片没有可分析的不透明像素");
        }
        List<ColorPaletteItem> palette = createPalette(samples);
        return buildAnalysisResult(palette);
    }

    /**
     * Java ImageIO 不支持某些格式时，使用对象存储返回的平均色提供可搜索的降级结果。
     */
    public static ColorAnalysisResult fromAverageColor(String averageColor) {
        String normalizedHex = normalizeHex(averageColor);
        double[] lab = hexToLab(normalizedHex);
        ColorPaletteItem item = new ColorPaletteItem(normalizedHex, lab[0], lab[1], lab[2], 1D);
        return buildAnalysisResult(Collections.singletonList(item));
    }

    /**
     * 将任意合法 HEX 颜色映射到产品十种标准色之一。
     */
    public static String resolveCanonicalKey(String color) {
        double[] requestedLab = hexToLab(normalizeHex(color));
        CanonicalColor nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (CanonicalColor canonicalColor : CANONICAL_COLORS) {
            double distance = deltaE2000(requestedLab, canonicalColor.lab);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = canonicalColor;
            }
        }
        return nearest == null ? null : nearest.key;
    }

    public static String normalizeHex(String color) {
        if (color == null) {
            throw new IllegalArgumentException("颜色不能为空");
        }
        String normalized = color.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        } else if (normalized.regionMatches(true, 0, "0x", 0, 2)) {
            normalized = normalized.substring(2);
        }
        if (!normalized.matches("^[0-9a-fA-F]{6}$")) {
            throw new IllegalArgumentException("颜色格式必须为 #RRGGBB、0xRRGGBB 或 RRGGBB");
        }
        return "#" + normalized.toUpperCase(Locale.ROOT);
    }

    public static double[] hexToLab(String color) {
        String normalized = normalizeHex(color).substring(1);
        int rgb = Integer.parseInt(normalized, 16);
        return rgbToLab((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255);
    }

    public static double deltaE2000(double[] first, double[] second) {
        double l1 = first[0];
        double a1 = first[1];
        double b1 = first[2];
        double l2 = second[0];
        double a2 = second[1];
        double b2 = second[2];

        double c1 = Math.hypot(a1, b1);
        double c2 = Math.hypot(a2, b2);
        double meanC = (c1 + c2) / 2D;
        double meanC7 = Math.pow(meanC, 7);
        double g = 0.5D * (1D - Math.sqrt(meanC7 / (meanC7 + Math.pow(25D, 7))));
        double adjustedA1 = (1D + g) * a1;
        double adjustedA2 = (1D + g) * a2;
        double adjustedC1 = Math.hypot(adjustedA1, b1);
        double adjustedC2 = Math.hypot(adjustedA2, b2);
        double hue1 = hueDegrees(adjustedA1, b1);
        double hue2 = hueDegrees(adjustedA2, b2);

        double deltaL = l2 - l1;
        double deltaC = adjustedC2 - adjustedC1;
        double deltaHueDegrees;
        if (adjustedC1 * adjustedC2 == 0D) {
            deltaHueDegrees = 0D;
        } else if (Math.abs(hue2 - hue1) <= 180D) {
            deltaHueDegrees = hue2 - hue1;
        } else if (hue2 <= hue1) {
            deltaHueDegrees = hue2 - hue1 + 360D;
        } else {
            deltaHueDegrees = hue2 - hue1 - 360D;
        }
        double deltaH = 2D * Math.sqrt(adjustedC1 * adjustedC2)
                * Math.sin(Math.toRadians(deltaHueDegrees / 2D));

        double meanL = (l1 + l2) / 2D;
        double meanAdjustedC = (adjustedC1 + adjustedC2) / 2D;
        double meanHue;
        if (adjustedC1 * adjustedC2 == 0D) {
            meanHue = hue1 + hue2;
        } else if (Math.abs(hue1 - hue2) <= 180D) {
            meanHue = (hue1 + hue2) / 2D;
        } else if (hue1 + hue2 < 360D) {
            meanHue = (hue1 + hue2 + 360D) / 2D;
        } else {
            meanHue = (hue1 + hue2 - 360D) / 2D;
        }

        double t = 1D
                - 0.17D * Math.cos(Math.toRadians(meanHue - 30D))
                + 0.24D * Math.cos(Math.toRadians(2D * meanHue))
                + 0.32D * Math.cos(Math.toRadians(3D * meanHue + 6D))
                - 0.20D * Math.cos(Math.toRadians(4D * meanHue - 63D));
        double deltaTheta = 30D * Math.exp(-Math.pow((meanHue - 275D) / 25D, 2));
        double meanAdjustedC7 = Math.pow(meanAdjustedC, 7);
        double rc = 2D * Math.sqrt(meanAdjustedC7 / (meanAdjustedC7 + Math.pow(25D, 7)));
        double sl = 1D + (0.015D * Math.pow(meanL - 50D, 2))
                / Math.sqrt(20D + Math.pow(meanL - 50D, 2));
        double sc = 1D + 0.045D * meanAdjustedC;
        double sh = 1D + 0.015D * meanAdjustedC * t;
        double rt = -Math.sin(Math.toRadians(2D * deltaTheta)) * rc;

        double normalizedL = deltaL / sl;
        double normalizedC = deltaC / sc;
        double normalizedH = deltaH / sh;
        return Math.sqrt(normalizedL * normalizedL
                + normalizedC * normalizedC
                + normalizedH * normalizedH
                + rt * normalizedC * normalizedH);
    }

    private static ColorAnalysisResult buildAnalysisResult(List<ColorPaletteItem> palette) {
        PictureColorScores scores = new PictureColorScores();
        List<String> tags = new ArrayList<>();
        String bestKey = null;
        double bestScore = -1D;
        for (CanonicalColor canonicalColor : CANONICAL_COLORS) {
            double score = calculateCanonicalScore(palette, canonicalColor.lab);
            scores.setScore(canonicalColor.key, round(score, 4));
            if (score >= TAG_SCORE_THRESHOLD) {
                tags.add(canonicalColor.key);
            }
            if (score > bestScore) {
                bestScore = score;
                bestKey = canonicalColor.key;
            }
        }
        // 每张可分析图片至少属于一个最接近的标准色，避免颜色过滤完全不可达。
        if (tags.isEmpty() && bestKey != null) {
            tags.add(bestKey);
        }
        return new ColorAnalysisResult(
                palette.get(0).getHex(),
                palette,
                tags,
                scores,
                ALGORITHM_VERSION
        );
    }

    private static double calculateCanonicalScore(List<ColorPaletteItem> palette, double[] targetLab) {
        double bestScore = 0D;
        for (ColorPaletteItem item : palette) {
            double[] itemLab = {item.getL(), item.getA(), item.getB()};
            double distance = deltaE2000(itemLab, targetLab);
            double perceptualSimilarity = Math.exp(-0.5D * Math.pow(distance / SCORE_SIGMA, 2));
            double coverage = Math.min(1D, item.getWeight() / 0.20D);
            double score = perceptualSimilarity * (0.65D + 0.35D * coverage);
            bestScore = Math.max(bestScore, score);
        }
        return Math.max(0D, Math.min(1D, bestScore));
    }

    private static List<double[]> sampleLabPixels(BufferedImage image) {
        int sampleWidth = Math.min(SAMPLE_EDGE, image.getWidth());
        int sampleHeight = Math.min(SAMPLE_EDGE, image.getHeight());
        List<double[]> samples = new ArrayList<>(sampleWidth * sampleHeight);
        for (int y = 0; y < sampleHeight; y++) {
            int sourceY = Math.min(image.getHeight() - 1,
                    (int) (((y + 0.5D) * image.getHeight()) / sampleHeight));
            for (int x = 0; x < sampleWidth; x++) {
                int sourceX = Math.min(image.getWidth() - 1,
                        (int) (((x + 0.5D) * image.getWidth()) / sampleWidth));
                int argb = image.getRGB(sourceX, sourceY);
                int alpha = (argb >>> 24) & 255;
                if (alpha < MIN_ALPHA) {
                    continue;
                }
                samples.add(rgbToLab((argb >> 16) & 255, (argb >> 8) & 255, argb & 255));
            }
        }
        return samples;
    }

    private static List<ColorPaletteItem> createPalette(List<double[]> samples) {
        int clusterCount = Math.min(PALETTE_SIZE, samples.size());
        double[][] centers = initializeCenters(samples, clusterCount);
        int[] assignments = new int[samples.size()];
        Arrays.fill(assignments, -1);

        for (int iteration = 0; iteration < MAX_KMEANS_ITERATIONS; iteration++) {
            boolean changed = false;
            double[][] sums = new double[clusterCount][3];
            int[] counts = new int[clusterCount];
            for (int i = 0; i < samples.size(); i++) {
                double[] sample = samples.get(i);
                int nearest = nearestCenter(sample, centers);
                if (assignments[i] != nearest) {
                    assignments[i] = nearest;
                    changed = true;
                }
                counts[nearest]++;
                sums[nearest][0] += sample[0];
                sums[nearest][1] += sample[1];
                sums[nearest][2] += sample[2];
            }
            for (int i = 0; i < clusterCount; i++) {
                if (counts[i] > 0) {
                    centers[i][0] = sums[i][0] / counts[i];
                    centers[i][1] = sums[i][1] / counts[i];
                    centers[i][2] = sums[i][2] / counts[i];
                }
            }
            if (!changed) {
                break;
            }
        }

        int[] counts = new int[clusterCount];
        for (int assignment : assignments) {
            counts[assignment]++;
        }
        List<ColorPaletteItem> palette = new ArrayList<>(clusterCount);
        for (int i = 0; i < clusterCount; i++) {
            if (counts[i] == 0) {
                continue;
            }
            double weight = counts[i] * 1D / samples.size();
            palette.add(new ColorPaletteItem(
                    labToHex(centers[i]),
                    round(centers[i][0], 4),
                    round(centers[i][1], 4),
                    round(centers[i][2], 4),
                    round(weight, 4)
            ));
        }
        palette.sort(Comparator.comparingDouble(ColorPaletteItem::getWeight).reversed());
        return palette;
    }

    private static double[][] initializeCenters(List<double[]> samples, int clusterCount) {
        double[] mean = new double[3];
        for (double[] sample : samples) {
            mean[0] += sample[0];
            mean[1] += sample[1];
            mean[2] += sample[2];
        }
        mean[0] /= samples.size();
        mean[1] /= samples.size();
        mean[2] /= samples.size();

        double[][] centers = new double[clusterCount][3];
        centers[0] = samples.get(nearestSample(samples, mean)).clone();
        for (int centerIndex = 1; centerIndex < clusterCount; centerIndex++) {
            int farthestIndex = 0;
            double farthestDistance = -1D;
            for (int sampleIndex = 0; sampleIndex < samples.size(); sampleIndex++) {
                double nearestDistance = Double.MAX_VALUE;
                for (int i = 0; i < centerIndex; i++) {
                    nearestDistance = Math.min(nearestDistance,
                            squaredDistance(samples.get(sampleIndex), centers[i]));
                }
                if (nearestDistance > farthestDistance) {
                    farthestDistance = nearestDistance;
                    farthestIndex = sampleIndex;
                }
            }
            centers[centerIndex] = samples.get(farthestIndex).clone();
        }
        return centers;
    }

    private static int nearestSample(List<double[]> samples, double[] target) {
        int nearest = 0;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < samples.size(); i++) {
            double distance = squaredDistance(samples.get(i), target);
            if (distance < nearestDistance) {
                nearest = i;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static int nearestCenter(double[] sample, double[][] centers) {
        int nearest = 0;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < centers.length; i++) {
            double distance = squaredDistance(sample, centers[i]);
            if (distance < nearestDistance) {
                nearest = i;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static double squaredDistance(double[] first, double[] second) {
        double deltaL = first[0] - second[0];
        double deltaA = first[1] - second[1];
        double deltaB = first[2] - second[2];
        return deltaL * deltaL + deltaA * deltaA + deltaB * deltaB;
    }

    private static double[] rgbToLab(int red, int green, int blue) {
        double r = pivotRgb(red / 255D);
        double g = pivotRgb(green / 255D);
        double b = pivotRgb(blue / 255D);
        double x = (r * 0.4124564D + g * 0.3575761D + b * 0.1804375D) / 0.95047D;
        double y = r * 0.2126729D + g * 0.7151522D + b * 0.0721750D;
        double z = (r * 0.0193339D + g * 0.1191920D + b * 0.9503041D) / 1.08883D;
        double fx = pivotXyz(x);
        double fy = pivotXyz(y);
        double fz = pivotXyz(z);
        return new double[]{116D * fy - 16D, 500D * (fx - fy), 200D * (fy - fz)};
    }

    private static String labToHex(double[] lab) {
        double fy = (lab[0] + 16D) / 116D;
        double fx = lab[1] / 500D + fy;
        double fz = fy - lab[2] / 200D;
        double x = 0.95047D * inversePivotXyz(fx);
        double y = inversePivotXyz(fy);
        double z = 1.08883D * inversePivotXyz(fz);
        double linearR = x * 3.2404542D + y * -1.5371385D + z * -0.4985314D;
        double linearG = x * -0.9692660D + y * 1.8760108D + z * 0.0415560D;
        double linearB = x * 0.0556434D + y * -0.2040259D + z * 1.0572252D;
        int red = clampRgb(inversePivotRgb(linearR) * 255D);
        int green = clampRgb(inversePivotRgb(linearG) * 255D);
        int blue = clampRgb(inversePivotRgb(linearB) * 255D);
        return String.format(Locale.ROOT, "#%02X%02X%02X", red, green, blue);
    }

    private static double pivotRgb(double value) {
        return value <= 0.04045D ? value / 12.92D : Math.pow((value + 0.055D) / 1.055D, 2.4D);
    }

    private static double inversePivotRgb(double value) {
        return value <= 0.0031308D ? 12.92D * value : 1.055D * Math.pow(value, 1D / 2.4D) - 0.055D;
    }

    private static double pivotXyz(double value) {
        double delta = 6D / 29D;
        return value > delta * delta * delta
                ? Math.cbrt(value)
                : value / (3D * delta * delta) + 4D / 29D;
    }

    private static double inversePivotXyz(double value) {
        double delta = 6D / 29D;
        return value > delta
                ? value * value * value
                : 3D * delta * delta * (value - 4D / 29D);
    }

    private static int clampRgb(double value) {
        return (int) Math.round(Math.max(0D, Math.min(255D, value)));
    }

    private static double hueDegrees(double a, double b) {
        if (a == 0D && b == 0D) {
            return 0D;
        }
        double hue = Math.toDegrees(Math.atan2(b, a));
        return hue < 0D ? hue + 360D : hue;
    }

    private static double round(double value, int scale) {
        double factor = Math.pow(10D, scale);
        return Math.round(value * factor) / factor;
    }

    private static final class CanonicalColor {
        private final String key;
        private final double[] lab;

        private CanonicalColor(String key, String hex) {
            this.key = key;
            this.lab = hexToLab(hex);
        }
    }
}
