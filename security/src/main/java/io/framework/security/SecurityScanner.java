package io.framework.security;

import java.util.List;

/**
 * SPI for a security scanner. The built-in static scanner lives here; dynamic/runtime scanners
 * (MobSF, OWASP ZAP, Frida) ship as separate jars implementing this interface. Findings are
 * normalized to {@link Finding} so reporting/defect routing is scanner-agnostic.
 *
 * Note: only run against apps you own or are authorized to test.
 */
public interface SecurityScanner {

    String name();

    List<Finding> scan(ScanTarget target);
}
