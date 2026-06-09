# api — module documentation

## 1. What it is
REST testing for standalone API suites and mid-mobile-flow use (seed data, read an OTP, verify
backend state). `ApiClient` builds requests over a `HttpTransport` (default `JdkHttpTransport`
on the JDK HttpClient — no extra dependency); `ApiResponse` exposes status checks and JSON field
extraction (via SnakeYAML, since JSON is a YAML subset). The transport is an interface so all
request-building/response logic is unit-tested without network I/O.

## 2. How to maintain
- Keep `ApiClient` immutable: `withHeader`/`withBearer` return new clients.
- `ApiResponse.jsonField` does a dotted-path lookup over a parsed object; keep it null-safe and
  returning `Optional`.
- `JdkHttpTransport` is the only networked piece; it is integration-only (not unit-tested).

## 3. How to add new methods
- New HTTP verb/helper: add to `ApiClient` + a `ApiClientTest` case using the fake transport.
- New response assertion (schema validation, header checks): add to `ApiResponse` + tests.
- New transport (e.g. one that injects tracing): implement `HttpTransport`.

## 4. Coding structure
Patterns: client over an injected transport (Strategy), immutable value records (`ApiRequest`,
`ApiResponse`), pure URL joining. Network is isolated in `JdkHttpTransport`; everything else is
deterministic and unit-tested with a capturing fake.

## 5. Token-optimization usage
No AI here. For AI failure-analysis, pass the structured `ApiResponse` (status + parsed fields),
not the raw body, and mask secrets (auth tokens) via the `secrets` masker before logging.

## 6. Examples
```java
ApiClient api = new ApiClient(config.string("apiUrl"), new JdkHttpTransport())
        .withBearer(secrets.get("API_TOKEN"));
ApiResponse r = api.post("/users", "{\"name\":\"alice\"}");
assertThat(r.is2xx()).isTrue();
String otp = api.get("/otp/alice").jsonField("code").map(Object::toString).orElseThrow();
// ... then use otp in the mobile login flow
```
