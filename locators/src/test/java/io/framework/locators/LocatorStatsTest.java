package io.framework.locators;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocatorStatsTest {

    private final LocatorCandidate id = new LocatorCandidate(Strategy.ID, "login");
    private final LocatorCandidate xpath = new LocatorCandidate(Strategy.XPATH, "//Button");

    @Test
    void preservesOrderWhenNoWins() {
        var stats = new LocatorStats();
        assertThat(stats.rank("Login", "btn", List.of(id, xpath)))
                .containsExactly(id, xpath);
    }

    @Test
    void promotesMoreSuccessfulCandidate() {
        var stats = new LocatorStats();
        stats.recordSuccess("Login", "btn", xpath);   // xpath now leads

        assertThat(stats.rank("Login", "btn", List.of(id, xpath)))
                .containsExactly(xpath, id);
    }

    @Test
    void winsAreScopedPerElement() {
        var stats = new LocatorStats();
        stats.recordSuccess("Login", "btn", xpath);
        assertThat(stats.wins("Login", "btn", xpath)).isEqualTo(1);
        assertThat(stats.wins("Other", "btn", xpath)).isZero();
    }
}
