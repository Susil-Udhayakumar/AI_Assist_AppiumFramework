package io.framework.core.spi;

import io.framework.core.exception.FrameworkException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceRegistryTest {

    @Test
    void loadsRegisteredImplementation() {
        var registry = new ServiceRegistry();
        Greeter g = registry.get(Greeter.class);
        assertThat(g.greet()).isEqualTo("hello");
    }

    @Test
    void cachesSameInstanceAcrossCalls() {
        var registry = new ServiceRegistry();
        assertThat(registry.get(Greeter.class)).isSameAs(registry.get(Greeter.class));
    }

    @Test
    void throwsWhenNoImplementation() {
        var registry = new ServiceRegistry();
        assertThatThrownBy(() -> registry.get(Runnable.class))
                .isInstanceOf(FrameworkException.class)
                .hasMessageContaining("No implementation");
    }
}
