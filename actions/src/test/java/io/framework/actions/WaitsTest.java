package io.framework.actions;

import io.framework.core.exception.ConfigException;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class WaitsTest {

    @Test
    void defaultsAreReasonable() {
        WaitConfig c = WaitConfig.defaults();
        assertThat(c.timeout()).isEqualTo(Duration.ofSeconds(20));
        assertThat(c.poll()).isEqualTo(Duration.ofMillis(500));
    }

    @Test
    void rejectsNonPositiveTimeout() {
        assertThatThrownBy(() -> new WaitConfig(Duration.ZERO, Duration.ofMillis(500)))
                .isInstanceOf(ConfigException.class);
    }

    @Test
    void untilReturnsWhenConditionImmediatelySatisfied() {
        var waits = new Waits(WaitConfig.defaults());
        String result = waits.until(mock(WebDriver.class), driver -> "ready");
        assertThat(result).isEqualTo("ready");
    }
}
