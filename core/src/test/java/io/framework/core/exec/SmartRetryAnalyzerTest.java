package io.framework.core.exec;

import io.framework.core.config.RetryPolicy;
import io.framework.core.failure.FailureCategory;
import io.framework.core.failure.FailureClassifier;
import org.junit.jupiter.api.Test;
import org.testng.ITestResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmartRetryAnalyzerTest {

    private ITestResult resultWith(Throwable t) {
        ITestResult r = mock(ITestResult.class);
        when(r.getThrowable()).thenReturn(t);
        return r;
    }

    private RetryPolicy policy(boolean enabled, int max, String... retryOn) {
        return new RetryPolicy(enabled, max, List.of(retryOn), 3);
    }

    @Test
    void retriesTransientFailureUpToMax() {
        FailureClassifier network = text -> FailureCategory.NETWORK;
        var analyzer = new SmartRetryAnalyzer(policy(true, 2, "network"), network);
        ITestResult result = resultWith(new RuntimeException("boom"));

        assertThat(analyzer.retry(result)).isTrue();   // attempt 1
        assertThat(analyzer.retry(result)).isTrue();   // attempt 2
        assertThat(analyzer.retry(result)).isFalse();  // exhausted
        assertThat(analyzer.attempts()).isEqualTo(2);
    }

    @Test
    void neverRetriesAssertionFailures() {
        FailureClassifier assertion = text -> FailureCategory.ASSERTION;
        var analyzer = new SmartRetryAnalyzer(policy(true, 3, "assertion"), assertion);

        assertThat(analyzer.retry(resultWith(new AssertionError("expected")))).isFalse();
    }

    @Test
    void doesNotRetryCategoryNotInPolicy() {
        FailureClassifier infra = text -> FailureCategory.INFRA;
        var analyzer = new SmartRetryAnalyzer(policy(true, 2, "network"), infra);

        assertThat(analyzer.retry(resultWith(new RuntimeException()))).isFalse();
    }

    @Test
    void disabledPolicyNeverRetries() {
        FailureClassifier network = text -> FailureCategory.NETWORK;
        var analyzer = new SmartRetryAnalyzer(policy(false, 5, "network"), network);

        assertThat(analyzer.retry(resultWith(new RuntimeException()))).isFalse();
    }
}
