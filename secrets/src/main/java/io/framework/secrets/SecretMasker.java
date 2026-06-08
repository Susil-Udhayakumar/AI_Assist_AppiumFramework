package io.framework.secrets;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of resolved secret values to scrub from any text (logs, screenshots metadata,
 * reports, network captures). Every secret resolved through {@link Secrets} auto-registers
 * here, so observability/reporting modules just call {@link #mask(String)} before emitting.
 * Thread-safe.
 */
public final class SecretMasker {

    private static final String MASK = "****";

    private final Set<String> secrets = ConcurrentHashMap.newKeySet();

    public void register(String secret) {
        if (secret != null && !secret.isBlank()) {
            secrets.add(secret);
        }
    }

    /** Replace every registered secret occurrence in {@code text} with a mask. Null-safe. */
    public String mask(String text) {
        if (text == null) {
            return null;
        }
        String out = text;
        for (String s : secrets) {
            out = out.replace(s, MASK);
        }
        return out;
    }

    public int size() {
        return secrets.size();
    }
}
