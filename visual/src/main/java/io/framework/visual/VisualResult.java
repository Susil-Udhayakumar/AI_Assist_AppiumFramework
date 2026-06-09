package io.framework.visual;

import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * Outcome of a pixel comparison. diffRatio is the fraction of differing pixels (0..1); on a size
 * mismatch it is 1.0 and pixel counts are -1. diffImage highlights the differing pixels in red.
 */
public record VisualResult(boolean matched, double diffRatio, int diffPixels, int totalPixels,
                           Optional<BufferedImage> diffImage) {
}
