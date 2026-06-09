package io.framework.devices.browserstack;

import io.framework.core.config.Platform;
import io.framework.core.exception.ConfigException;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds W3C capabilities for a BrowserStack session: standard platform/version + the
 * {@code bstack:options} block (credentials, device, project/build). To run, point the existing
 * {@code DriverFactory} at {@link #HUB_URL} (via {@code -Dappium.server.url}); the app under test
 * is referenced by its uploaded BrowserStack app id (bs://...).
 */
public final class BrowserStackCapabilities {

    public static final String HUB_URL = "https://hub.browserstack.com/wd/hub";

    private BrowserStackCapabilities() {
    }

    public static Capabilities build(Platform platform, CloudDevice device, String appUrl,
                                     CloudCredentials credentials, String projectName, String buildName) {
        if (platform == Platform.BOTH) {
            throw new ConfigException("BrowserStack needs a concrete platform (ANDROID or IOS), got BOTH");
        }
        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability("platformName", platform == Platform.IOS ? "iOS" : "Android");
        if (device.osVersion() != null) {
            caps.setCapability("appium:platformVersion", device.osVersion());
        }
        if (appUrl != null) {
            caps.setCapability("appium:app", appUrl);
        }

        Map<String, Object> bstack = new LinkedHashMap<>();
        bstack.put("userName", credentials.userName());
        bstack.put("accessKey", credentials.accessKey());
        bstack.put("deviceName", device.name());
        if (device.osVersion() != null) {
            bstack.put("osVersion", device.osVersion());
        }
        if (projectName != null) {
            bstack.put("projectName", projectName);
        }
        if (buildName != null) {
            bstack.put("buildName", buildName);
        }
        caps.setCapability("bstack:options", bstack);
        return caps;
    }
}
