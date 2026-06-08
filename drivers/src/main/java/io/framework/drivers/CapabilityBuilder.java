package io.framework.drivers;

import io.framework.core.config.Platform;
import io.framework.core.exception.ConfigException;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.Map;

/**
 * Builds W3C Appium capabilities for a concrete platform from device info + an
 * {@link AppSource} + optional extra capabilities. Android uses the UiAutomator2
 * automation engine, iOS uses XCUITest. Appium-specific keys carry the "appium:" prefix;
 * platformName is the standard W3C key.
 */
public final class CapabilityBuilder {

    private CapabilityBuilder() {
    }

    public static Capabilities build(Platform platform, String deviceName, String udid,
                                     AppSource app, Map<String, Object> extra) {
        DesiredCapabilities caps = new DesiredCapabilities();
        switch (platform) {
            case ANDROID -> {
                caps.setCapability("platformName", "Android");
                caps.setCapability("appium:automationName", "UiAutomator2");
            }
            case IOS -> {
                caps.setCapability("platformName", "iOS");
                caps.setCapability("appium:automationName", "XCUITest");
            }
            case BOTH -> throw new ConfigException(
                    "CapabilityBuilder needs a concrete platform (ANDROID or IOS), got BOTH");
        }
        if (deviceName != null) {
            caps.setCapability("appium:deviceName", deviceName);
        }
        if (udid != null) {
            caps.setCapability("appium:udid", udid);
        }
        if (app != null) {
            app.applyTo(caps);
        }
        if (extra != null) {
            extra.forEach(caps::setCapability);
        }
        return caps;
    }
}
