package io.framework.knowledge;

import io.framework.locators.ElementHealer;
import io.framework.locators.HealRequest;
import io.framework.locators.LocatorCandidate;
import io.framework.locators.Strategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class MemoizingElementHealerTest {

    private final LocatorCandidate healedTo = new LocatorCandidate(Strategy.ID, "healed");
    private final HealRequest request = new HealRequest("Login", "btn",
            List.of(new LocatorCandidate(Strategy.ID, "login")), "");

    @Test
    void delegatesOnceThenServesFromCache(@TempDir Path dir) {
        AtomicInteger delegateCalls = new AtomicInteger();
        ElementHealer delegate = req -> {
            delegateCalls.incrementAndGet();
            return Optional.of(healedTo);
        };
        var healer = new MemoizingElementHealer(delegate, new HealMemory(dir));

        assertThat(healer.heal(request)).contains(healedTo);
        assertThat(healer.heal(request)).contains(healedTo);
        assertThat(delegateCalls.get()).isEqualTo(1);   // second call served from memory
    }

    @Test
    void cachedHealSurvivesRestart(@TempDir Path dir) {
        new MemoizingElementHealer(req -> Optional.of(healedTo), new HealMemory(dir)).heal(request);

        // a new run: delegate would now fail, but the cached heal is reused
        var afterRestart = new MemoizingElementHealer(req -> Optional.empty(), new HealMemory(dir));
        assertThat(afterRestart.heal(request)).contains(healedTo);
    }
}
