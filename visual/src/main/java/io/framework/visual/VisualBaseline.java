package io.framework.visual;

import io.framework.core.exception.FrameworkException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Baseline store + check. On first sight of a name the actual image becomes the baseline
 * (NEW_BASELINE); afterwards the actual is compared to it. On a diff, the actual and a red diff
 * image are written next to the baseline for inspection. Baselines are PNGs under one directory,
 * meant to be committed and reviewed.
 */
public final class VisualBaseline {

    private final Path dir;
    private final VisualComparator comparator = new VisualComparator();

    public VisualBaseline(Path dir) {
        this.dir = dir;
    }

    public VisualOutcome compareOrCreate(String name, BufferedImage actual, double tolerance) {
        Path baselineFile = dir.resolve(sanitize(name) + ".png");
        if (!Files.exists(baselineFile)) {
            write(actual, baselineFile);
            return new VisualOutcome(VisualOutcome.Status.NEW_BASELINE, null);
        }

        BufferedImage baseline = read(baselineFile);
        VisualResult result = comparator.compare(baseline, actual, tolerance);
        if (!result.matched()) {
            write(actual, dir.resolve(sanitize(name) + ".actual.png"));
            result.diffImage().ifPresent(img -> write(img, dir.resolve(sanitize(name) + ".diff.png")));
            return new VisualOutcome(VisualOutcome.Status.DIFF, result);
        }
        return new VisualOutcome(VisualOutcome.Status.MATCH, result);
    }

    private void write(BufferedImage image, Path file) {
        try {
            Files.createDirectories(dir);
            ImageIO.write(image, "png", file.toFile());
        } catch (IOException e) {
            throw new FrameworkException("Could not write image: " + file, e);
        }
    }

    private BufferedImage read(Path file) {
        try {
            return ImageIO.read(file.toFile());
        } catch (IOException e) {
            throw new FrameworkException("Could not read baseline image: " + file, e);
        }
    }

    private static String sanitize(String s) {
        return s == null ? "_" : s.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
