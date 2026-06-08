package io.framework.ai.heuristic;

import java.util.LinkedHashSet;
import java.util.Set;

/** Splits identifiers into lowercase word tokens (camelCase + separators aware). */
final class Tokenizer {

    private Tokenizer() {
    }

    static Set<String> tokens(String s) {
        Set<String> out = new LinkedHashSet<>();
        if (s == null) {
            return out;
        }
        String spaced = s.replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        for (String part : spaced.toLowerCase().split("[^a-z0-9]+")) {
            if (!part.isBlank()) {
                out.add(part);
            }
        }
        return out;
    }

    /** Jaccard similarity of two token sets (0..1); 0 when either is empty. */
    static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Set<String> inter = new LinkedHashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new LinkedHashSet<>(a);
        union.addAll(b);
        return (double) inter.size() / union.size();
    }
}
