package io.framework.reporting;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultSummaryTest {

    @Test
    void countsByStatusAndSumsDuration() {
        var summary = ResultSummary.of(List.of(
                TestResult.pass("a", 100, "d"),
                TestResult.fail("b", 200, "d", "boom"),
                TestResult.pass("c", 50, "d"),
                TestResult.skip("e", "d")));

        assertThat(summary.total()).isEqualTo(4);
        assertThat(summary.passed()).isEqualTo(2);
        assertThat(summary.failed()).isEqualTo(1);
        assertThat(summary.skipped()).isEqualTo(1);
        assertThat(summary.durationMs()).isEqualTo(350);
    }
}
