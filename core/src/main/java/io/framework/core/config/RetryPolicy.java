package io.framework.core.config;

import java.util.List;
import java.util.Set;

public record RetryPolicy(boolean enabled, int maxRetries, List<String> retryOn, int quarantineAfter) {
    public RetryPolicy {
        retryOn = List.copyOf(retryOn);
    }
    public boolean shouldRetryOn(String classification) {
        return enabled && Set.copyOf(retryOn).contains(classification);
    }
}
