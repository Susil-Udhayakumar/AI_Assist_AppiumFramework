package io.framework.api;

import org.yaml.snakeyaml.Yaml;

import java.util.Map;
import java.util.Optional;

/**
 * An immutable HTTP response with convenience helpers. JSON parsing reuses SnakeYAML (JSON is a
 * subset of YAML), so no extra dependency is needed for field extraction in tests.
 */
public record ApiResponse(int status, String body, Map<String, String> headers) {

    public ApiResponse {
        headers = Map.copyOf(headers);
    }

    public boolean is2xx() {
        return status >= 200 && status < 300;
    }

    public Object json() {
        return new Yaml().load(body == null ? "" : body);
    }

    /** Dotted-path lookup into a JSON object body, e.g. "user.name" or "id". */
    public Optional<Object> jsonField(String dottedPath) {
        Object node = json();
        for (String part : dottedPath.split("\\.")) {
            if (!(node instanceof Map<?, ?> map)) {
                return Optional.empty();
            }
            node = map.get(part);
            if (node == null) {
                return Optional.empty();
            }
        }
        return Optional.of(node);
    }
}
