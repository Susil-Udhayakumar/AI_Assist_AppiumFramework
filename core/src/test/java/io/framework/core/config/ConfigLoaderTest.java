package io.framework.core.config;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class ConfigLoaderTest {

    private ConfigLoader loaderWith(Map<String, String> cli, Map<String, String> env) {
        return new ConfigLoader(
                "config/test.yaml",        // classpath resource
                cli::get,                  // CLI override source (dotted keys)
                env::get);                 // ENV override source (UPPER_SNAKE keys)
    }

    @Test
    void loadsYamlValues() {
        var cfg = loaderWith(Map.of(), Map.of("TEST_HOST", "example.com")).load();
        assertThat(cfg.platform()).isEqualTo(Platform.ANDROID);
        assertThat(cfg.execution().threads()).isEqualTo(2);
        assertThat(cfg.execution().parallelBy()).isEqualTo(Execution.ParallelBy.DEVICE);
        assertThat(cfg.capture().screenshots()).isEqualTo(Capture.When.ON_ACTION);
        assertThat(cfg.retry().shouldRetryOn("network")).isTrue();
    }

    @Test
    void cliOverridesYaml() {
        var cfg = loaderWith(Map.of("execution.threads", "8"),
                Map.of("TEST_HOST", "example.com")).load();
        assertThat(cfg.execution().threads()).isEqualTo(8);
    }

    @Test
    void envOverridesCli() {
        var cfg = loaderWith(Map.of("execution.threads", "8"),
                Map.of("TEST_HOST", "example.com", "EXECUTION_THREADS", "16")).load();
        assertThat(cfg.execution().threads()).isEqualTo(16);
    }

    @Test
    void expandsEnvPlaceholderInValues() {
        var cfg = loaderWith(Map.of(), Map.of("TEST_HOST", "example.com")).load();
        assertThat(cfg.string("baseUrl")).isEqualTo("https://example.com/api");
    }
}
