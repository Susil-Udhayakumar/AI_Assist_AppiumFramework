package io.framework.ai.llm;

import io.framework.locators.HealRequest;
import io.framework.locators.LocatorCandidate;
import io.framework.locators.Strategy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LlmElementHealerTest {

    private HealRequest request(String pageSource) {
        return new HealRequest("LoginScreen", "loginButton",
                List.of(new LocatorCandidate(Strategy.ID, "login")), pageSource);
    }

    @Test
    void parsesStrategyEqualsValueReply() {
        var healer = new LlmElementHealer(prompt -> "ACCESSIBILITY_ID=Login button");
        Optional<LocatorCandidate> healed = healer.heal(request("<hierarchy/>"));
        assertThat(healed).contains(new LocatorCandidate(Strategy.ACCESSIBILITY_ID, "Login button"));
    }

    @Test
    void tolerantOfLeadingProseLines() {
        var healer = new LlmElementHealer(prompt -> "\n  \nID=com.x:id/login\n");
        assertThat(healer.heal(request("<hierarchy/>")))
                .contains(new LocatorCandidate(Strategy.ID, "com.x:id/login"));
    }

    @Test
    void emptyWhenReplyUnparseable() {
        var healer = new LlmElementHealer(prompt -> "I am not sure, sorry");
        assertThat(healer.heal(request("<hierarchy/>"))).isEmpty();
    }

    @Test
    void doesNotCallModelWithoutPageSource() {
        var healer = new LlmElementHealer(prompt -> {
            throw new AssertionError("model must not be called without page source");
        });
        assertThat(healer.heal(request(""))).isEmpty();
    }

    @Test
    void promptCarriesElementNameAndSource() {
        String[] captured = new String[1];
        var healer = new LlmElementHealer(prompt -> {
            captured[0] = prompt;
            return "ID=x";
        });
        healer.heal(request("<hierarchy id='abc'/>"));
        assertThat(captured[0]).contains("loginButton").contains("<hierarchy id='abc'/>");
    }
}
