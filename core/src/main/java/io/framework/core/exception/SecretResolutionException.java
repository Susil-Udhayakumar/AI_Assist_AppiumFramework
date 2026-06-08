package io.framework.core.exception;

/** Thrown when a referenced secret cannot be resolved. Fail-fast at startup. */
public class SecretResolutionException extends FrameworkException {
    public SecretResolutionException(String key, String backend) {
        super("Could not resolve secret '" + key + "' from backend '" + backend + "'");
    }
}
