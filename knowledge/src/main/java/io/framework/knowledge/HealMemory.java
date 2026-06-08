package io.framework.knowledge;

import io.framework.locators.LocatorCandidate;
import io.framework.locators.Strategy;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Remembers past heals: a signature of (screen, element, the candidates that were tried) maps
 * to the locator that worked. On the next break with the same signature the heal is a
 * deterministic lookup — zero AI calls. This is the token-saver + reliability loop.
 */
public final class HealMemory {

    private final Path file;
    private final Map<String, String> heals = new ConcurrentHashMap<>();

    public HealMemory(Path baseDir) {
        this.file = baseDir.resolve("learned").resolve("heals.yaml");
        YamlStore.load(file).forEach((k, v) -> heals.put(k, String.valueOf(v)));
    }

    public static String signature(String screen, String element, List<LocatorCandidate> tried) {
        String triedKeys = tried.stream().map(LocatorCandidate::key).collect(Collectors.joining(","));
        return Hashes.sha1(screen + "|" + element + "|" + triedKeys);
    }

    public Optional<LocatorCandidate> lookup(String signature) {
        String value = heals.get(signature);
        if (value == null) {
            return Optional.empty();
        }
        int eq = value.indexOf('=');
        if (eq < 0) {
            return Optional.empty();
        }
        return Optional.of(new LocatorCandidate(
                Strategy.valueOf(value.substring(0, eq)), value.substring(eq + 1)));
    }

    public void record(String signature, LocatorCandidate healed) {
        heals.put(signature, healed.key());
        YamlStore.save(file, new LinkedHashMap<>(heals));
    }

    public int size() {
        return heals.size();
    }
}
