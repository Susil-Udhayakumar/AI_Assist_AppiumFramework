package io.framework.reporting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RtmHtmlWriterTest {

    @Test
    void writesMatrixWithStatusesAndCounts(@TempDir Path dir) throws Exception {
        List<RequirementCoverage> matrix = List.of(
                new RequirementCoverage("REQ-1", List.of("LoginTest.valid"), CoverageStatus.COVERED_PASSING),
                new RequirementCoverage("REQ-2", List.of("ProfileTest.edit"), CoverageStatus.NOT_COVERED));

        new RtmHtmlWriter().write(matrix, dir);

        String html = Files.readString(dir.resolve("rtm.html"));
        assertThat(html).contains("REQ-1").contains("REQ-2")
                .contains("COVERED_PASSING").contains("NOT_COVERED")
                .contains("Requirements: 2").contains("Not covered: 1");
    }
}
