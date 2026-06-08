package io.framework.ai.llm;

import io.framework.core.failure.FailureCategory;
import io.framework.core.failure.FailureClassifier;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * LLM-backed {@link FailureClassifier}. Asks the model to pick one {@link FailureCategory} for a
 * failure, and tolerantly extracts the category from the reply (handles a bare token or a short
 * sentence). Falls back to {@link FailureCategory#UNKNOWN} when nothing matches.
 */
public final class LlmFailureClassifier implements FailureClassifier {

    private final LlmClient client;

    public LlmFailureClassifier(LlmClient client) {
        this.client = client;
    }

    @Override
    public FailureCategory classify(String failureText) {
        return parse(client.complete(buildPrompt(failureText)));
    }

    static String buildPrompt(String failureText) {
        String categories = Arrays.stream(FailureCategory.values())
                .map(Enum::name).collect(Collectors.joining(", "));
        return "Classify the mobile test failure below into exactly one of: " + categories + ".\n"
                + "Reply with only the category name.\n\nFAILURE:\n"
                + (failureText == null ? "" : failureText);
    }

    static FailureCategory parse(String reply) {
        if (reply == null) {
            return FailureCategory.UNKNOWN;
        }
        String upper = reply.toUpperCase();
        for (FailureCategory category : FailureCategory.values()) {
            if (category != FailureCategory.UNKNOWN && upper.contains(category.name())) {
                return category;
            }
        }
        return FailureCategory.UNKNOWN;
    }
}
