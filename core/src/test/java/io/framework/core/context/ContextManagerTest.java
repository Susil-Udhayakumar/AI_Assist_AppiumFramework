package io.framework.core.context;

import org.openqa.selenium.WebDriver;
import io.framework.core.parallel.DeviceLease;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextManagerTest {

    private DriverContext sampleContext(String deviceId) {
        return DriverContext.builder()
                .driver(Mockito.mock(WebDriver.class))
                .device(new DeviceLease(deviceId, 8200, "local"))
                .build();
    }

    @Test
    void currentReturnsContextSetOnThisThread() {
        ContextManager.set(sampleContext("d1"));
        try {
            assertThat(ContextManager.current().device().deviceId()).isEqualTo("d1");
        } finally {
            ContextManager.clear();
        }
    }

    @Test
    void throwsWhenNoContextOnThread() {
        ContextManager.clear();
        assertThatThrownBy(ContextManager::current).hasMessageContaining("No DriverContext");
    }

    @Test
    void contextIsIsolatedPerThread() throws Exception {
        ContextManager.set(sampleContext("main"));
        try {
            var executor = Executors.newSingleThreadExecutor();
            Future<String> other = executor.submit(() -> {
                ContextManager.set(sampleContext("worker"));
                try {
                    return ContextManager.current().device().deviceId();
                } finally {
                    ContextManager.clear();
                }
            });
            assertThat(other.get()).isEqualTo("worker");
            assertThat(ContextManager.current().device().deviceId()).isEqualTo("main");
            executor.shutdownNow();
        } finally {
            ContextManager.clear();
        }
    }
}
