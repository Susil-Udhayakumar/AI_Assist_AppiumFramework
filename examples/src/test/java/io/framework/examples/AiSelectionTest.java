package io.framework.examples;

import io.framework.ai.heuristic.HeuristicElementHealer;
import io.framework.ai.heuristic.HeuristicFailureClassifier;
import io.framework.ai.llm.LlmClient;
import io.framework.ai.llm.LlmElementHealer;
import io.framework.ai.llm.LlmFailureClassifier;
import io.framework.core.config.ConfigLoader;
import io.framework.core.config.FrameworkConfig;
import io.framework.core.exception.FrameworkException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiSelectionTest {

    private FrameworkConfig config(Map<String, String> overrides) {
        return new ConfigLoader("config/ai-test.yaml", overrides::get, k -> null).load();
    }

    private final LlmClient client = prompt -> "ID=x";

    @Test
    void defaultsToHeuristicWhenAiDisabled() {
        FrameworkConfig cfg = config(Map.of());
        assertThat(AiSelection.pickHealerBase(cfg, client)).isInstanceOf(HeuristicElementHealer.class);
        assertThat(AiSelection.pickClassifierBase(cfg, client)).isInstanceOf(HeuristicFailureClassifier.class);
    }

    @Test
    void usesLlmWhenEnabledAndSelected() {
        FrameworkConfig cfg = config(Map.of(
                "ai.enabled", "true",
                "ai.providers.healer", "llm",
                "ai.providers.classifier", "llm"));
        assertThat(AiSelection.pickHealerBase(cfg, client)).isInstanceOf(LlmElementHealer.class);
        assertThat(AiSelection.pickClassifierBase(cfg, client)).isInstanceOf(LlmFailureClassifier.class);
    }

    @Test
    void heuristicWhenEnabledButProviderIsHeuristic() {
        FrameworkConfig cfg = config(Map.of("ai.enabled", "true"));
        assertThat(AiSelection.pickHealerBase(cfg, client)).isInstanceOf(HeuristicElementHealer.class);
    }

    @Test
    void failsFastWhenLlmSelectedButNoClient() {
        FrameworkConfig cfg = config(Map.of("ai.enabled", "true", "ai.providers.healer", "llm"));
        assertThatThrownBy(() -> AiSelection.pickHealerBase(cfg, null))
                .isInstanceOf(FrameworkException.class)
                .hasMessageContaining("no LlmClient");
    }
}
