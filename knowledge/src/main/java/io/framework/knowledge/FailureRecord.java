package io.framework.knowledge;

/** A known failure classification and the defect it is linked to (may be empty). */
public record FailureRecord(String classification, String defectId) {
}
