package io.framework.core.spi;

import io.framework.core.exception.FrameworkException;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thin cached wrapper over ServiceLoader. Resolves the single (or all) implementations
 * of an SPI interface from the classpath. First scan is cached for the registry's lifetime.
 */
public final class ServiceRegistry {

    private final Map<Class<?>, Object> singletons = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> spi) {
        return (T) singletons.computeIfAbsent(spi, this::loadFirst);
    }

    public <T> List<T> all(Class<T> spi) {
        return ServiceLoader.load(spi).stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toList());
    }

    private <T> T loadFirst(Class<T> spi) {
        return ServiceLoader.load(spi).findFirst()
                .orElseThrow(() -> new FrameworkException(
                        "No implementation registered for SPI " + spi.getName()
                                + " (check META-INF/services and classpath)"));
    }
}
