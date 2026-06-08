package io.framework.core.context;

import io.appium.java_client.AppiumDriver;
import io.framework.core.config.FrameworkConfig;
import io.framework.core.parallel.DeviceLease;
import org.openqa.selenium.WebDriver;

import java.util.Objects;

/**
 * Per-thread world for one test on one device. The isolation boundary:
 * everything a running test reaches goes through the current context.
 *
 * The driver is held as the narrow {@link WebDriver} interface (core only needs that
 * contract, and it keeps the type mockable). Appium-specific modules call
 * {@link #appiumDriver()} to get the concrete driver. config may be null in unit tests
 * that only exercise driver/device wiring.
 */
public final class DriverContext {

    private final WebDriver driver;
    private final DeviceLease device;
    private final FrameworkConfig config;

    private DriverContext(Builder b) {
        this.driver = Objects.requireNonNull(b.driver, "driver");
        this.device = Objects.requireNonNull(b.device, "device");
        this.config = b.config;
    }

    public WebDriver driver() { return driver; }

    /** Convenience for Appium-specific modules. Throws ClassCastException if the driver
     *  is not an AppiumDriver (e.g. a plain WebDriver mock in a unit test). */
    public AppiumDriver appiumDriver() { return (AppiumDriver) driver; }

    public DeviceLease device() { return device; }
    public FrameworkConfig config() { return config; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private WebDriver driver;
        private DeviceLease device;
        private FrameworkConfig config;

        public Builder driver(WebDriver d) { this.driver = d; return this; }
        public Builder device(DeviceLease d) { this.device = d; return this; }
        public Builder config(FrameworkConfig c) { this.config = c; return this; }
        public DriverContext build() { return new DriverContext(this); }
    }
}
