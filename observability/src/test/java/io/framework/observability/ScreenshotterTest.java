package io.framework.observability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScreenshotterTest {

    @Test
    void writesPngWithExtension(@TempDir Path dir) throws Exception {
        TakesScreenshot source = mock(TakesScreenshot.class);
        when(source.getScreenshotAs(OutputType.BYTES)).thenReturn("png-bytes".getBytes(StandardCharsets.UTF_8));

        Path file = new Screenshotter().capture(source, dir, "1-AFTER_ACTION");

        assertThat(file.getFileName().toString()).isEqualTo("1-AFTER_ACTION.png");
        assertThat(Files.readString(file)).isEqualTo("png-bytes");
    }
}
