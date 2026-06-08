package io.framework.locators;

import org.openqa.selenium.By;

import java.util.Optional;

/**
 * Last-resort hook used by {@link SmartFinder} when all registered candidates miss.
 * Two interchangeable implementations live in higher modules: a deterministic heuristic
 * (ai-heuristic) and an LLM-backed one (ai-llm). Returning empty means "could not heal".
 *
 * Defined here (with the locator types it needs) rather than in ai-spi to keep module
 * dependencies acyclic: ai-heuristic/ai-llm depend on `locators`, not the reverse.
 */
@FunctionalInterface
public interface ElementHealer {
    Optional<By> heal(HealRequest request);
}
