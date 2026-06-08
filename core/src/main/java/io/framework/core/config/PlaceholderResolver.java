package io.framework.core.config;

import io.framework.core.exception.ConfigException;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expands ${prefix:key} placeholders inside strings.
 * The "env" prefix is built in (backed by the source given at construction).
 * Other prefixes (e.g. "secret", "vault") are registered by other modules.
 */
public final class PlaceholderResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([a-zA-Z]+):([^}]+)}");

    private final Map<String, ValueResolver> resolvers = new HashMap<>();

    public PlaceholderResolver(ValueResolver envSource) {
        resolvers.put("env", envSource);
    }

    public void register(String prefix, ValueResolver resolver) {
        resolvers.put(prefix, resolver);
    }

    public String expand(String raw) {
        if (raw == null) return null;
        Matcher m = PLACEHOLDER.matcher(raw);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String prefix = m.group(1);
            String key = m.group(2);
            ValueResolver resolver = resolvers.get(prefix);
            if (resolver == null) {
                throw new ConfigException("No resolver registered for placeholder prefix '" + prefix + "'");
            }
            String value = resolver.resolve(key);
            if (value == null) {
                throw new ConfigException("Could not resolve placeholder ${" + prefix + ":" + key + "}");
            }
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return out.toString();
    }
}
