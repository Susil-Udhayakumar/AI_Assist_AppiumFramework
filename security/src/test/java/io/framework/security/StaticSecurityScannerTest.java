package io.framework.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StaticSecurityScannerTest {

    private final StaticSecurityScanner scanner = new StaticSecurityScanner();

    @Test
    void combinesManifestAndSecretFindings() {
        var target = new ScanTarget(
                "<manifest><application android:debuggable=\"true\"/></manifest>",
                "apiKey = \"AKIAIOSFODNN7EXAMPLE\"");

        var ids = scanner.scan(target).stream().map(Finding::id).toList();
        assertThat(ids).contains("MANIFEST_DEBUGGABLE", "SECRET_AWS_ACCESS_KEY");
    }

    @Test
    void nameIsStatic() {
        assertThat(scanner.name()).isEqualTo("static");
    }

    @Test
    void emptyTargetYieldsNoFindings() {
        assertThat(scanner.scan(new ScanTarget(null, null))).isEmpty();
    }
}
