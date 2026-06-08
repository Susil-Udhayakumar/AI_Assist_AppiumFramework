package io.framework.core.events;

@FunctionalInterface
public interface EventListener {
    void on(EventContext ctx);
}
