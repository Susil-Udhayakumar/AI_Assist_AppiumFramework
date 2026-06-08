package io.framework.knowledge;

import io.framework.locators.ElementHealer;
import io.framework.locators.HealRequest;
import io.framework.locators.LocatorCandidate;

import java.util.Optional;

/**
 * Wraps any {@link ElementHealer} with a persistent cache: a heal signature is looked up in
 * {@link HealMemory} first (zero-cost reuse, no model call), and a fresh heal from the delegate
 * is recorded for next time. This is what makes AI heals "solve once, reuse forever".
 */
public final class MemoizingElementHealer implements ElementHealer {

    private final ElementHealer delegate;
    private final HealMemory memory;

    public MemoizingElementHealer(ElementHealer delegate, HealMemory memory) {
        this.delegate = delegate;
        this.memory = memory;
    }

    @Override
    public Optional<LocatorCandidate> heal(HealRequest request) {
        String signature = HealMemory.signature(request.screen(), request.elementName(), request.tried());
        Optional<LocatorCandidate> cached = memory.lookup(signature);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<LocatorCandidate> healed = delegate.heal(request);
        healed.ifPresent(candidate -> memory.record(signature, candidate));
        return healed;
    }
}
