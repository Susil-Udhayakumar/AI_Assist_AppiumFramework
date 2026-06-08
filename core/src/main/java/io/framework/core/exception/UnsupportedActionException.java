package io.framework.core.exception;

/** Thrown when an action is not supported on the active platform. */
public class UnsupportedActionException extends FrameworkException {
    public UnsupportedActionException(String action, String platform) {
        super("Action '" + action + "' is not supported on platform " + platform);
    }
}
