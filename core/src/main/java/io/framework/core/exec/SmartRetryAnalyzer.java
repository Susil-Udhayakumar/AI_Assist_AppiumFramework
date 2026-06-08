package io.framework.core.exec;

import io.framework.core.config.RetryPolicy;
import io.framework.core.failure.FailureCategory;
import io.framework.core.failure.FailureClassifier;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TestNG retry analyzer that retries only classified-transient failures, bounded by
 * {@link RetryPolicy}. A genuine {@link FailureCategory#ASSERTION} failure is never retried, so
 * real bugs are not hidden. TestNG constructs analyzers via the no-arg constructor, so the
 * policy + classifier are supplied through {@link #configure} at suite start; unit tests use the
 * explicit constructor.
 */
public final class SmartRetryAnalyzer implements IRetryAnalyzer {

    private static volatile RetryPolicy defaultPolicy =
            new RetryPolicy(false, 0, List.of(), Integer.MAX_VALUE);
    private static volatile FailureClassifier defaultClassifier = text -> FailureCategory.UNKNOWN;

    /** Wire the real policy + classifier once, before the suite runs. */
    public static void configure(RetryPolicy policy, FailureClassifier classifier) {
        defaultPolicy = policy;
        defaultClassifier = classifier;
    }

    private final RetryPolicy policy;
    private final FailureClassifier classifier;
    private final AtomicInteger attempts = new AtomicInteger();

    public SmartRetryAnalyzer() {
        this(defaultPolicy, defaultClassifier);
    }

    public SmartRetryAnalyzer(RetryPolicy policy, FailureClassifier classifier) {
        this.policy = policy;
        this.classifier = classifier;
    }

    @Override
    public boolean retry(ITestResult result) {
        if (!policy.enabled() || attempts.get() >= policy.maxRetries()) {
            return false;
        }
        String text = result.getThrowable() == null ? "" : String.valueOf(result.getThrowable());
        FailureCategory category = classifier.classify(text);
        if (category == FailureCategory.ASSERTION) {
            return false;   // never retry a real assertion failure
        }
        if (!policy.shouldRetryOn(category.name().toLowerCase())) {
            return false;
        }
        attempts.incrementAndGet();
        return true;
    }

    public int attempts() {
        return attempts.get();
    }
}
