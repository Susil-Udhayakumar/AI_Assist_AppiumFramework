package io.framework.reporting;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TraceabilityMatrixTest {

    private RequirementCoverage byId(List<RequirementCoverage> m, String id) {
        return m.stream().filter(c -> c.requirementId().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void classifiesPassingFailingAndUncovered() {
        Map<String, List<String>> reqToTests = Map.of(
                "REQ-1", List.of("LoginTest.valid"),
                "REQ-2", List.of("LoginTest.invalid"),
                "REQ-3", List.of("ProfileTest.edit"));   // not executed this run

        List<TestResult> results = List.of(
                TestResult.pass("LoginTest.valid", 10, "d"),
                TestResult.fail("LoginTest.invalid", 10, "d", "boom"));

        List<RequirementCoverage> matrix = TraceabilityMatrix.build(reqToTests, results);

        assertThat(byId(matrix, "REQ-1").status()).isEqualTo(CoverageStatus.COVERED_PASSING);
        assertThat(byId(matrix, "REQ-2").status()).isEqualTo(CoverageStatus.COVERED_FAILING);
        assertThat(byId(matrix, "REQ-3").status()).isEqualTo(CoverageStatus.NOT_COVERED);
    }

    @Test
    void anyFailingCoveringTestMakesRequirementFailing() {
        Map<String, List<String>> reqToTests = Map.of("REQ-1", List.of("a", "b"));
        List<TestResult> results = List.of(
                TestResult.pass("a", 1, "d"),
                TestResult.fail("b", 1, "d", "x"));

        assertThat(byId(TraceabilityMatrix.build(reqToTests, results), "REQ-1").status())
                .isEqualTo(CoverageStatus.COVERED_FAILING);
    }
}
