package io.framework.core.driver;

import io.framework.core.config.Platform;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

/**
 * SPI implemented by the `drivers` module (real Android/iOS) and by test doubles.
 * Keeps `core` independent of concrete driver construction so the engine is testable.
 *
 * Returns the narrow {@link WebDriver} interface deliberately: core only needs the
 * WebDriver contract (quit, etc.), and concrete Appium drivers cannot be mocked via
 * Mockito's inline maker. Real providers return an {@code AppiumDriver} (a WebDriver);
 * Appium-specific modules reach it through {@code DriverContext.appiumDriver()}.
 */
public interface DriverProvider {
    WebDriver create(Platform platform, Capabilities caps);
}
