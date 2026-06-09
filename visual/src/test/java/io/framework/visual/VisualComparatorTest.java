package io.framework.visual;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

class VisualComparatorTest {

    private final VisualComparator comparator = new VisualComparator();

    private BufferedImage solid(int w, int h, int rgb) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, rgb);
            }
        }
        return img;
    }

    @Test
    void identicalImagesMatch() {
        var a = solid(10, 10, 0xFFFFFFFF);
        var b = solid(10, 10, 0xFFFFFFFF);
        VisualResult r = comparator.compare(a, b, 0.0);
        assertThat(r.matched()).isTrue();
        assertThat(r.diffPixels()).isZero();
        assertThat(r.diffRatio()).isZero();
    }

    @Test
    void singlePixelDiffRespectsTolerance() {
        var a = solid(10, 10, 0xFFFFFFFF);   // 100 pixels
        var b = solid(10, 10, 0xFFFFFFFF);
        b.setRGB(0, 0, 0xFF000000);          // 1 differing pixel -> ratio 0.01

        assertThat(comparator.compare(a, b, 0.0).matched()).isFalse();
        VisualResult lenient = comparator.compare(a, b, 0.02);
        assertThat(lenient.matched()).isTrue();
        assertThat(lenient.diffPixels()).isEqualTo(1);
        assertThat(lenient.diffRatio()).isEqualTo(0.01);
    }

    @Test
    void sizeMismatchNeverMatches() {
        VisualResult r = comparator.compare(solid(10, 10, 0xFF), solid(8, 10, 0xFF), 1.0);
        assertThat(r.matched()).isFalse();
        assertThat(r.diffRatio()).isEqualTo(1.0);
        assertThat(r.diffImage()).isEmpty();
    }
}
