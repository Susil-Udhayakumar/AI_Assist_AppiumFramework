package io.framework.core.failure;

/**
 * Classifies a failure (stacktrace/message text) into a {@link FailureCategory}. Two
 * interchangeable implementations: a deterministic heuristic (ai-heuristic) and an LLM-backed
 * one (ai-llm). A memoizing wrapper (knowledge) caches results by fingerprint so recurring
 * failures are classified without re-work.
 */
@FunctionalInterface
public interface FailureClassifier {
    FailureCategory classify(String failureText);
}
