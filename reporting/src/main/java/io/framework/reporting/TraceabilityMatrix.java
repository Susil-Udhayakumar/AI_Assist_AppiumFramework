package io.framework.reporting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a Requirement Traceability Matrix from a requirement→tests mapping and a run's results.
 * A requirement is NOT_COVERED if none of its mapped tests executed; otherwise it is
 * COVERED_FAILING if any covering test failed, else COVERED_PASSING.
 */
public final class TraceabilityMatrix {

    private TraceabilityMatrix() {
    }

    public static List<RequirementCoverage> build(Map<String, ? extends Collection<String>> requirementToTests,
                                                  List<TestResult> results) {
        Map<String, TestStatus> statusByTest = new LinkedHashMap<>();
        for (TestResult r : results) {
            statusByTest.merge(r.name(), r.status(),
                    (existing, incoming) -> existing == TestStatus.FAIL ? existing : incoming);
        }

        List<RequirementCoverage> matrix = new ArrayList<>();
        for (Map.Entry<String, ? extends Collection<String>> entry : requirementToTests.entrySet()) {
            List<String> tests = new ArrayList<>(entry.getValue());
            boolean anyExecuted = tests.stream().anyMatch(statusByTest::containsKey);
            CoverageStatus status;
            if (!anyExecuted) {
                status = CoverageStatus.NOT_COVERED;
            } else {
                boolean anyFailed = tests.stream()
                        .map(statusByTest::get)
                        .anyMatch(s -> s == TestStatus.FAIL);
                status = anyFailed ? CoverageStatus.COVERED_FAILING : CoverageStatus.COVERED_PASSING;
            }
            matrix.add(new RequirementCoverage(entry.getKey(), tests, status));
        }
        return matrix;
    }
}
