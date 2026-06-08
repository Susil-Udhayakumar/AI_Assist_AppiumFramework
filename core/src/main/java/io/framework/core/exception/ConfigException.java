package io.framework.core.exception;

/** Thrown for invalid or unreadable configuration. Fail-fast at startup. */
public class ConfigException extends FrameworkException {
    public ConfigException(String message) { super(message); }
    public ConfigException(String message, Throwable cause) { super(message, cause); }
}
