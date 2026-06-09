package io.framework.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ManifestAnalyzerTest {

    private final ManifestAnalyzer analyzer = new ManifestAnalyzer();

    private List<String> ids(String xml) {
        return analyzer.analyze(xml).stream().map(Finding::id).toList();
    }

    @Test
    void flagsDebuggableBackupAndCleartext() {
        String xml = "<manifest><application android:debuggable=\"true\" "
                + "android:allowBackup=\"true\" android:usesCleartextTraffic=\"true\"/></manifest>";
        assertThat(ids(xml)).contains("MANIFEST_DEBUGGABLE", "MANIFEST_ALLOW_BACKUP", "MANIFEST_CLEARTEXT");
    }

    @Test
    void flagsExportedComponentWithoutPermission() {
        String xml = "<manifest><application>"
                + "<activity android:name=\".Secret\" android:exported=\"true\"/>"
                + "</application></manifest>";
        List<Finding> findings = analyzer.analyze(xml);
        assertThat(findings).extracting(Finding::id).contains("MANIFEST_EXPORTED_ACTIVITY");
        assertThat(findings).anyMatch(f -> f.evidence().contains(".Secret"));
    }

    @Test
    void exportedWithPermissionIsNotFlagged() {
        String xml = "<manifest><application>"
                + "<service android:name=\".Svc\" android:exported=\"true\" "
                + "android:permission=\"com.x.PRIV\"/></application></manifest>";
        assertThat(ids(xml)).doesNotContain("MANIFEST_EXPORTED_SERVICE");
    }

    @Test
    void cleanManifestHasNoFindings() {
        String xml = "<manifest><application android:debuggable=\"false\" "
                + "android:allowBackup=\"false\"/></manifest>";
        assertThat(analyzer.analyze(xml)).isEmpty();
    }

    @Test
    void malformedManifestYieldsNoFindings() {
        assertThat(analyzer.analyze("<not xml")).isEmpty();
    }
}
