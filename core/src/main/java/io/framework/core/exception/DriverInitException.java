package io.framework.core.exception;

/** Thrown when an Appium driver cannot be created. */
public class DriverInitException extends FrameworkException {
    public DriverInitException(String message, Throwable cause) { super(message, cause); }
}
