package io.framework.devices.browserstack;

import io.framework.core.config.Platform;
import io.framework.core.parallel.DeviceLease;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BrowserStackDeviceProviderTest {

    @Test
    void nameIsBrowserstack() {
        assertThat(new BrowserStackDeviceProvider(List.of()).name()).isEqualTo("browserstack");
    }

    @Test
    void parsesNameAndOsVersionList() {
        List<CloudDevice> devices = BrowserStackDeviceProvider.parse("Google Pixel 7:13.0, iPhone 15:17");
        assertThat(devices).containsExactly(
                new CloudDevice("Google Pixel 7", "13.0"),
                new CloudDevice("iPhone 15", "17"));
    }

    @Test
    void parseHandlesNullAndBlank() {
        assertThat(BrowserStackDeviceProvider.parse(null)).isEmpty();
        assertThat(BrowserStackDeviceProvider.parse("   ")).isEmpty();
    }

    @Test
    void discoverYieldsCloudLeases() {
        var provider = new BrowserStackDeviceProvider(List.of(
                new CloudDevice("Google Pixel 7", "13.0"),
                new CloudDevice("iPhone 15", "17")));

        List<DeviceLease> leases = provider.discover(Platform.ANDROID);
        assertThat(leases).extracting(DeviceLease::deviceId)
                .containsExactly("Google Pixel 7", "iPhone 15");
        assertThat(leases).allMatch(l -> l.providerName().equals("browserstack"));
    }
}
