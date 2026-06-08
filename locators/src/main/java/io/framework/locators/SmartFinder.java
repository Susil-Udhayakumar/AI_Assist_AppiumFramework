package io.framework.locators;

import io.framework.core.exception.ElementNotFoundException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resolves elements with multi-candidate, success-ranked self-healing:
 *   1. ask {@link LocatorStats} to rank the element's candidates by past success
 *   2. try each candidate in order; first hit wins and is recorded
 *   3. if all miss and an {@link ElementHealer} is configured, ask it for a By and try that
 *   4. otherwise throw {@link ElementNotFoundException} listing everything tried
 *
 * Steps 1–2 are the no-AI self-heal; step 3 is the optional AI/heuristic hook.
 */
public final class SmartFinder {

    private final LocatorRepository repository;
    private final CandidateRanker stats;
    private final ElementHealer healer;   // nullable

    public SmartFinder(LocatorRepository repository, CandidateRanker stats, ElementHealer healer) {
        this.repository = repository;
        this.stats = stats;
        this.healer = healer;
    }

    public WebElement find(SearchContext context, String screen, String element) {
        List<LocatorCandidate> candidates = stats.rank(screen, element,
                repository.candidates(screen, element));
        List<String> tried = new ArrayList<>();

        for (LocatorCandidate candidate : candidates) {
            try {
                WebElement found = context.findElement(candidate.toBy());
                stats.recordSuccess(screen, element, candidate);
                return found;
            } catch (NoSuchElementException miss) {
                tried.add(candidate.key());
            }
        }

        if (healer != null) {
            Optional<LocatorCandidate> healed = healer.heal(
                    new HealRequest(screen, element, candidates, pageSource(context)));
            if (healed.isPresent()) {
                try {
                    return context.findElement(healed.get().toBy());
                } catch (NoSuchElementException miss) {
                    tried.add("healed:" + healed.get().key());
                }
            }
        }

        throw new ElementNotFoundException(screen + "." + element, tried);
    }

    private static String pageSource(SearchContext context) {
        if (context instanceof WebDriver driver) {
            try {
                return driver.getPageSource();
            } catch (RuntimeException e) {
                return "";
            }
        }
        return "";
    }
}
