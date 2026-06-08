package io.framework.core.config;

public record Capture(When screenshots, When video, boolean network, boolean vitals) {
    public enum When { OFF, ON_ASSERTION, ON_FAILURE, ON_ACTION, ALWAYS }
}
