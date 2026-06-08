package io.framework.locators;

import java.util.Optional;

/**
 * Last-resort hook used by {@link SmartFinder} when all registered candidates miss.
 * Two interchangeable implementations live in higher modules: a deterministic heuristic
 * (ai-heuristic) and an LLM-backed one (ai-llm). Returning empty means "could not heal".
 *
 * Returns a {@link LocatorCandidate} (not a raw By) so a heal is both convertible to a By and
 * serializable — letting the knowledge module cache and reuse heals across runs. Defined here
 * (with the locator types it needs) rather than in ai-spi to keep module deps acyclic.
 */
@FunctionalInterface
public interface ElementHealer {
    Optional<LocatorCandidate> heal(HealRequest request);
}
