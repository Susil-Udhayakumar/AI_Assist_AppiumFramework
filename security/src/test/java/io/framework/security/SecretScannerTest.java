package io.framework.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretScannerTest {

    private final SecretScanner scanner = new SecretScanner();

    @Test
    void detectsAwsAccessKey() {
        var findings = scanner.scan("String k = \"AKIAIOSFODNN7EXAMPLE\";");
        assertThat(findings).extracting(Finding::id).contains("SECRET_AWS_ACCESS_KEY");
        assertThat(findings.get(0).evidence()).doesNotContain("AKIAIOSFODNN7EXAMPLE"); // redacted
    }

    @Test
    void detectsPrivateKeyBlock() {
        var findings = scanner.scan("-----BEGIN RSA PRIVATE KEY-----\nMIIB...\n-----END RSA PRIVATE KEY-----");
        assertThat(findings).extracting(Finding::id).contains("SECRET_PRIVATE_KEY");
    }

    @Test
    void detectsGenericCredential() {
        var findings = scanner.scan("password = \"hunter2horse\"");
        assertThat(findings).extracting(Finding::id).contains("SECRET_GENERIC_CREDENTIAL");
    }

    @Test
    void cleanTextHasNoFindings() {
        assertThat(scanner.scan("int total = computeSum(items);")).isEmpty();
    }
}
