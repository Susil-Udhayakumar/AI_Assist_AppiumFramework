package io.framework.devices;

import io.framework.core.config.Platform;
import io.framework.core.parallel.DeviceLease;

import java.util.List;

/**
 * SPI for a device source. Implementations are discovered via ServiceLoader and selected by
 * {@code name()} (config {@code device.target}). The local provider lives here; cloud
 * providers (BrowserStack/Sauce/LambdaTest) ship as separate jars in later waves.
 */
public interface DeviceProvider {

    /** Backend id, e.g. "local", "browserstack". */
    String name();

    /** Discover the devices currently available for the given platform, as leases. */
    List<DeviceLease> discover(Platform platform);
}
