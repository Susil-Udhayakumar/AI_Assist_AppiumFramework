package io.framework.secrets;

import java.util.Optional;

/**
 * SPI for a secret backend. Implementations are discovered via ServiceLoader and selected
 * by {@code name()}. New backends (Vault, AWS, Azure, GCP, file) ship as separate jars; the
 * env-backed default lives here.
 */
public interface SecretResolver {

    /** Backend id used in config and in {@code provider:key} references, e.g. "env", "vault". */
    String name();

    /** Resolve a secret by key, or empty if this backend does not have it. */
    Optional<String> resolve(String key);
}
