package io.framework.devices;

/**
 * Runs an external command and returns its combined stdout/stderr text.
 * Abstracted so device discovery parsing is unit-testable without invoking adb/xcrun.
 */
@FunctionalInterface
public interface CommandRunner {
    String run(String... command);
}
