package io.framework.ai.llm;

import io.framework.core.failure.FailureCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmFailureClassifierTest {

    @Test
    void parsesBareCategory() {
        var classifier = new LlmFailureClassifier(prompt -> "NETWORK");
        assertThat(classifier.classify("conn refused")).isEqualTo(FailureCategory.NETWORK);
    }

    @Test
    void extractsCategoryFromSentence() {
        var classifier = new LlmFailureClassifier(prompt -> "This looks like a TIMEOUT to me.");
        assertThat(classifier.classify("waited too long")).isEqualTo(FailureCategory.TIMEOUT);
    }

    @Test
    void unknownWhenNoCategoryPresent() {
        var classifier = new LlmFailureClassifier(prompt -> "no idea honestly");
        assertThat(classifier.classify("weird")).isEqualTo(FailureCategory.UNKNOWN);
    }

    @Test
    void promptContainsFailureText() {
        String[] captured = new String[1];
        var classifier = new LlmFailureClassifier(prompt -> {
            captured[0] = prompt;
            return "INFRA";
        });
        classifier.classify("SessionNotCreatedException xyz");
        assertThat(captured[0]).contains("SessionNotCreatedException xyz");
    }
}
