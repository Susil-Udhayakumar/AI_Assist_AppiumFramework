# secrets — module documentation

## 1. What it is
Backend-agnostic secret resolution + masking. Provides the `SecretResolver` SPI, an
environment-variable default (`EnvSecretResolver`), the `Secrets` facade (`get`, caching,
fail-fast, masking registration), and `SecretMasker` (scrubs secret values from any text).
Bridges into core's `PlaceholderResolver` so `${secret:KEY}` placeholders in config are
expanded at load time. **It does NOT** read config files itself or decide which provider is
default — the caller wires that from `FrameworkConfig`.

## 2. How to maintain
- `SecretResolver` is the SPI; new backends (Vault/AWS/Azure/GCP/file) are separate jars
  registered via `META-INF/services/io.framework.secrets.SecretResolver`.
- `Secrets` caches per reference in memory; values are never written to disk.
- Every resolved value is registered with `SecretMasker`; keep that call in `doResolve`.
- Fail-fast invariants: unknown default provider → `FrameworkException` at construction;
  missing key or unknown provider at lookup → `SecretResolutionException`.

## 3. How to add new methods
- New backend: implement `SecretResolver` (unique `name()`), add a `META-INF/services` entry
  in the new jar, add a resolver unit test, no change to `Secrets`.
- New reference syntax: extend `Secrets.doResolve` parsing + add a `SecretsTest` case.

## 4. Coding structure
Patterns: Strategy/SPI (`SecretResolver`), Facade (`Secrets`), Adapter (`placeholderResolver()`
bridges to core's `ValueResolver`). DSA: `ConcurrentHashMap` reference cache,
`ConcurrentHashMap.newKeySet()` masker registry. Injectable env source keeps `EnvSecretResolver`
unit-testable without touching the real process environment.

## 5. Token-optimization usage
Secrets are config/runtime values, not AI inputs. The point for token/AI work is that
`SecretMasker` lets observability/AI layers strip secrets from any text *before* it is logged,
reported, or sent to a model — so secrets never inflate or leak into prompts.

## 6. Examples
```java
var masker = new SecretMasker();
var secrets = new Secrets(new ServiceRegistry().all(SecretResolver.class), "env", masker);

String pass = secrets.get("LOGIN_PASSWORD");      // default provider (env)
String key  = secrets.get("vault:app/db#apiKey"); // explicit provider

// wire ${secret:...} into core config loading:
var pr = new PlaceholderResolver(System::getenv);
pr.register("secret", secrets.placeholderResolver());
```
