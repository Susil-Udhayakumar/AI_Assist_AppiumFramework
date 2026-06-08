package io.framework.core.support;

import io.framework.core.config.Platform;
import io.framework.core.driver.DriverProvider;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.mockito.Mockito;

/**
 * Test double: returns a Mockito mock WebDriver instead of talking to a real device.
 * Mocks the WebDriver interface (not the concrete AppiumDriver) so Mockito's inline
 * maker never has to instrument the heavy Appium/Selenium concrete hierarchy.
 */
public class FakeDriverProvider implements DriverProvider {
    @Override
    public WebDriver create(Platform platform, Capabilities caps) {
        return Mockito.mock(WebDriver.class);
    }
}
