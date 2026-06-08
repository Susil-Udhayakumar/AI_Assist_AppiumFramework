package io.framework.knowledge;

import io.framework.core.exception.FrameworkException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Writes a human-readable Markdown digest per run (referenced for trend + bug classification)
 * plus an appendable index line. Compact on purpose: a glanceable record of history that is
 * cheap for a human or an AI to scan.
 */
public final class ExecutionHistory {

    private final Path dir;

    public ExecutionHistory(Path baseDir) {
        this.dir = baseDir.resolve("learned").resolve("history");
    }

    public Path append(RunDigest d) {
        Path file = dir.resolve(d.date() + "-" + sanitize(d.suite()) + ".md");
        try {
            Files.createDirectories(dir);
            Files.writeString(file, render(d));
            Files.writeString(dir.resolve("index.md"),
                    "- " + d.date() + " " + d.suite() + " | " + d.platform()
                            + " | PASS " + d.passed() + " FAIL " + d.failed()
                            + " FLAKY " + d.flaky() + " | " + d.durationMs() + "ms"
                            + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new FrameworkException("Could not write execution history: " + file, e);
        }
        return file;
    }

    private static String render(RunDigest d) {
        StringBuilder md = new StringBuilder();
        md.append("# Run ").append(d.date()).append(" | ").append(d.platform())
                .append(" | suite:").append(d.suite()).append("\n");
        md.append("PASS ").append(d.passed()).append(" FAIL ").append(d.failed())
                .append(" FLAKY ").append(d.flaky()).append(" | TOTAL ").append(d.total())
                .append(" | ").append(d.durationMs()).append("ms\n");
        if (!d.failureLines().isEmpty()) {
            md.append("## Failures\n");
            for (String line : d.failureLines()) {
                md.append("- ").append(line).append("\n");
            }
        }
        return md.toString();
    }

    private static String sanitize(String s) {
        return s == null ? "_" : s.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
