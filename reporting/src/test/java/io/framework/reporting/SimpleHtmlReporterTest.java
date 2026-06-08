package io.framework.reporting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleHtmlReporterTest {

    @Test
    void writesReportWithSummaryAndRows(@TempDir Path dir) throws Exception {
        new SimpleHtmlReporter().report(List.of(
                TestResult.pass("LoginTest.valid", 120, "emulator-5554"),
                TestResult.fail("LoginTest.invalid", 80, "emulator-5554", "assertion failed")), dir);

        String html = Files.readString(dir.resolve("report.html"));
        assertThat(html).contains("Passed: 1").contains("Failed: 1");
        assertThat(html).contains("LoginTest.valid").contains("LoginTest.invalid");
        assertThat(html).contains("assertion failed");
    }

    @Test
    void escapesHtmlInMessages(@TempDir Path dir) throws Exception {
        new SimpleHtmlReporter().report(List.of(
                TestResult.fail("t", 1, "d", "<script>x</script>")), dir);

        String html = Files.readString(dir.resolve("report.html"));
        assertThat(html).contains("&lt;script&gt;").doesNotContain("<script>x</script>");
    }

    @Test
    void reporterNameIsHtml() {
        assertThat(new SimpleHtmlReporter().name()).isEqualTo("html");
    }
}
