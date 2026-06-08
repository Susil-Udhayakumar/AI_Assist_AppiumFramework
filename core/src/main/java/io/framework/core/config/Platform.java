package io.framework.core.config;

import io.framework.core.exception.ConfigException;

public enum Platform {
    ANDROID, IOS, BOTH;

    public static Platform from(String raw) {
        if (raw == null) throw new ConfigException("platform is required");
        try {
            return Platform.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ConfigException("Unknown platform: " + raw + " (expected android|ios|both)");
        }
    }
}
