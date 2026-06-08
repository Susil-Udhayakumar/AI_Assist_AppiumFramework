package io.framework.ai.heuristic;

import io.framework.core.failure.FailureCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicFailureClassifierTest {

    private final HeuristicFailureClassifier classifier = new HeuristicFailureClassifier();

    @Test
    void classifiesElementNotFound() {
        assertThat(classifier.classify("org.openqa.selenium.NoSuchElementException: no such element"))
                .isEqualTo(FailureCategory.ELEMENT_NOT_FOUND);
    }

    @Test
    void classifiesTimeout() {
        assertThat(classifier.classify("TimeoutException: timed out after 30 seconds"))
                .isEqualTo(FailureCategory.TIMEOUT);
    }

    @Test
    void classifiesNetwork() {
        assertThat(classifier.classify("java.net.ConnectException: Connection refused"))
                .isEqualTo(FailureCategory.NETWORK);
    }

    @Test
    void classifiesAssertion() {
        assertThat(classifier.classify("AssertionError expected: <true> but was: <false>"))
                .isEqualTo(FailureCategory.ASSERTION);
    }

    @Test
    void classifiesInfra() {
        assertThat(classifier.classify("SessionNotCreatedException: Unable to create session with Appium"))
                .isEqualTo(FailureCategory.INFRA);
    }

    @Test
    void classifiesAppCrash() {
        assertThat(classifier.classify("FATAL EXCEPTION: main - app crashed"))
                .isEqualTo(FailureCategory.APP_CRASH);
    }

    @Test
    void unknownWhenNoSignal() {
        assertThat(classifier.classify("something odd happened")).isEqualTo(FailureCategory.UNKNOWN);
        assertThat(classifier.classify(null)).isEqualTo(FailureCategory.UNKNOWN);
    }
}
