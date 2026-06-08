package io.framework.core.config;

import io.framework.core.exception.ConfigException;

import java.util.Map;
import java.util.Optional;

/**
 * Immutable, typed view over the merged configuration map.
 * Built once by ConfigLoader and injected via context. No System.getProperty elsewhere.
 */
public final class FrameworkConfig {

    private final String env;
    private final Platform platform;
    private final Execution execution;
    private final Capture capture;
    private final RetryPolicy retry;
    private final Map<String, Object> raw;   // already placeholder-expanded for string leaves

    FrameworkConfig(String env, Platform platform, Execution execution,
                    Capture capture, RetryPolicy retry, Map<String, Object> raw) {
        this.env = env;
        this.platform = platform;
        this.execution = execution;
        this.capture = capture;
        this.retry = retry;
        this.raw = Map.copyOf(raw);
    }

    public String env() { return env; }
    public Platform platform() { return platform; }
    public Execution execution() { return execution; }
    public Capture capture() { return capture; }
    public RetryPolicy retry() { return retry; }

    /** Dotted-path lookup for arbitrary leaf values, e.g. "baseUrl" or "device.target". */
    public String string(String dottedKey) {
        return optional(dottedKey)
                .map(Object::toString)
                .orElseThrow(() -> new ConfigException("Missing config key: " + dottedKey));
    }

    public Optional<Object> optional(String dottedKey) {
        Object node = raw;
        for (String part : dottedKey.split("\\.")) {
            if (!(node instanceof Map<?, ?> m)) return Optional.empty();
            node = m.get(part);
            if (node == null) return Optional.empty();
        }
        return Optional.of(node);
    }
}
