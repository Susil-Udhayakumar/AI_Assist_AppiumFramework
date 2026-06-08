package io.framework.drivers;

import io.framework.core.config.Platform;
import io.framework.core.exception.DriverInitException;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DriverFactoryTest {

    private DriverFactory factory() throws MalformedURLException {
        return new DriverFactory(new URL(DriverFactory.DEFAULT_SERVER));
    }

    @Test
    void bothPlatformIsRejectedBeforeAnySession() throws MalformedURLException {
        assertThatThrownBy(() -> factory().create(Platform.BOTH, new DesiredCapabilities()))
                .isInstanceOf(DriverInitException.class)
                .hasMessageContaining("BOTH");
    }

    @Test
    void exposesConfiguredServerUrl() throws MalformedURLException {
        assertThat(factory().serverUrl().toString()).isEqualTo(DriverFactory.DEFAULT_SERVER);
    }
}
