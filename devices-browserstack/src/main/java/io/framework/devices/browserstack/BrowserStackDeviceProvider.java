package io.framework.devices.browserstack;

import io.framework.core.config.Platform;
import io.framework.core.parallel.DeviceLease;
import io.framework.devices.DeviceProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Cloud {@link DeviceProvider} for BrowserStack. The device matrix is configured (not adb-
 * discovered): set {@code BROWSERSTACK_DEVICES} as a comma list of {@code Name:osVersion}
 * (e.g. {@code "Google Pixel 7:13.0, iPhone 15:17"}), or construct with an explicit list.
 * systemPort is 0 — the cloud manages ports. Selected by config {@code device.target=browserstack}.
 */
public final class BrowserStackDeviceProvider implements DeviceProvider {

    private final List<CloudDevice> devices;

    public BrowserStackDeviceProvider() {
        this(parse(System.getenv("BROWSERSTACK_DEVICES")));
    }

    public BrowserStackDeviceProvider(List<CloudDevice> devices) {
        this.devices = List.copyOf(devices);
    }

    @Override
    public String name() {
        return "browserstack";
    }

    @Override
    public List<DeviceLease> discover(Platform platform) {
        List<DeviceLease> leases = new ArrayList<>();
        for (CloudDevice device : devices) {
            leases.add(new DeviceLease(device.name(), 0, name()));
        }
        return leases;
    }

    static List<CloudDevice> parse(String env) {
        List<CloudDevice> out = new ArrayList<>();
        if (env == null || env.isBlank()) {
            return out;
        }
        for (String part : env.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            int colon = p.lastIndexOf(':');
            if (colon > 0) {
                out.add(new CloudDevice(p.substring(0, colon).trim(), p.substring(colon + 1).trim()));
            } else {
                out.add(new CloudDevice(p, null));
            }
        }
        return out;
    }
}
