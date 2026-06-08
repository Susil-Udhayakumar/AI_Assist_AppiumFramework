package io.framework.actions;

import io.framework.core.exception.ConfigException;

import java.time.Duration;

/** Timeout + polling settings for smart-sync waits. */
public record WaitConfig(Duration timeout, Duration poll) {

    public WaitConfig {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new ConfigException("wait timeout must be positive");
        }
        if (poll == null || poll.isNegative() || poll.isZero()) {
            throw new ConfigException("wait poll interval must be positive");
        }
    }

    public static WaitConfig defaults() {
        return new WaitConfig(Duration.ofSeconds(20), Duration.ofMillis(500));
    }
}
