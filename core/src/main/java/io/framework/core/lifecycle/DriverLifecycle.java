package io.framework.core.lifecycle;

import io.framework.core.config.Platform;
import io.framework.core.context.ContextManager;
import io.framework.core.context.DriverContext;
import io.framework.core.driver.DriverProvider;
import io.framework.core.events.EventBus;
import io.framework.core.events.TestEvent;
import io.framework.core.parallel.DeviceLease;
import io.framework.core.parallel.DevicePool;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates one test's driver lifecycle:
 *   start: acquire device -> create driver -> set ThreadLocal context -> emit TEST_START
 *   stop:  emit TEST_FAIL/TEST_PASS -> emit TEST_END -> quit driver -> release device -> clear context
 * Ordering matters: failure event fires before the driver is torn down so listeners
 * (screenshot/video/source) can still capture evidence.
 */
public final class DriverLifecycle {

    private final DevicePool pool;
    private final DriverProvider driverProvider;
    private final EventBus bus;

    public DriverLifecycle(DevicePool pool, DriverProvider driverProvider, EventBus bus) {
        this.pool = pool;
        this.driverProvider = driverProvider;
        this.bus = bus;
    }

    public void start(Platform platform, Capabilities caps, long acquireTimeout, TimeUnit unit) {
        DeviceLease lease = pool.acquire(acquireTimeout, unit);
        WebDriver driver = driverProvider.create(platform, caps);
        DriverContext ctx = DriverContext.builder().driver(driver).device(lease).build();
        ContextManager.set(ctx);
        bus.emit(TestEvent.TEST_START, Map.of("device", lease.deviceId()));
    }

    public void stop(boolean failed) {
        DriverContext ctx = ContextManager.current();
        try {
            bus.emit(failed ? TestEvent.TEST_FAIL : TestEvent.TEST_PASS,
                    Map.of("device", ctx.device().deviceId()));
            bus.emit(TestEvent.TEST_END, Map.of("device", ctx.device().deviceId()));
        } finally {
            try {
                ctx.driver().quit();
            } finally {
                pool.release(ctx.device());
                ContextManager.clear();
            }
        }
    }
}
