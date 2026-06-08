package io.framework.reporting;

import java.nio.file.Path;
import java.util.List;

/** Fan-out: forwards results to every configured {@link Reporter} (Extent + Allure + custom...). */
public final class Reporters {

    private final List<Reporter> reporters;

    public Reporters(List<Reporter> reporters) {
        this.reporters = List.copyOf(reporters);
    }

    public void report(List<TestResult> results, Path outputDir) {
        for (Reporter reporter : reporters) {
            reporter.report(results, outputDir);
        }
    }

    public int size() {
        return reporters.size();
    }
}
