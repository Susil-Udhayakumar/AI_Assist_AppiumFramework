package io.framework.core.config;

/** Resolves a key to a value for a given placeholder prefix. Returns null if unknown. */
@FunctionalInterface
public interface ValueResolver {
    String resolve(String key);
}
