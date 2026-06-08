package io.framework.locators;

import io.framework.core.exception.ConfigException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocatorRepositoryTest {

    @Test
    void registersAndReturnsCandidates() {
        var repo = new LocatorRepository()
                .register("Login", "btn",
                        new LocatorCandidate(Strategy.ID, "login"),
                        new LocatorCandidate(Strategy.XPATH, "//Button"));

        assertThat(repo.candidates("Login", "btn"))
                .extracting(LocatorCandidate::key)
                .containsExactly("ID=login", "XPATH=//Button");
    }

    @Test
    void unknownElementFailsFast() {
        var repo = new LocatorRepository();
        assertThatThrownBy(() -> repo.candidates("Login", "missing"))
                .isInstanceOf(ConfigException.class)
                .hasMessageContaining("No locators registered");
    }

    @Test
    void registeringWithNoCandidatesFails() {
        var repo = new LocatorRepository();
        assertThatThrownBy(() -> repo.register("Login", "btn"))
                .isInstanceOf(ConfigException.class);
    }

    @Test
    void loadsFromYaml() {
        String yaml = String.join("\n",
                "LoginScreen:",
                "  loginButton:",
                "    - {strategy: id, value: login}",
                "    - {strategy: accessibilityId, value: login_btn}",
                "");
        var repo = LocatorRepository.fromYaml(
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

        assertThat(repo.candidates("LoginScreen", "loginButton"))
                .extracting(LocatorCandidate::key)
                .containsExactly("ID=login", "ACCESSIBILITY_ID=login_btn");
    }
}
