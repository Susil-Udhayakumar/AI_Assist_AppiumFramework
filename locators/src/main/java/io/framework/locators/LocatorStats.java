package io.framework.locators;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory success ranking for locator candidates. Smart-find tries the most-successful
 * candidate first, so the no-AI self-heal gets more reliable over a run. (Persistent,
 * cross-run ranking arrives with the `knowledge` module; this is the in-process v1.)
 */
public final class LocatorStats {

    private final Map<String, Map<String, Integer>> wins = new ConcurrentHashMap<>();

    /** Candidates sorted by descending past success; ties keep their original order (stable). */
    public List<LocatorCandidate> rank(String screen, String element, List<LocatorCandidate> candidates) {
        Map<String, Integer> w = wins.getOrDefault(key(screen, element), Map.of());
        List<LocatorCandidate> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingInt((LocatorCandidate c) -> w.getOrDefault(c.key(), 0)).reversed());
        return sorted;
    }

    public void recordSuccess(String screen, String element, LocatorCandidate candidate) {
        wins.computeIfAbsent(key(screen, element), k -> new ConcurrentHashMap<>())
                .merge(candidate.key(), 1, Integer::sum);
    }

    public int wins(String screen, String element, LocatorCandidate candidate) {
        return wins.getOrDefault(key(screen, element), Map.of()).getOrDefault(candidate.key(), 0);
    }

    private static String key(String screen, String element) {
        return screen + "|" + element;
    }
}
