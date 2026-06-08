package io.framework.ai.llm;

/**
 * Minimal text-completion abstraction over a language model. Real providers (OpenAI, Anthropic,
 * Bedrock, Ollama) are thin drop-in implementations selected by config; the healer/classifier
 * here depend only on this interface, so their prompt/parse logic is unit-tested with a fake.
 */
@FunctionalInterface
public interface LlmClient {
    String complete(String prompt);
}
