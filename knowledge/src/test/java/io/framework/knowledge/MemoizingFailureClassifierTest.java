package io.framework.knowledge;

import io.framework.core.failure.FailureCategory;
import io.framework.core.failure.FailureClassifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class MemoizingFailureClassifierTest {

    @Test
    void delegatesOnceThenServesFromMemory(@TempDir Path dir) {
        AtomicInteger calls = new AtomicInteger();
        FailureClassifier delegate = text -> {
            calls.incrementAndGet();
            return FailureCategory.NETWORK;
        };
        var classifier = new MemoizingFailureClassifier(delegate, new FailureMemory(dir));

        assertThat(classifier.classify("Connection refused at 30s")).isEqualTo(FailureCategory.NETWORK);
        // same failure shape (numbers differ) -> same fingerprint -> served from memory
        assertThat(classifier.classify("Connection refused at 5s")).isEqualTo(FailureCategory.NETWORK);
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void knownClassificationSurvivesRestart(@TempDir Path dir) {
        new MemoizingFailureClassifier(t -> FailureCategory.INFRA, new FailureMemory(dir))
                .classify("appium session error 1");

        var afterRestart = new MemoizingFailureClassifier(
                t -> FailureCategory.UNKNOWN, new FailureMemory(dir));
        assertThat(afterRestart.classify("appium session error 2")).isEqualTo(FailureCategory.INFRA);
    }
}
