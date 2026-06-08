package io.framework.core.lifecycle;

import org.openqa.selenium.WebDriver;
import io.framework.core.config.Platform;
import io.framework.core.context.ContextManager;
import io.framework.core.events.EventBus;
import io.framework.core.events.TestEvent;
import io.framework.core.parallel.DeviceLease;
import io.framework.core.parallel.DevicePool;
import io.framework.core.support.FakeDriverProvider;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DriverLifecycleTest {

    private DriverLifecycle lifecycle(List<String> events) {
        var pool = new DevicePool(List.of(new DeviceLease("d1", 8200, "local")));
        var bus = new EventBus();
        bus.subscribe(e -> events.add(e.event().name()));
        return new DriverLifecycle(pool, new FakeDriverProvider(), bus);
    }

    @Test
    void startSetsContextAndEmitsTestStart() {
        List<String> events = new ArrayList<>();
        var lc = lifecycle(events);
        Capabilities caps = new DesiredCapabilities();

        lc.start(Platform.ANDROID, caps, 2, TimeUnit.SECONDS);
        try {
            assertThat(ContextManager.isSet()).isTrue();
            assertThat(ContextManager.current().device().deviceId()).isEqualTo("d1");
            assertThat(events).contains(TestEvent.TEST_START.name());
        } finally {
            lc.stop(false);
        }
    }

    @Test
    void stopQuitsDriverReleasesDeviceClearsContextEmitsTestEnd() {
        List<String> events = new ArrayList<>();
        var lc = lifecycle(events);
        lc.start(Platform.ANDROID, new DesiredCapabilities(), 2, TimeUnit.SECONDS);
        WebDriver driver = ContextManager.current().driver();

        lc.stop(false);

        assertThat(ContextManager.isSet()).isFalse();
        org.mockito.Mockito.verify(driver).quit();
        assertThat(events).contains(TestEvent.TEST_END.name());
        // device returned to pool: a fresh start must succeed
        lc.start(Platform.ANDROID, new DesiredCapabilities(), 1, TimeUnit.SECONDS);
        lc.stop(false);
    }

    @Test
    void stopWithFailureEmitsTestFailBeforeTestEnd() {
        List<String> events = new ArrayList<>();
        var lc = lifecycle(events);
        lc.start(Platform.ANDROID, new DesiredCapabilities(), 2, TimeUnit.SECONDS);

        lc.stop(true);

        int failIdx = events.indexOf(TestEvent.TEST_FAIL.name());
        int endIdx = events.indexOf(TestEvent.TEST_END.name());
        assertThat(failIdx).isGreaterThanOrEqualTo(0);
        assertThat(endIdx).isGreaterThan(failIdx);
    }
}
