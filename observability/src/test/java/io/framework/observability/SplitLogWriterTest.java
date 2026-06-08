package io.framework.observability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SplitLogWriterTest {

    @Test
    void writesSeparateFilesPerChannel(@TempDir Path dir) throws Exception {
        var logs = new SplitLogWriter(dir);
        logs.append(SplitLogWriter.Channel.TEST, "test line");
        logs.append(SplitLogWriter.Channel.APPIUM, "appium line");
        logs.append(SplitLogWriter.Channel.TEST, "second test line");

        assertThat(Files.readString(logs.file(SplitLogWriter.Channel.TEST)))
                .contains("test line").contains("second test line");
        assertThat(Files.readString(logs.file(SplitLogWriter.Channel.APPIUM)))
                .contains("appium line").doesNotContain("test line");
    }
}
