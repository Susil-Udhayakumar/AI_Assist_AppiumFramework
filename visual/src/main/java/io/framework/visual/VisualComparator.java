package io.framework.visual;

import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * Pure pixel comparison. Two images match when the fraction of differing pixels is within
 * tolerance. A size mismatch is an automatic non-match. Produces a diff image with differing
 * pixels painted red over a faded copy of the actual image.
 */
public final class VisualComparator {

    private static final int RED = 0xFFFF0000;

    public VisualResult compare(BufferedImage baseline, BufferedImage actual, double tolerance) {
        if (baseline.getWidth() != actual.getWidth() || baseline.getHeight() != actual.getHeight()) {
            return new VisualResult(false, 1.0, -1, -1, Optional.empty());
        }
        int width = baseline.getWidth();
        int height = baseline.getHeight();
        int total = width * height;
        int diff = 0;

        BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int a = baseline.getRGB(x, y);
                int b = actual.getRGB(x, y);
                if (a != b) {
                    diff++;
                    diffImage.setRGB(x, y, RED);
                } else {
                    diffImage.setRGB(x, y, fade(b));
                }
            }
        }

        double ratio = total == 0 ? 0.0 : (double) diff / total;
        boolean matched = ratio <= tolerance;
        return new VisualResult(matched, ratio, diff, total, Optional.of(diffImage));
    }

    /** Halve the alpha so unchanged areas read as a faint backdrop behind red diffs. */
    private static int fade(int argb) {
        int alpha = ((argb >>> 24) & 0xFF) / 2;
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
