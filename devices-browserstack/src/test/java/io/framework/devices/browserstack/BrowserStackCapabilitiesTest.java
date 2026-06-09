package io.framework.devices.browserstack;

import io.framework.core.config.Platform;
import io.framework.core.exception.ConfigException;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Capabilities;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrowserStackCapabilitiesTest {

    @Test
    @SuppressWarnings("unchecked")
    void buildsPlatformVersionAppAndBstackOptions() {
        Capabilities caps = BrowserStackCapabilities.build(
                Platform.ANDROID, new CloudDevice("Google Pixel 7", "13.0"), "bs://app123",
                new CloudCredentials("user1", "key1"), "MyProject", "build-42");

        assertThat(String.valueOf(caps.getCapability("platformName"))).isEqualToIgnoringCase("android");
        assertThat(caps.getCapability("appium:platformVersion")).isEqualTo("13.0");
        assertThat(caps.getCapability("appium:app")).isEqualTo("bs://app123");

        Map<String, Object> bstack = (Map<String, Object>) caps.getCapability("bstack:options");
        assertThat(bstack)
                .containsEntry("userName", "user1")
                .containsEntry("accessKey", "key1")
                .containsEntry("deviceName", "Google Pixel 7")
                .containsEntry("osVersion", "13.0")
                .containsEntry("projectName", "MyProject")
                .containsEntry("buildName", "build-42");
    }

    @Test
    void bothPlatformRejected() {
        assertThatThrownBy(() -> BrowserStackCapabilities.build(
                Platform.BOTH, new CloudDevice("x", "1"), null, new CloudCredentials("u", "k"), null, null))
                .isInstanceOf(ConfigException.class);
    }
}
