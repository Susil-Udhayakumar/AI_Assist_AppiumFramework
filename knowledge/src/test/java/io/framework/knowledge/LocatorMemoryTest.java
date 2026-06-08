package io.framework.knowledge;

import io.framework.locators.LocatorCandidate;
import io.framework.locators.Strategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocatorMemoryTest {

    private final LocatorCandidate id = new LocatorCandidate(Strategy.ID, "login");
    private final LocatorCandidate xpath = new LocatorCandidate(Strategy.XPATH, "//Button");

    @Test
    void promotesSuccessfulCandidate(@TempDir Path dir) {
        var memory = new LocatorMemory(dir);
        memory.recordSuccess("Login", "btn", xpath);

        assertThat(memory.rank("Login", "btn", List.of(id, xpath)))
                .containsExactly(xpath, id);
    }

    @Test
    void persistsAcrossInstances(@TempDir Path dir) {
        new LocatorMemory(dir).recordSuccess("Login", "btn", xpath);

        // a fresh instance (new run) reads the same store
        var reloaded = new LocatorMemory(dir);
        assertThat(reloaded.wins("Login", "btn", xpath)).isEqualTo(1);
        assertThat(reloaded.rank("Login", "btn", List.of(id, xpath))).containsExactly(xpath, id);
    }
}
