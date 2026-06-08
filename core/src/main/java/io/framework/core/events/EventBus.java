package io.framework.core.events;

import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Decouples lifecycle from reactions (capture/reporting/knowledge listeners).
 * Copy-on-write list makes emit safe while other threads subscribe.
 * A failing listener is isolated so it cannot break the run.
 */
public final class EventBus {

    private final CopyOnWriteArrayList<EventListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(EventListener listener) {
        listeners.add(listener);
    }

    public void emit(TestEvent event, Map<String, Object> payload) {
        EventContext ctx = new EventContext(event, payload);
        for (EventListener l : listeners) {
            try {
                l.on(ctx);
            } catch (RuntimeException e) {
                // isolate listener failures; never break the run for a capture/report error
                System.err.println("[EventBus] listener failed on " + event + ": " + e.getMessage());
            }
        }
    }
}
