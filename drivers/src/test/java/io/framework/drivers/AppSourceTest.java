package io.framework.drivers;

import io.framework.core.exception.ConfigException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppSourceTest {

    @Test
    void fileAppSetsAbsoluteAppCapability(@TempDir Path dir) throws IOException {
        Path apk = Files.writeString(dir.resolve("app.apk"), "binary");
        var caps = new DesiredCapabilities();

        AppSource.file(apk.toString()).applyTo(caps);

        assertThat(caps.getCapability("appium:app")).isEqualTo(apk.toAbsolutePath().toString());
    }

    @Test
    void missingFileFailsFast() {
        var caps = new DesiredCapabilities();
        assertThatThrownBy(() -> AppSource.file("does/not/exist.apk").applyTo(caps))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void urlAppSetsAppCapability() {
        var caps = new DesiredCapabilities();
        AppSource.url("https://cdn/app.ipa").applyTo(caps);
        assertThat(caps.getCapability("appium:app")).isEqualTo("https://cdn/app.ipa");
    }

    @Test
    void installedAndroidSetsPackageAndActivity() {
        var caps = new DesiredCapabilities();
        AppSource.installedAndroid("com.x.app", ".Main").applyTo(caps);
        assertThat(caps.getCapability("appium:appPackage")).isEqualTo("com.x.app");
        assertThat(caps.getCapability("appium:appActivity")).isEqualTo(".Main");
    }

    @Test
    void installedIosSetsBundleId() {
        var caps = new DesiredCapabilities();
        AppSource.installedIos("com.x.app").applyTo(caps);
        assertThat(caps.getCapability("appium:bundleId")).isEqualTo("com.x.app");
    }

    @Test
    void installedAndroidWithoutPackageFailsFast() {
        var caps = new DesiredCapabilities();
        assertThatThrownBy(() -> AppSource.installedAndroid("  ", ".Main").applyTo(caps))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("appPackage");
    }
}
