# security — module documentation

## 1. What it is
Static application-security scanning behind the `SecurityScanner` SPI. The built-in
`StaticSecurityScanner` runs `ManifestAnalyzer` (AndroidManifest risk audit: debuggable,
allowBackup, cleartext traffic, exported components without permission) and `SecretScanner`
(hardcoded AWS/Google keys, private keys, generic credentials). Findings are normalized to
`Finding` with a MASVS/CWE reference. Dynamic/runtime scanners (MobSF, OWASP ZAP, Frida) are
external-tool drop-ins implementing the same SPI in later work. **Only scan apps you own or are
authorized to test.**

## 2. How to maintain
- Keep `SecretScanner` patterns high-confidence to avoid false positives; evidence is redacted so
  findings never leak the full secret.
- `ManifestAnalyzer` reads `android:`-prefixed attributes literally (non-namespace parsing) and
  fails closed (malformed manifest → no findings, never an exception).
- Map every new check to a MASVS/CWE reference for consistent reporting/triage.

## 3. How to add new methods
- New manifest check: add to `ManifestAnalyzer.analyze` + a test with a sample manifest.
- New secret pattern: add a `Rule` in `SecretScanner` + a positive and a clean test.
- New scanner backend (MobSF/ZAP/Frida): implement `SecurityScanner` in a new jar, register it in
  `META-INF/services/io.framework.security.SecurityScanner`.

## 4. Coding structure
Patterns: Strategy/SPI (`SecurityScanner`), composable analyzers, value object (`Finding`). Pure
parsing/regex keeps it unit-tested with inline manifests and source snippets.

## 5. Token-optimization usage
Findings are structured — feed an AI triage step the normalized `Finding` list (id, severity,
reference), never raw scanner dumps, and dedupe against `knowledge` before filing defects.

## 6. Examples
```java
SecurityScanner scanner = new ServiceRegistry().get(SecurityScanner.class); // "static" by default
List<Finding> findings = scanner.scan(new ScanTarget(manifestXml, decompiledSource));
// fail the build on new CRITICAL/HIGH findings, route others to reporting/testmgmt
```
