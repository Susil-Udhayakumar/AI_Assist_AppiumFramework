package io.framework.secrets;

import io.framework.core.config.ValueResolver;
import io.framework.core.exception.FrameworkException;
import io.framework.core.exception.SecretResolutionException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backend-agnostic facade over the configured {@link SecretResolver}s.
 *
 * Usage:
 *   secrets.get("LOGIN_PASSWORD")     // default provider
 *   secrets.get("vault:app/db#pass")  // explicit provider prefix
 *
 * Behaviour:
 *  - resolves once per reference and caches in memory (never written to disk)
 *  - auto-registers each resolved value with the {@link SecretMasker} so it is scrubbed
 *    from logs/reports/captures
 *  - fail-fast: a missing secret or unknown provider throws {@link SecretResolutionException}
 *  - rotation-safe: values are read at runtime, so a new run picks up rotated secrets
 *
 * Bridge: {@link #placeholderResolver()} plugs into core's PlaceholderResolver so that
 * {@code ${secret:KEY}} placeholders in config are expanded at load time.
 */
public final class Secrets {

    private final Map<String, SecretResolver> byName = new LinkedHashMap<>();
    private final String defaultProvider;
    private final SecretMasker masker;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public Secrets(List<SecretResolver> resolvers, String defaultProvider, SecretMasker masker) {
        for (SecretResolver r : resolvers) {
            byName.put(r.name(), r);
        }
        if (!byName.containsKey(defaultProvider)) {
            throw new FrameworkException("Unknown default secret provider: '" + defaultProvider
                    + "' (available: " + byName.keySet() + ")");
        }
        this.defaultProvider = defaultProvider;
        this.masker = masker;
    }

    /** Resolve a reference: "KEY" (default provider) or "provider:KEY". Cached after first call. */
    public String get(String reference) {
        return cache.computeIfAbsent(reference, this::doResolve);
    }

    private String doResolve(String reference) {
        String provider = defaultProvider;
        String key = reference;
        int idx = reference.indexOf(':');
        if (idx > 0) {
            provider = reference.substring(0, idx);
            key = reference.substring(idx + 1);
        }
        SecretResolver resolver = byName.get(provider);
        if (resolver == null) {
            throw new SecretResolutionException(key, provider);
        }
        final String backend = provider;
        final String secretKey = key;
        String value = resolver.resolve(secretKey)
                .orElseThrow(() -> new SecretResolutionException(secretKey, backend));
        masker.register(value);
        return value;
    }

    /** Adapter for core's PlaceholderResolver: handles the "secret" prefix in {@code ${secret:KEY}}. */
    public ValueResolver placeholderResolver() {
        return this::get;
    }

    public SecretMasker masker() {
        return masker;
    }
}
