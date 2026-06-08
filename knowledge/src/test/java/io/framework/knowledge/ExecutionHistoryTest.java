package io.framework.knowledge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionHistoryTest {

    @Test
    void writesDigestAndIndex(@TempDir Path dir) throws Exception {
        var digest = new RunDigest("2026-06-09", "android-14", "regression",
                148, 142, 6, 3, 492000,
                List.of("LoginTest.otp -> KNOWN bug JIRA-411", "CartTest.add -> NEW net-error"));

        Path md = new ExecutionHistory(dir).append(digest);

        String body = Files.readString(md);
        assertThat(body).contains("suite:regression")
                .contains("PASS 142 FAIL 6 FLAKY 3")
                .contains("JIRA-411")
                .contains("CartTest.add");

        Path index = dir.resolve("learned").resolve("history").resolve("index.md");
        assertThat(Files.readString(index)).contains("2026-06-09 regression");
    }

    @Test
    void appendsMultipleRunsToIndex(@TempDir Path dir) throws Exception {
        var history = new ExecutionHistory(dir);
        history.append(new RunDigest("2026-06-08", "android-14", "smoke", 10, 10, 0, 0, 1000, List.of()));
        history.append(new RunDigest("2026-06-09", "android-14", "smoke", 10, 9, 1, 0, 1100, List.of()));

        Path index = dir.resolve("learned").resolve("history").resolve("index.md");
        assertThat(Files.readString(index).lines().count()).isEqualTo(2);
    }
}
