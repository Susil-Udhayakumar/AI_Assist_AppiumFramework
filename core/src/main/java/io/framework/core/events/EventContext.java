package io.framework.core.events;

import java.util.Map;

/** Immutable event payload delivered to listeners. */
public record EventContext(TestEvent event, Map<String, Object> payload) {
    public EventContext {
        payload = Map.copyOf(payload);
    }
}
