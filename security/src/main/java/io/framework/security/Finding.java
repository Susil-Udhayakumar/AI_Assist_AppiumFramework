package io.framework.security;

/**
 * One normalized security finding. {@code reference} carries a standard id (MASVS / CWE), so
 * findings from any scanner land in a common shape for reporting and defect routing.
 */
public record Finding(String id, String title, Severity severity,
                      String reference, String evidence, String remediation) {
}
