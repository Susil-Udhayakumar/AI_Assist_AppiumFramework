package io.framework.ai.llm;

import io.framework.locators.ElementHealer;
import io.framework.locators.HealRequest;
import io.framework.locators.LocatorCandidate;
import io.framework.locators.Strategy;

import java.util.Optional;

/**
 * LLM-backed {@link ElementHealer}. Builds a scoped prompt (element name + already-tried
 * locators + truncated page source) and parses the model's "STRATEGY=value" reply into a
 * {@link LocatorCandidate}. Off by default; selected by config when AI is enabled. Returns empty
 * when there is no page source to ground on or the reply is unparseable.
 */
public final class LlmElementHealer implements ElementHealer {

    /** Cap the page source sent to the model to keep token usage bounded. */
    static final int MAX_SOURCE_CHARS = 4000;

    private final LlmClient client;

    public LlmElementHealer(LlmClient client) {
        this.client = client;
    }

    @Override
    public Optional<LocatorCandidate> heal(HealRequest request) {
        String source = request.pageSource();
        if (source == null || source.isBlank()) {
            return Optional.empty();
        }
        String reply = client.complete(buildPrompt(request, truncate(source)));
        return parse(reply);
    }

    static String buildPrompt(HealRequest request, String source) {
        String tried = request.tried().stream().map(LocatorCandidate::key).reduce("", (a, b) ->
                a.isEmpty() ? b : a + ", " + b);
        return "You are repairing a broken mobile UI locator.\n"
                + "Element name: " + request.elementName() + "\n"
                + "Screen: " + request.screen() + "\n"
                + "Already tried (all failed): " + tried + "\n"
                + "Reply with the single best locator as STRATEGY=value where STRATEGY is one of "
                + "ID, ACCESSIBILITY_ID, XPATH, CLASS_NAME, NAME, ANDROID_UIAUTOMATOR, "
                + "IOS_PREDICATE, IOS_CLASS_CHAIN. No prose.\n\n"
                + "PAGE SOURCE:\n" + source;
    }

    static Optional<LocatorCandidate> parse(String reply) {
        if (reply == null) {
            return Optional.empty();
        }
        String line = reply.lines().map(String::strip).filter(s -> !s.isEmpty()).findFirst().orElse("");
        int eq = line.indexOf('=');
        if (eq <= 0 || eq == line.length() - 1) {
            return Optional.empty();
        }
        String strategy = line.substring(0, eq).strip();
        String value = line.substring(eq + 1).strip();
        try {
            return Optional.of(new LocatorCandidate(Strategy.from(strategy), value));
        } catch (RuntimeException notAStrategy) {
            return Optional.empty();
        }
    }

    private static String truncate(String s) {
        return s.length() <= MAX_SOURCE_CHARS ? s : s.substring(0, MAX_SOURCE_CHARS);
    }
}
