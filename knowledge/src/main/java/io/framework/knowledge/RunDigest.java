package io.framework.knowledge;

import java.util.List;

/** Inputs for one execution-history digest. failureLines are pre-formatted summary strings. */
public record RunDigest(String date, String platform, String suite,
                        int total, int passed, int failed, int flaky,
                        long durationMs, List<String> failureLines) {
}
