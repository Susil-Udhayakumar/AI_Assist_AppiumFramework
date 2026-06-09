package io.framework.examples;

import io.framework.ai.heuristic.HeuristicElementHealer;
import io.framework.ai.heuristic.HeuristicFailureClassifier;
import io.framework.ai.llm.LlmClient;
import io.framework.ai.llm.LlmElementHealer;
import io.framework.ai.llm.LlmFailureClassifier;
import io.framework.core.config.FrameworkConfig;
import io.framework.core.exception.FrameworkException;
import io.framework.core.failure.FailureClassifier;
import io.framework.knowledge.KnowledgeStore;
import io.framework.knowledge.MemoizingElementHealer;
import io.framework.knowledge.MemoizingFailureClassifier;
import io.framework.locators.ElementHealer;

/**
 * Picks the heuristic or LLM implementation per concern from config and wraps it in the
 * memoizing (persistent) cache. Heuristic is the default; LLM is used only when
 * {@code ai.enabled=true} and the concern's provider is {@code llm} — and then an
 * {@link LlmClient} must be supplied, else it fails fast (never silently downgrades).
 */
public final class AiSelection {

    private AiSelection() {
    }

    public static ElementHealer elementHealer(FrameworkConfig config, KnowledgeStore knowledge,
                                              LlmClient llmClient) {
        return new MemoizingElementHealer(pickHealerBase(config, llmClient), knowledge.heals());
    }

    public static FailureClassifier failureClassifier(FrameworkConfig config, KnowledgeStore knowledge,
                                                      LlmClient llmClient) {
        return new MemoizingFailureClassifier(pickClassifierBase(config, llmClient), knowledge.failures());
    }

    static ElementHealer pickHealerBase(FrameworkConfig config, LlmClient llmClient) {
        if (useLlm(config, "healer")) {
            if (llmClient == null) {
                throw new FrameworkException("ai.providers.healer=llm but no LlmClient was configured");
            }
            return new LlmElementHealer(llmClient);
        }
        return new HeuristicElementHealer();
    }

    static FailureClassifier pickClassifierBase(FrameworkConfig config, LlmClient llmClient) {
        if (useLlm(config, "classifier")) {
            if (llmClient == null) {
                throw new FrameworkException("ai.providers.classifier=llm but no LlmClient was configured");
            }
            return new LlmFailureClassifier(llmClient);
        }
        return new HeuristicFailureClassifier();
    }

    private static boolean useLlm(FrameworkConfig config, String concern) {
        boolean enabled = config.optional("ai.enabled")
                .map(Object::toString).map(Boolean::parseBoolean).orElse(false);
        String provider = config.optional("ai.providers." + concern)
                .map(Object::toString).orElse("heuristic");
        return enabled && provider.equalsIgnoreCase("llm");
    }
}
