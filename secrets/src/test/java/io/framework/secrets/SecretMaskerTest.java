package io.framework.secrets;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SecretMaskerTest {

    @Test
    void masksRegisteredSecretInText() {
        var masker = new SecretMasker();
        masker.register("p@ssw0rd");
        assertThat(masker.mask("logging in with p@ssw0rd now")).isEqualTo("logging in with **** now");
    }

    @Test
    void leavesTextWithoutSecretsUnchanged() {
        var masker = new SecretMasker();
        masker.register("p@ssw0rd");
        assertThat(masker.mask("nothing sensitive here")).isEqualTo("nothing sensitive here");
    }

    @Test
    void ignoresNullAndBlankRegistrations() {
        var masker = new SecretMasker();
        masker.register(null);
        masker.register("   ");
        assertThat(masker.size()).isZero();
    }

    @Test
    void maskIsNullSafe() {
        assertThat(new SecretMasker().mask(null)).isNull();
    }
}
