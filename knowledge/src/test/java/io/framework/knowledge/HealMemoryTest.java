package io.framework.knowledge;

import io.framework.locators.LocatorCandidate;
import io.framework.locators.Strategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HealMemoryTest {

    private final List<LocatorCandidate> tried = List.of(
            new LocatorCandidate(Strategy.ID, "login"),
            new LocatorCandidate(Strategy.XPATH, "//Button"));

    @Test
    void signatureIsStableForSameInput() {
        assertThat(HealMemory.signature("Login", "btn", tried))
                .isEqualTo(HealMemory.signature("Login", "btn", tried));
    }

    @Test
    void recordsAndLooksUpHealAcrossInstances(@TempDir Path dir) {
        String sig = HealMemory.signature("Login", "btn", tried);
        var healed = new LocatorCandidate(Strategy.ACCESSIBILITY_ID, "login_btn");

        new HealMemory(dir).record(sig, healed);

        var reloaded = new HealMemory(dir);
        assertThat(reloaded.lookup(sig)).contains(healed);
    }

    @Test
    void preservesValuesContainingEquals(@TempDir Path dir) {
        String sig = HealMemory.signature("S", "e", tried);
        var healed = new LocatorCandidate(Strategy.XPATH, "//*[@id='a=b']");

        new HealMemory(dir).record(sig, healed);

        assertThat(new HealMemory(dir).lookup(sig)).contains(healed);
    }

    @Test
    void emptyWhenUnknown(@TempDir Path dir) {
        assertThat(new HealMemory(dir).lookup("nope")).isEmpty();
    }
}
