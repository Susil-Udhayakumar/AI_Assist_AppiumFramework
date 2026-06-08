package io.framework.core.events;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class EventBusTest {

    @Test
    void deliversEventToAllListenersInRegistrationOrder() {
        var bus = new EventBus();
        List<String> calls = new ArrayList<>();
        bus.subscribe(e -> calls.add("a:" + e.event()));
        bus.subscribe(e -> calls.add("b:" + e.event()));

        bus.emit(TestEvent.TEST_START, Map.of());

        assertThat(calls).containsExactly("a:TEST_START", "b:TEST_START");
    }

    @Test
    void payloadIsAccessibleToListener() {
        var bus = new EventBus();
        var seen = new Object() { Object value; };
        bus.subscribe(e -> seen.value = e.payload().get("name"));

        bus.emit(TestEvent.AFTER_ACTION, Map.of("name", "tap"));

        assertThat(seen.value).isEqualTo("tap");
    }

    @Test
    void oneListenerFailureDoesNotStopOthers() {
        var bus = new EventBus();
        List<String> calls = new ArrayList<>();
        bus.subscribe(e -> { throw new RuntimeException("boom"); });
        bus.subscribe(e -> calls.add("reached"));

        bus.emit(TestEvent.RUN_END, Map.of());

        assertThat(calls).containsExactly("reached");
    }
}
