package io.framework.core.base;

import io.appium.java_client.AppiumDriver;
import io.framework.core.context.ContextManager;
import io.framework.core.context.DriverContext;
import org.openqa.selenium.WebDriver;

/**
 * Base for page objects. Subclasses call driver()/appiumDriver()/context() to reach the
 * current thread's session without threading the driver through constructors.
 */
public abstract class BaseScreen {

    protected DriverContext context() {
        return ContextManager.current();
    }

    protected WebDriver driver() {
        return context().driver();
    }

    /** For Appium-specific gestures/actions in higher modules. */
    protected AppiumDriver appiumDriver() {
        return context().appiumDriver();
    }
}
