package io.framework.observability;

import io.framework.core.config.Capture;
import io.framework.core.events.EventContext;
import io.framework.core.events.EventListener;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Subscribes to core's EventBus and reacts: logs every event to the test channel and takes a
 * screenshot when {@link CapturePolicy} says so for the current capture setting. The screenshot
 * source is injected via {@link ScreenshotProvider}, and the target directory is the test's
 * pre-resolved capture dir. Adding a new capture type = a new listener, no core change.
 */
public final class CaptureListener implements EventListener {

    private final Capture capture;
    private final Path dir;
    private final Screenshotter screenshotter;
    private final ScreenshotProvider provider;
    private final SplitLogWriter logs;
    private final AtomicInteger sequence = new AtomicInteger();

    public CaptureListener(Capture capture, Path dir, Screenshotter screenshotter,
                           ScreenshotProvider provider, SplitLogWriter logs) {
        this.capture = capture;
        this.dir = dir;
        this.screenshotter = screenshotter;
        this.provider = provider;
        this.logs = logs;
    }

    @Override
    public void on(EventContext ctx) {
        logs.append(SplitLogWriter.Channel.TEST, ctx.event() + " " + ctx.payload());

        if (CapturePolicy.shouldScreenshot(capture.screenshots(), ctx.event())) {
            provider.current().ifPresent(source ->
                    screenshotter.capture(source, dir,
                            sequence.incrementAndGet() + "-" + ctx.event()));
        }
    }
}
