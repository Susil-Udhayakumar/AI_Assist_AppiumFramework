package io.framework.core.config;

import io.framework.core.exception.ConfigException;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds an immutable FrameworkConfig from the cascade:
 *   classpath YAML  ->  CLI (-Dkey=val, dotted)  ->  ENV vars (UPPER_SNAKE).
 * Later sources win. String leaves are placeholder-expanded via env source.
 */
public final class ConfigLoader {

    private final String classpathResource;
    private final ValueResolver cliSource;   // dotted-key -> value (e.g. "execution.threads")
    private final ValueResolver envSource;   // UPPER_SNAKE -> value (e.g. "EXECUTION_THREADS")

    public ConfigLoader(String classpathResource, ValueResolver cliSource, ValueResolver envSource) {
        this.classpathResource = classpathResource;
        this.cliSource = cliSource;
        this.envSource = envSource;
    }

    @SuppressWarnings("unchecked")
    public FrameworkConfig load() {
        Map<String, Object> merged = readYaml();
        applyOverrides(merged, "");
        expandPlaceholders(merged, new PlaceholderResolver(envSource));

        String env = str(merged, "env", "local");
        Platform platform = Platform.from(str(merged, "platform", null));

        Map<String, Object> exec = (Map<String, Object>) merged.getOrDefault("execution", Map.of());
        Execution execution = new Execution(
                Execution.Mode.valueOf(str(exec, "mode", "sequential").toUpperCase()),
                Execution.ParallelBy.valueOf(str(exec, "parallelBy", "test").toUpperCase()),
                intval(exec, "threads", 1));

        Map<String, Object> cap = (Map<String, Object>) merged.getOrDefault("capture", Map.of());
        Capture capture = new Capture(
                when(str(cap, "screenshots", "onFailure")),
                when(str(cap, "video", "off")),
                boolval(cap, "network", false),
                boolval(cap, "vitals", false));

        Map<String, Object> rt = (Map<String, Object>) merged.getOrDefault("retry", Map.of());
        RetryPolicy retry = new RetryPolicy(
                boolval(rt, "enabled", false),
                intval(rt, "maxRetries", 0),
                (java.util.List<String>) rt.getOrDefault("retryOn", java.util.List.of()),
                intval(rt, "quarantineAfter", Integer.MAX_VALUE));

        return new FrameworkConfig(env, platform, execution, capture, retry, merged);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readYaml() {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(classpathResource)) {
            if (in == null) throw new ConfigException("Config resource not found: " + classpathResource);
            Object loaded = new Yaml().load(in);
            if (loaded == null) return new LinkedHashMap<>();
            if (!(loaded instanceof Map)) throw new ConfigException("Config root must be a YAML mapping");
            return new LinkedHashMap<>((Map<String, Object>) loaded);
        } catch (ConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new ConfigException("Failed to read config: " + classpathResource, e);
        }
    }

    /** Walk the tree; for each leaf path, apply CLI then ENV override if present. */
    @SuppressWarnings("unchecked")
    private void applyOverrides(Map<String, Object> node, String prefix) {
        for (Map.Entry<String, Object> e : node.entrySet()) {
            String dotted = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
            if (e.getValue() instanceof Map<?, ?> child) {
                applyOverrides((Map<String, Object>) child, dotted);
            } else {
                String cli = cliSource.resolve(dotted);
                String env = envSource.resolve(dotted.replace('.', '_').toUpperCase());
                if (env != null) e.setValue(env);
                else if (cli != null) e.setValue(cli);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void expandPlaceholders(Map<String, Object> node, PlaceholderResolver resolver) {
        for (Map.Entry<String, Object> e : node.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Map<?, ?> child) expandPlaceholders((Map<String, Object>) child, resolver);
            else if (v instanceof String s) e.setValue(resolver.expand(s));
        }
    }

    private static String str(Map<String, Object> m, String k, String def) {
        Object v = m.get(k);
        return v == null ? def : v.toString();
    }
    private static int intval(Map<String, Object> m, String k, int def) {
        Object v = m.get(k);
        return v == null ? def : Integer.parseInt(v.toString());
    }
    private static boolean boolval(Map<String, Object> m, String k, boolean def) {
        Object v = m.get(k);
        return v == null ? def : Boolean.parseBoolean(v.toString());
    }
    private static Capture.When when(String raw) {
        return switch (raw.toLowerCase()) {
            case "off" -> Capture.When.OFF;
            case "onassertion" -> Capture.When.ON_ASSERTION;
            case "onfailure" -> Capture.When.ON_FAILURE;
            case "onaction" -> Capture.When.ON_ACTION;
            case "always" -> Capture.When.ALWAYS;
            default -> throw new ConfigException("Unknown capture value: " + raw);
        };
    }
}
