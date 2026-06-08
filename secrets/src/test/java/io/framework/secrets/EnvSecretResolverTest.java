package io.framework.secrets;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class EnvSecretResolverTest {

    @Test
    void nameIsEnv() {
        assertThat(new EnvSecretResolver().name()).isEqualTo("env");
    }

    @Test
    void resolvesPresentKeyFromSource() {
        var resolver = new EnvSecretResolver(Map.of("DB_PASS", "s3cr3t")::get);
        assertThat(resolver.resolve("DB_PASS")).contains("s3cr3t");
    }

    @Test
    void emptyWhenKeyAbsent() {
        var resolver = new EnvSecretResolver(Map.of("DB_PASS", "s3cr3t")::get);
        assertThat(resolver.resolve("MISSING")).isEmpty();
    }
}
