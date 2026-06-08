package io.framework.secrets;

import java.util.Optional;
import java.util.function.Function;

/**
 * Default secret backend: reads from environment variables (the CI-friendly default).
 * The env source is injectable for testing; the public no-arg constructor (required by
 * ServiceLoader) uses the real process environment.
 */
public final class EnvSecretResolver implements SecretResolver {

    private final Function<String, String> env;

    public EnvSecretResolver() {
        this(System::getenv);
    }

    EnvSecretResolver(Function<String, String> env) {
        this.env = env;
    }

    @Override
    public String name() {
        return "env";
    }

    @Override
    public Optional<String> resolve(String key) {
        return Optional.ofNullable(env.apply(key));
    }
}
