package io.framework.devices;

import io.framework.core.config.Platform;
import io.framework.core.parallel.DeviceLease;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDeviceProviderTest {

    private static final String ADB_OUTPUT = String.join("\n",
            "List of devices attached",
            "emulator-5554\tdevice",
            "RZ8N12345\tdevice",
            "0123offline\toffline",
            "");

    private static final String SIMCTL_OUTPUT = String.join("\n",
            "== Devices ==",
            "-- iOS 17.0 --",
            "    iPhone 15 (E1B2C3D4-1234-5678-9ABC-DEF012345678) (Booted)",
            "    iPad (11111111-2222-3333-4444-555555555555) (Shutdown)",
            "");

    @Test
    void nameIsLocal() {
        assertThat(new LocalDeviceProvider().name()).isEqualTo("local");
    }

    @Test
    void parsesOnlyOnlineAdbDevices() {
        assertThat(LocalDeviceProvider.parseAdbDevices(ADB_OUTPUT))
                .containsExactly("emulator-5554", "RZ8N12345");
    }

    @Test
    void parsesOnlyBootedSimulators() {
        assertThat(LocalDeviceProvider.parseBootedSimulators(SIMCTL_OUTPUT))
                .containsExactly("E1B2C3D4-1234-5678-9ABC-DEF012345678");
    }

    @Test
    void discoverAssignsSequentialSystemPorts() {
        var provider = new LocalDeviceProvider(cmd -> ADB_OUTPUT);
        List<DeviceLease> leases = provider.discover(Platform.ANDROID);

        assertThat(leases).extracting(DeviceLease::deviceId)
                .containsExactly("emulator-5554", "RZ8N12345");
        assertThat(leases).extracting(DeviceLease::systemPort)
                .containsExactly(LocalDeviceProvider.BASE_SYSTEM_PORT,
                        LocalDeviceProvider.BASE_SYSTEM_PORT + 1);
        assertThat(leases).allMatch(l -> l.providerName().equals("local"));
    }

    @Test
    void emptyOutputYieldsNoLeases() {
        var provider = new LocalDeviceProvider(cmd -> "List of devices attached\n");
        assertThat(provider.discover(Platform.ANDROID)).isEmpty();
    }
}
