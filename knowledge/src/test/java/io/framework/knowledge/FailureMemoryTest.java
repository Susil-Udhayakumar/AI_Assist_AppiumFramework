package io.framework.knowledge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FailureMemoryTest {

    @Test
    void fingerprintGroupsFailuresDifferingOnlyByNumbers() {
        String a = FailureMemory.fingerprint("Timeout after 30s at line 42");
        String b = FailureMemory.fingerprint("Timeout after 5s at line 99");
        assertThat(a).isEqualTo(b);
    }

    @Test
    void differentFailuresHaveDifferentFingerprints() {
        assertThat(FailureMemory.fingerprint("ElementNotFound: login"))
                .isNotEqualTo(FailureMemory.fingerprint("Timeout waiting for home"));
    }

    @Test
    void recordsAndLooksUpAcrossInstances(@TempDir Path dir) {
        String fp = FailureMemory.fingerprint("api timeout on /login");
        new FailureMemory(dir).record(fp, "infra", "JIRA-411");

        var reloaded = new FailureMemory(dir);
        assertThat(reloaded.lookup(fp)).contains(new FailureRecord("infra", "JIRA-411"));
    }

    @Test
    void emptyWhenUnknown(@TempDir Path dir) {
        assertThat(new FailureMemory(dir).lookup("nope")).isEmpty();
    }
}
