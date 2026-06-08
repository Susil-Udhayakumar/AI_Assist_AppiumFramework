package io.framework.observability;

import io.framework.core.exception.FrameworkException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Computes and creates per-test capture directories: base/runId/device/test. */
public final class CaptureLayout {

    private final Path base;

    public CaptureLayout(Path base) {
        this.base = base;
    }

    public Path testDir(String runId, String device, String test) {
        Path dir = base.resolve(sanitize(runId)).resolve(sanitize(device)).resolve(sanitize(test));
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new FrameworkException("Could not create capture directory: " + dir, e);
        }
        return dir;
    }

    static String sanitize(String s) {
        return s == null ? "_" : s.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
