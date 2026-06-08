package io.framework.reporting;

import java.nio.file.Path;
import java.util.List;

/**
 * SPI for a report format. Implementations are discovered via ServiceLoader; the built-in HTML
 * reporter lives here, Extent/Allure/custom reporters ship as separate jars in later waves.
 */
public interface Reporter {

    /** Format id, e.g. "html", "allure". */
    String name();

    /** Write the report for the given results into outputDir. */
    void report(List<TestResult> results, Path outputDir);
}
