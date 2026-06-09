package io.framework.security;

/** What a scanner inspects. Fields are optional (null) when not applicable to a given scanner. */
public record ScanTarget(String androidManifestXml, String sourceText) {
}
