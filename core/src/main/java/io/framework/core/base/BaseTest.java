package io.framework.core.base;

import io.framework.core.config.Platform;
import io.framework.core.lifecycle.DriverLifecycle;
import org.openqa.selenium.Capabilities;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.util.concurrent.TimeUnit;

/**
 * TestNG lifecycle skeleton (Template Method). Concrete test base classes in higher
 * modules supply the wired DriverLifecycle, Platform, and Capabilities; here we only
 * define the start/stop hooks so every test gets context setup + teardown for free.
 *
 * Subclasses override provideLifecycle()/providePlatform()/provideCapabilities().
 */
public abstract class BaseTest {

    protected abstract DriverLifecycle provideLifecycle();
    protected abstract Platform providePlatform();
    protected abstract Capabilities provideCapabilities();

    private DriverLifecycle lifecycle;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        lifecycle = provideLifecycle();
        lifecycle.start(providePlatform(), provideCapabilities(), 60, TimeUnit.SECONDS);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        boolean failed = result.getStatus() == ITestResult.FAILURE;
        lifecycle.stop(failed);
    }
}
