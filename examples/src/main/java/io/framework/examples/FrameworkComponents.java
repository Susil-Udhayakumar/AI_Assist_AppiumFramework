package io.framework.examples;

import io.framework.actions.ElementActions;
import io.framework.core.events.EventBus;
import io.framework.core.failure.FailureClassifier;
import io.framework.core.lifecycle.DriverLifecycle;
import io.framework.core.parallel.DevicePool;
import io.framework.locators.SmartFinder;
import io.framework.observability.CaptureLayout;
import io.framework.observability.ScreenshotProvider;
import io.framework.observability.Screenshotter;
import io.framework.reporting.Reporters;

/** The wired framework singletons produced by {@link Bootstrap}. */
public record FrameworkComponents(
        EventBus eventBus,
        DriverLifecycle lifecycle,
        DevicePool devicePool,
        SmartFinder finder,
        ElementActions elementActions,
        FailureClassifier failureClassifier,
        Reporters reporters,
        CaptureLayout captureLayout,
        Screenshotter screenshotter,
        ScreenshotProvider screenshotProvider) {
}
