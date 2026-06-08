package io.framework.core.context;

import io.framework.core.exception.FrameworkException;

/**
 * Holds the current thread's DriverContext. Facade everything reads via current().
 * Each parallel worker sets its own context; clear() must run in teardown to avoid leaks.
 */
public final class ContextManager {

    private static final ThreadLocal<DriverContext> CURRENT = new ThreadLocal<>();

    private ContextManager() { }

    public static void set(DriverContext context) {
        CURRENT.set(context);
    }

    public static DriverContext current() {
        DriverContext ctx = CURRENT.get();
        if (ctx == null) {
            throw new FrameworkException("No DriverContext on thread "
                    + Thread.currentThread().getName() + " (was set() called in setup?)");
        }
        return ctx;
    }

    public static boolean isSet() {
        return CURRENT.get() != null;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
