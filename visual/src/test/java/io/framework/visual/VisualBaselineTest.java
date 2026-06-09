package io.framework.visual;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class VisualBaselineTest {

    private BufferedImage solid(int rgb) {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                img.setRGB(x, y, rgb);
            }
        }
        return img;
    }

    @Test
    void firstSightCreatesBaseline(@TempDir Path dir) {
        var outcome = new VisualBaseline(dir).compareOrCreate("home", solid(0xFFFFFFFF), 0.0);

        assertThat(outcome.status()).isEqualTo(VisualOutcome.Status.NEW_BASELINE);
        assertThat(Files.exists(dir.resolve("home.png"))).isTrue();
    }

    @Test
    void matchingActualPasses(@TempDir Path dir) {
        var baseline = new VisualBaseline(dir);
        baseline.compareOrCreate("home", solid(0xFFFFFFFF), 0.0);   // create

        var outcome = baseline.compareOrCreate("home", solid(0xFFFFFFFF), 0.0);
        assertThat(outcome.status()).isEqualTo(VisualOutcome.Status.MATCH);
        assertThat(outcome.isFailure()).isFalse();
    }

    @Test
    void differingActualWritesDiffArtifacts(@TempDir Path dir) {
        var baseline = new VisualBaseline(dir);
        baseline.compareOrCreate("home", solid(0xFFFFFFFF), 0.0);   // create white baseline

        var outcome = baseline.compareOrCreate("home", solid(0xFF000000), 0.0);  // all black
        assertThat(outcome.status()).isEqualTo(VisualOutcome.Status.DIFF);
        assertThat(outcome.isFailure()).isTrue();
        assertThat(Files.exists(dir.resolve("home.actual.png"))).isTrue();
        assertThat(Files.exists(dir.resolve("home.diff.png"))).isTrue();
    }
}
