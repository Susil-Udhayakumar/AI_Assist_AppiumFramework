package io.framework.observability;

import io.framework.core.exception.FrameworkException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Writes separate log files per concern (appium / test execution / device vitals) in one dir. */
public final class SplitLogWriter {

    public enum Channel {
        APPIUM("appium.log"),
        TEST("test.log"),
        VITALS("vitals.log");

        private final String fileName;

        Channel(String fileName) {
            this.fileName = fileName;
        }
    }

    private final Path dir;

    public SplitLogWriter(Path dir) {
        this.dir = dir;
    }

    public void append(Channel channel, String line) {
        Path file = dir.resolve(channel.fileName);
        try {
            Files.writeString(file, line + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new FrameworkException("Could not write log " + file, e);
        }
    }

    public Path file(Channel channel) {
        return dir.resolve(channel.fileName);
    }
}
