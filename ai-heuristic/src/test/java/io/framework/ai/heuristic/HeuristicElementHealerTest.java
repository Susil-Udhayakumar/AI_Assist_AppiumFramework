package io.framework.ai.heuristic;

import io.framework.locators.HealRequest;
import io.framework.locators.LocatorCandidate;
import io.framework.locators.Strategy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicElementHealerTest {

    private final HeuristicElementHealer healer = new HeuristicElementHealer();

    private HealRequest request(String elementName, String pageSource) {
        return new HealRequest("LoginScreen", elementName, List.of(), pageSource);
    }

    @Test
    void healsByAccessibilityId() {
        String xml = "<hierarchy>"
                + "<android.widget.Button content-desc=\"Login button\"/>"
                + "<android.widget.TextView text=\"Welcome\"/>"
                + "</hierarchy>";

        Optional<LocatorCandidate> healed = healer.heal(request("loginButton", xml));

        assertThat(healed).isPresent();
        assertThat(healed.get().strategy()).isEqualTo(Strategy.ACCESSIBILITY_ID);
        assertThat(healed.get().value()).isEqualTo("Login button");
    }

    @Test
    void healsByResourceIdLocalPart() {
        String xml = "<hierarchy>"
                + "<android.widget.Button resource-id=\"com.x.app:id/login_button\"/>"
                + "</hierarchy>";

        Optional<LocatorCandidate> healed = healer.heal(request("loginButton", xml));

        assertThat(healed).contains(new LocatorCandidate(Strategy.ID, "com.x.app:id/login_button"));
    }

    @Test
    void returnsEmptyWhenNothingMatchesWell() {
        String xml = "<hierarchy>"
                + "<android.widget.ImageView content-desc=\"Settings icon\"/>"
                + "</hierarchy>";

        assertThat(healer.heal(request("loginButton", xml))).isEmpty();
    }

    @Test
    void returnsEmptyWhenNoPageSource() {
        assertThat(healer.heal(request("loginButton", ""))).isEmpty();
    }

    @Test
    void returnsEmptyOnUnparseableSource() {
        assertThat(healer.heal(request("loginButton", "<<<not xml>>>"))).isEmpty();
    }
}
