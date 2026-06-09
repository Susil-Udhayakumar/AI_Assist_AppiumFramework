package io.framework.security;

import java.util.ArrayList;
import java.util.List;

/** Built-in {@link SecurityScanner}: runs the manifest audit + secret scan over a target. */
public final class StaticSecurityScanner implements SecurityScanner {

    private final ManifestAnalyzer manifestAnalyzer = new ManifestAnalyzer();
    private final SecretScanner secretScanner = new SecretScanner();

    @Override
    public String name() {
        return "static";
    }

    @Override
    public List<Finding> scan(ScanTarget target) {
        List<Finding> findings = new ArrayList<>();
        if (target.androidManifestXml() != null) {
            findings.addAll(manifestAnalyzer.analyze(target.androidManifestXml()));
        }
        if (target.sourceText() != null) {
            findings.addAll(secretScanner.scan(target.sourceText()));
        }
        return findings;
    }
}
