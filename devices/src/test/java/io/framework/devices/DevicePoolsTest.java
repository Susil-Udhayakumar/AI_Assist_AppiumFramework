package io.framework.devices;

import io.framework.core.config.Platform;
import io.framework.core.exception.FrameworkException;
import io.framework.core.parallel.DeviceLease;
import io.framework.core.parallel.DevicePool;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevicePoolsTest {

    private DeviceProvider providerReturning(List<DeviceLease> leases) {
        return new DeviceProvider() {
            public String name() { return "fake"; }
            public List<DeviceLease> discover(Platform platform) { return leases; }
        };
    }

    @Test
    void buildsPoolWithDiscoveredCapacity() {
        var provider = providerReturning(List.of(
                new DeviceLease("d0", 8200, "fake"),
                new DeviceLease("d1", 8201, "fake")));

        DevicePool pool = DevicePools.fromProvider(provider, Platform.ANDROID);

        assertThat(pool.capacity()).isEqualTo(2);
    }

    @Test
    void noDevicesFailsFast() {
        var provider = providerReturning(List.of());
        assertThatThrownBy(() -> DevicePools.fromProvider(provider, Platform.ANDROID))
                .isInstanceOf(FrameworkException.class)
                .hasMessageContaining("No devices found");
    }
}
