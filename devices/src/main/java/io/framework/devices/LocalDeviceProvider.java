package io.framework.devices;

import io.framework.core.config.Platform;
import io.framework.core.parallel.DeviceLease;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Discovers locally available devices:
 *   Android — {@code adb devices} (keeps entries in state "device")
 *   iOS     — {@code xcrun simctl list devices booted} (keeps Booted simulators)
 * Each discovered device gets a sequential Appium system port from {@link #BASE_SYSTEM_PORT}
 * so parallel sessions never clash. The command runner is injectable for unit testing.
 */
public final class LocalDeviceProvider implements DeviceProvider {

    public static final int BASE_SYSTEM_PORT = 8200;

    private static final Pattern BOOTED_SIM =
            Pattern.compile("\\(([0-9A-Fa-f-]{36})\\)\\s*\\(Booted\\)");

    private final CommandRunner runner;

    public LocalDeviceProvider() {
        this(new ProcessCommandRunner());
    }

    LocalDeviceProvider(CommandRunner runner) {
        this.runner = runner;
    }

    @Override
    public String name() {
        return "local";
    }

    @Override
    public List<DeviceLease> discover(Platform platform) {
        List<String> ids = new ArrayList<>();
        switch (platform) {
            case ANDROID -> ids.addAll(parseAdbDevices(runner.run("adb", "devices")));
            case IOS -> ids.addAll(parseBootedSimulators(
                    runner.run("xcrun", "simctl", "list", "devices", "booted")));
            case BOTH -> {
                ids.addAll(parseAdbDevices(runner.run("adb", "devices")));
                ids.addAll(parseBootedSimulators(
                        runner.run("xcrun", "simctl", "list", "devices", "booted")));
            }
        }
        List<DeviceLease> leases = new ArrayList<>();
        int port = BASE_SYSTEM_PORT;
        for (String id : ids) {
            leases.add(new DeviceLease(id, port++, name()));
        }
        return leases;
    }

    /** Parse `adb devices`: keep ids whose state column is exactly "device". */
    static List<String> parseAdbDevices(String output) {
        List<String> ids = new ArrayList<>();
        if (output == null) {
            return ids;
        }
        for (String line : output.split("\\R")) {
            String l = line.trim();
            if (l.isEmpty() || l.startsWith("List of devices")) {
                continue;
            }
            String[] parts = l.split("\\s+");
            if (parts.length >= 2 && parts[1].equals("device")) {
                ids.add(parts[0]);
            }
        }
        return ids;
    }

    /** Parse `xcrun simctl list devices booted`: capture UDIDs of Booted simulators. */
    static List<String> parseBootedSimulators(String output) {
        List<String> ids = new ArrayList<>();
        if (output == null) {
            return ids;
        }
        Matcher m = BOOTED_SIM.matcher(output);
        while (m.find()) {
            ids.add(m.group(1));
        }
        return ids;
    }
}
