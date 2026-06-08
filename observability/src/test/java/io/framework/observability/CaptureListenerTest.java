package io.framework.observability;

import io.framework.core.config.Capture;
import io.framework.core.events.EventContext;
import io.framework.core.events.TestEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CaptureListenerTest {

    private Capture capture(Capture.When screenshots) {
        return new Capture(screenshots, Capture.When.OFF, false, false);
    }

    @Test
    void shootsAndLogsOnActionWhenSourcePresent(@TempDir Path dir) throws Exception {
        TakesScreenshot source = mock(TakesScreenshot.class);
        when(source.getScreenshotAs(OutputType.BYTES)).thenReturn("png".getBytes(StandardCharsets.UTF_8));

        var listener = new CaptureListener(capture(Capture.When.ON_ACTION), dir,
                new Screenshotter(), () -> Optional.of(source), new SplitLogWriter(dir));

        listener.on(new EventContext(TestEvent.AFTER_ACTION, Map.of("name", "tap")));

        long pngs = Files.list(dir).filter(p -> p.toString().endsWith(".png")).count();
        assertThat(pngs).isEqualTo(1);
        assertThat(Files.readString(dir.resolve("test.log"))).contains("AFTER_ACTION");
    }

    @Test
    void logsButDoesNotShootWhenNoSource(@TempDir Path dir) throws Exception {
        var listener = new CaptureListener(capture(Capture.When.ON_FAILURE), dir,
                new Screenshotter(), Optional::empty, new SplitLogWriter(dir));

        listener.on(new EventContext(TestEvent.TEST_FAIL, Map.of()));

        long pngs = Files.list(dir).filter(p -> p.toString().endsWith(".png")).count();
        assertThat(pngs).isZero();
        assertThat(Files.readString(dir.resolve("test.log"))).contains("TEST_FAIL");
    }

    @Test
    void doesNotShootWhenPolicySaysNo(@TempDir Path dir) throws Exception {
        TakesScreenshot source = mock(TakesScreenshot.class);
        var listener = new CaptureListener(capture(Capture.When.ON_FAILURE), dir,
                new Screenshotter(), () -> Optional.of(source), new SplitLogWriter(dir));

        listener.on(new EventContext(TestEvent.AFTER_ACTION, Map.of()));

        long pngs = Files.list(dir).filter(p -> p.toString().endsWith(".png")).count();
        assertThat(pngs).isZero();
    }
}
