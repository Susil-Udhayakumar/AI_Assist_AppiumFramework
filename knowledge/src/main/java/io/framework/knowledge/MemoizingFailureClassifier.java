package io.framework.knowledge;

import io.framework.core.failure.FailureCategory;
import io.framework.core.failure.FailureClassifier;

import java.util.Optional;

/**
 * Wraps a {@link FailureClassifier} with persistent memory: a failure fingerprint is looked up
 * in {@link FailureMemory} first (known failures classified instantly, no re-work), and a fresh
 * classification is recorded for next time. The store also links failures to defects, so a
 * defect-filing step can dedupe against it.
 */
public final class MemoizingFailureClassifier implements FailureClassifier {

    private final FailureClassifier delegate;
    private final FailureMemory memory;

    public MemoizingFailureClassifier(FailureClassifier delegate, FailureMemory memory) {
        this.delegate = delegate;
        this.memory = memory;
    }

    @Override
    public FailureCategory classify(String failureText) {
        String fingerprint = FailureMemory.fingerprint(failureText);
        Optional<FailureRecord> known = memory.lookup(fingerprint);
        if (known.isPresent()) {
            return parse(known.get().classification());
        }
        FailureCategory category = delegate.classify(failureText);
        memory.record(fingerprint, category.name(), null);
        return category;
    }

    private static FailureCategory parse(String name) {
        try {
            return FailureCategory.valueOf(name);
        } catch (IllegalArgumentException e) {
            return FailureCategory.UNKNOWN;
        }
    }
}
