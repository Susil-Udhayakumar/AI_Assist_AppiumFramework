package io.framework.knowledge;

import io.framework.locators.CandidateRanker;
import io.framework.locators.LocatorCandidate;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent, cross-run version of locators' in-memory ranking. Records which candidate won
 * per element and ranks future lookups by past success — so the no-AI self-heal gets more
 * reliable run after run. Backed by a git-friendly YAML file.
 */
public final class LocatorMemory implements CandidateRanker {

    private final Path file;
    private final Map<String, Map<String, Integer>> wins = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public LocatorMemory(Path baseDir) {
        this.file = baseDir.resolve("learned").resolve("locators.yaml");
        for (Map.Entry<String, Object> e : YamlStore.load(file).entrySet()) {
            Map<String, Integer> inner = new ConcurrentHashMap<>();
            ((Map<String, Object>) e.getValue()).forEach((k, v) -> inner.put(k, ((Number) v).intValue()));
            wins.put(e.getKey(), inner);
        }
    }

    public List<LocatorCandidate> rank(String screen, String element, List<LocatorCandidate> candidates) {
        Map<String, Integer> w = wins.getOrDefault(key(screen, element), Map.of());
        List<LocatorCandidate> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingInt((LocatorCandidate c) -> w.getOrDefault(c.key(), 0)).reversed());
        return sorted;
    }

    public void recordSuccess(String screen, String element, LocatorCandidate candidate) {
        wins.computeIfAbsent(key(screen, element), k -> new ConcurrentHashMap<>())
                .merge(candidate.key(), 1, Integer::sum);
        save();
    }

    public int wins(String screen, String element, LocatorCandidate candidate) {
        return wins.getOrDefault(key(screen, element), Map.of()).getOrDefault(candidate.key(), 0);
    }

    private void save() {
        Map<String, Object> out = new LinkedHashMap<>();
        wins.forEach((k, v) -> out.put(k, new LinkedHashMap<>(v)));
        YamlStore.save(file, out);
    }

    private static String key(String screen, String element) {
        return screen + "|" + element;
    }
}
