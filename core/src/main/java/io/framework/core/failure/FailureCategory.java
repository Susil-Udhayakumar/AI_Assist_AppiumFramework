package io.framework.core.failure;

/** Normalized failure categories used for classification, retry decisions, and defect routing. */
public enum FailureCategory {
    ASSERTION,
    ELEMENT_NOT_FOUND,
    TIMEOUT,
    NETWORK,
    INFRA,
    APP_CRASH,
    UNKNOWN
}
