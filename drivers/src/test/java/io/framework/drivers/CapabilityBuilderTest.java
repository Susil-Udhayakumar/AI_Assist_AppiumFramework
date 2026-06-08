package io.framework.drivers;

import io.framework.core.config.Platform;
import io.framework.core.exception.ConfigException;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Capabilities;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapabilityBuilderTest {

    @Test
    void androidSetsPlatformAutomationDeviceAndApp() {
        Capabilities caps = CapabilityBuilder.build(
                Platform.ANDROID, "Pixel_7", "emulator-5554",
                AppSource.installedAndroid("com.x.app", ".Main"), null);

        // Selenium normalizes the special W3C key "platformName" to a Platform enum; assert
        // case-insensitively. The appium:-prefixed keys are not normalized and assert exactly.
        assertThat(String.valueOf(caps.getCapability("platformName"))).isEqualToIgnoringCase("android");
        assertThat(caps.getCapability("appium:automationName")).isEqualTo("UiAutomator2");
        assertThat(caps.getCapability("appium:deviceName")).isEqualTo("Pixel_7");
        assertThat(caps.getCapability("appium:udid")).isEqualTo("emulator-5554");
        assertThat(caps.getCapability("appium:appPackage")).isEqualTo("com.x.app");
    }

    @Test
    void iosSetsXcuitestAndBundle() {
        Capabilities caps = CapabilityBuilder.build(
                Platform.IOS, "iPhone 15", null,
                AppSource.installedIos("com.x.app"), null);

        assertThat(String.valueOf(caps.getCapability("platformName"))).isEqualToIgnoringCase("ios");
        assertThat(caps.getCapability("appium:automationName")).isEqualTo("XCUITest");
        assertThat(caps.getCapability("appium:bundleId")).isEqualTo("com.x.app");
    }

    @Test
    void extraCapabilitiesAreApplied() {
        Capabilities caps = CapabilityBuilder.build(
                Platform.ANDROID, "Pixel_7", null, null,
                Map.of("appium:newCommandTimeout", 120));
        assertThat(caps.getCapability("appium:newCommandTimeout")).isEqualTo(120);
    }

    @Test
    void bothPlatformIsRejected() {
        assertThatThrownBy(() -> CapabilityBuilder.build(Platform.BOTH, "d", null, null, null))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("BOTH");
    }
}
