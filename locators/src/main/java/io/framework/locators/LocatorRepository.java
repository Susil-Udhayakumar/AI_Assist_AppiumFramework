package io.framework.locators;

import io.framework.core.exception.ConfigException;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central store of element locators, keyed by screen then element name. Each element maps to
 * an ordered list of {@link LocatorCandidate}. Build programmatically or load from YAML:
 *
 * <pre>
 * LoginScreen:
 *   loginButton:
 *     - {strategy: id, value: login}
 *     - {strategy: accessibilityId, value: login_btn}
 * </pre>
 */
public final class LocatorRepository {

    private final Map<String, Map<String, List<LocatorCandidate>>> byScreen = new LinkedHashMap<>();

    public LocatorRepository register(String screen, String element, List<LocatorCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            throw new ConfigException("At least one locator candidate required for "
                    + screen + "." + element);
        }
        byScreen.computeIfAbsent(screen, s -> new LinkedHashMap<>())
                .put(element, List.copyOf(candidates));
        return this;
    }

    public LocatorRepository register(String screen, String element, LocatorCandidate... candidates) {
        return register(screen, element, List.of(candidates));
    }

    public List<LocatorCandidate> candidates(String screen, String element) {
        Map<String, List<LocatorCandidate>> elements = byScreen.get(screen);
        if (elements == null || !elements.containsKey(element)) {
            throw new ConfigException("No locators registered for " + screen + "." + element);
        }
        return elements.get(element);
    }

    @SuppressWarnings("unchecked")
    public static LocatorRepository fromYaml(InputStream in) {
        LocatorRepository repo = new LocatorRepository();
        Object root = new Yaml().load(in);
        if (root == null) {
            return repo;
        }
        if (!(root instanceof Map)) {
            throw new ConfigException("Locator YAML root must be a mapping of screens");
        }
        Map<String, Object> screens = (Map<String, Object>) root;
        for (Map.Entry<String, Object> screen : screens.entrySet()) {
            Map<String, Object> elements = (Map<String, Object>) screen.getValue();
            for (Map.Entry<String, Object> element : elements.entrySet()) {
                List<Map<String, Object>> raw = (List<Map<String, Object>>) element.getValue();
                List<LocatorCandidate> candidates = new ArrayList<>();
                for (Map<String, Object> c : raw) {
                    candidates.add(new LocatorCandidate(
                            Strategy.from(String.valueOf(c.get("strategy"))),
                            String.valueOf(c.get("value"))));
                }
                repo.register(screen.getKey(), element.getKey(), candidates);
            }
        }
        return repo;
    }
}
