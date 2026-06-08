package io.framework.core.config;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceholderResolverTest {

    @Test
    void expandsEnvPlaceholderFromProvidedSource() {
        var resolver = new PlaceholderResolver(Map.of("HOST", "example.com")::get);
        assertThat(resolver.expand("https://${env:HOST}/api")).isEqualTo("https://example.com/api");
    }

    @Test
    void leavesPlainStringsUntouched() {
        var resolver = new PlaceholderResolver(k -> null);
        assertThat(resolver.expand("plain")).isEqualTo("plain");
    }

    @Test
    void delegatesNonEnvPrefixToExtraResolver() {
        var resolver = new PlaceholderResolver(k -> null);
        resolver.register("secret", k -> "S3CR3T");
        assertThat(resolver.expand("${secret:PWD}")).isEqualTo("S3CR3T");
    }

    @Test
    void throwsWhenUnresolved() {
        var resolver = new PlaceholderResolver(k -> null);
        assertThatThrownBy(() -> resolver.expand("${env:MISSING}"))
                .hasMessageContaining("MISSING");
    }
}
