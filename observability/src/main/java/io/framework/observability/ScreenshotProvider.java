package io.framework.observability;

import org.openqa.selenium.TakesScreenshot;

import java.util.Optional;

/**
 * Supplies the current screenshot source (the active driver), or empty when none is available
 * (e.g. before a session starts). Injected into {@link CaptureListener} so it stays testable.
 */
@FunctionalInterface
public interface ScreenshotProvider {
    Optional<TakesScreenshot> current();
}
