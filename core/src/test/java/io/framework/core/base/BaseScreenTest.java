package io.framework.core.base;

import io.framework.core.context.ContextManager;
import io.framework.core.context.DriverContext;
import io.framework.core.parallel.DeviceLease;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openqa.selenium.WebDriver;
import static org.assertj.core.api.Assertions.assertThat;

class BaseScreenTest {

    static class HomeScreen extends BaseScreen {
        WebDriver exposedDriver() { return driver(); }
    }

    @AfterEach
    void tearDown() { ContextManager.clear(); }

    @Test
    void screenReadsDriverFromCurrentContext() {
        WebDriver mock = Mockito.mock(WebDriver.class);
        ContextManager.set(DriverContext.builder()
                .driver(mock)
                .device(new DeviceLease("d1", 8200, "local"))
                .build());

        assertThat(new HomeScreen().exposedDriver()).isSameAs(mock);
    }
}
