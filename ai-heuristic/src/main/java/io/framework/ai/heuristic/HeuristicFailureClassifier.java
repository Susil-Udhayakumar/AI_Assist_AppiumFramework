package io.framework.ai.heuristic;

import io.framework.core.failure.FailureCategory;
import io.framework.core.failure.FailureClassifier;

/**
 * Deterministic, offline failure classifier: keyword/pattern matching over the failure text,
 * most-specific category first. No model — the no-AI classification path.
 */
public final class HeuristicFailureClassifier implements FailureClassifier {

    @Override
    public FailureCategory classify(String failureText) {
        if (failureText == null || failureText.isBlank()) {
            return FailureCategory.UNKNOWN;
        }
        String t = failureText.toLowerCase();

        if (containsAny(t, "nosuchelement", "element not found", "elementnotfound", "could not find")) {
            return FailureCategory.ELEMENT_NOT_FOUND;
        }
        if (containsAny(t, "timeout", "timed out", "wait timed")) {
            return FailureCategory.TIMEOUT;
        }
        if (containsAny(t, "econnrefused", "connection refused", "unreachable",
                "sockettimeout", "unknownhost", "network is")) {
            return FailureCategory.NETWORK;
        }
        if (containsAny(t, "fatal exception", " anr", "app crash", "crashed", "sigsegv")) {
            return FailureCategory.APP_CRASH;
        }
        if (containsAny(t, "sessionnotcreated", "webdriverexception", "session id",
                "unable to create session", "appium", "driverinit")) {
            return FailureCategory.INFRA;
        }
        if (containsAny(t, "assertionerror", "expected:", "but was", "assert that", "assertion failed")) {
            return FailureCategory.ASSERTION;
        }
        return FailureCategory.UNKNOWN;
    }

    private static boolean containsAny(String text, String... needles) {
        for (String n : needles) {
            if (text.contains(n)) {
                return true;
            }
        }
        return false;
    }
}
