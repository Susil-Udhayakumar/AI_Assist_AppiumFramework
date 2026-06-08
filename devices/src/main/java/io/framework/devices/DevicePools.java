package io.framework.devices;

import io.framework.core.config.Platform;
import io.framework.core.exception.FrameworkException;
import io.framework.core.parallel.DeviceLease;
import io.framework.core.parallel.DevicePool;

import java.util.List;

/** Bridges a {@link DeviceProvider} to core's {@link DevicePool}. */
public final class DevicePools {

    private DevicePools() {
    }

    public static DevicePool fromProvider(DeviceProvider provider, Platform platform) {
        List<DeviceLease> leases = provider.discover(platform);
        if (leases.isEmpty()) {
            throw new FrameworkException("No devices found for platform " + platform
                    + " via provider '" + provider.name()
                    + "'. Is an emulator/simulator running (or the Appium/adb path set)?");
        }
        return new DevicePool(leases);
    }
}
