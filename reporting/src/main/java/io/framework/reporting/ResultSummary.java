package io.framework.reporting;

import java.util.List;

/** Aggregate counts over a set of results. */
public record ResultSummary(int total, int passed, int failed, int skipped, long durationMs) {

    public static ResultSummary of(List<TestResult> results) {
        int passed = 0;
        int failed = 0;
        int skipped = 0;
        long duration = 0;
        for (TestResult r : results) {
            duration += r.durationMs();
            switch (r.status()) {
                case PASS -> passed++;
                case FAIL -> failed++;
                case SKIP -> skipped++;
            }
        }
        return new ResultSummary(results.size(), passed, failed, skipped, duration);
    }
}
