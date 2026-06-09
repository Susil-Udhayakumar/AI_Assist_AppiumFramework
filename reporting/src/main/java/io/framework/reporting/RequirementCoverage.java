package io.framework.reporting;

import java.util.List;

/** Traceability row: a requirement, the tests mapped to it, and its resulting coverage state. */
public record RequirementCoverage(String requirementId, List<String> coveringTests, CoverageStatus status) {

    public RequirementCoverage {
        coveringTests = List.copyOf(coveringTests);
    }
}
