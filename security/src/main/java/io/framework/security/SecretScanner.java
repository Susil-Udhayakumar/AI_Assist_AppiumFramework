package io.framework.security;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Scans source/config text for hardcoded secrets using conservative high-confidence patterns. */
public final class SecretScanner {

    private record Rule(String id, String title, Severity severity, Pattern pattern) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("SECRET_AWS_ACCESS_KEY", "Hardcoded AWS access key id", Severity.CRITICAL,
                    Pattern.compile("AKIA[0-9A-Z]{16}")),
            new Rule("SECRET_GOOGLE_API_KEY", "Hardcoded Google API key", Severity.HIGH,
                    Pattern.compile("AIza[0-9A-Za-z_\\-]{35}")),
            new Rule("SECRET_PRIVATE_KEY", "Embedded private key", Severity.CRITICAL,
                    Pattern.compile("-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----")),
            new Rule("SECRET_GENERIC_CREDENTIAL", "Hardcoded credential", Severity.HIGH,
                    Pattern.compile("(?i)(?:password|passwd|secret|api[_-]?key|token)\\s*[=:]\\s*[\"']?[A-Za-z0-9_\\-]{8,}")));

    public List<Finding> scan(String text) {
        List<Finding> findings = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return findings;
        }
        Map<String, Boolean> seen = new LinkedHashMap<>();
        for (Rule rule : RULES) {
            Matcher m = rule.pattern().matcher(text);
            while (m.find()) {
                String evidence = redact(m.group());
                String dedupeKey = rule.id() + "|" + evidence;
                if (seen.putIfAbsent(dedupeKey, Boolean.TRUE) == null) {
                    findings.add(new Finding(rule.id(), rule.title(), rule.severity(),
                            "MASVS-STORAGE-1", evidence,
                            "Remove the secret from source; load it at runtime from a secret manager."));
                }
            }
        }
        return findings;
    }

    /** Keep only a hint of the match so the finding itself does not leak the full secret. */
    private static String redact(String match) {
        if (match.length() <= 8) {
            return match.charAt(0) + "***";
        }
        return match.substring(0, 4) + "***" + match.substring(match.length() - 2);
    }
}
