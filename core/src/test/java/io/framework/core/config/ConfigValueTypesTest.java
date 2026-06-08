package io.framework.core.config;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ConfigValueTypesTest {

    @Test
    void platformParsesCaseInsensitively() {
        assertThat(Platform.from("android")).isEqualTo(Platform.ANDROID);
        assertThat(Platform.from("IOS")).isEqualTo(Platform.IOS);
        assertThat(Platform.from("Both")).isEqualTo(Platform.BOTH);
    }

    @Test
    void executionHoldsThreadAndMode() {
        var e = new Execution(Execution.Mode.PARALLEL, Execution.ParallelBy.DEVICE, 4);
        assertThat(e.threads()).isEqualTo(4);
        assertThat(e.mode()).isEqualTo(Execution.Mode.PARALLEL);
        assertThat(e.parallelBy()).isEqualTo(Execution.ParallelBy.DEVICE);
    }

    @Test
    void retryPolicyExposesRetryOnSet() {
        var r = new RetryPolicy(true, 2, List.of("infra", "network"), 3);
        assertThat(r.shouldRetryOn("network")).isTrue();
        assertThat(r.shouldRetryOn("assertion")).isFalse();
    }
}
