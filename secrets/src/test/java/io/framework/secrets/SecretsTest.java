package io.framework.secrets;

import io.framework.core.config.PlaceholderResolver;
import io.framework.core.exception.FrameworkException;
import io.framework.core.exception.SecretResolutionException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretsTest {

    private Secrets secretsWith(SecretMasker masker) {
        var env = new EnvSecretResolver(Map.of("DB_PASS", "s3cr3t", "API", "key1")::get);
        return new Secrets(List.of(env), "env", masker);
    }

    @Test
    void getResolvesViaDefaultProvider() {
        assertThat(secretsWith(new SecretMasker()).get("DB_PASS")).isEqualTo("s3cr3t");
    }

    @Test
    void getResolvesViaExplicitProviderPrefix() {
        assertThat(secretsWith(new SecretMasker()).get("env:API")).isEqualTo("key1");
    }

    @Test
    void resolvedSecretIsRegisteredForMasking() {
        var masker = new SecretMasker();
        var secrets = secretsWith(masker);
        secrets.get("DB_PASS");
        assertThat(masker.mask("token=s3cr3t")).isEqualTo("token=****");
    }

    @Test
    void missingSecretFailsFast() {
        assertThatThrownBy(() -> secretsWith(new SecretMasker()).get("MISSING"))
                .isInstanceOf(SecretResolutionException.class)
                .hasMessageContaining("MISSING");
    }

    @Test
    void unknownProviderFailsFast() {
        assertThatThrownBy(() -> secretsWith(new SecretMasker()).get("vault:X"))
                .isInstanceOf(SecretResolutionException.class)
                .hasMessageContaining("vault");
    }

    @Test
    void unknownDefaultProviderRejectedAtConstruction() {
        var env = new EnvSecretResolver(Map.of("K", "v")::get);
        assertThatThrownBy(() -> new Secrets(List.of(env), "vault", new SecretMasker()))
                .isInstanceOf(FrameworkException.class)
                .hasMessageContaining("vault");
    }

    @Test
    void bridgesIntoCorePlaceholderResolver() {
        var secrets = secretsWith(new SecretMasker());
        var pr = new PlaceholderResolver(k -> null);          // no env source needed here
        pr.register("secret", secrets.placeholderResolver());
        assertThat(pr.expand("password=${secret:DB_PASS}")).isEqualTo("password=s3cr3t");
    }
}
