# AI-Augmented Appium Mobile Test Automation Platform — Design

**Date:** 2026-06-08
**Status:** Approved (brainstorming complete; core module implemented + verified)
**Java:** 17 · **Build:** Maven (multi-module) · **Runner:** TestNG · **Appium:** 2.x (java-client 9.x)

---

## 1. Vision

A modular mobile test automation platform for Android and iOS built on Appium 2.x. Every "smart" capability (element identification, self-healing, regression selection, failure analysis, visual testing, test generation) has **two first-class implementations behind one interface**: an **LLM-backed** engine and a **deterministic heuristic** engine. The heuristic engine is a real, fully offline fallback — not a stub — so the entire platform works with `ai.enabled=false`. AI, when enabled, is called only on cache-miss after the knowledge store and heuristics are consulted, which keeps token usage low.

The platform is a **target architecture of 17 modules**. We do not build all 17 at once. We build a thin **v1 core slice** that runs a real test end to end, then grow outward module by module, each with its own spec → plan → implementation cycle.

### Design principles

- **Swap by config + classpath, never by recompile.** `core` depends on interfaces only; concrete cloud / AI / report / security / secret implementations are discovered at runtime via `ServiceLoader`.
- **No-AI parity.** Anything AI does, a heuristic engine also does. Default is heuristic.
- **Self-improving without a model.** A project-local knowledge store makes the no-AI path more reliable every run and cuts AI tokens when AI is on.
- **Isolation.** Each parallel worker owns an immutable context; no shared mutable state.
- **Narrow dependencies.** Modules depend on the smallest interface they need (e.g. `core` holds the driver as `WebDriver`, not concrete `AppiumDriver`).
- **DSA + patterns deliberately applied**, documented per module, enforced by quality gates.

---

## 2. Architecture — module map

Maven multi-module under a parent POM (BOM + dependency management). `⚡` marks a `ServiceLoader` SPI seam (drop-in providers).

```
appium-framework (parent POM, BOM)
├─ core              driver lifecycle, config, context, parallel engine, event bus, base test/screen
├─ locators          hybrid POM API + multi-candidate locator repo + smart-find
├─ actions           full driver action library (gestures, input, device, app, system, SMS, notifications)
├─ api               REST/API testing (RestAssured) — standalone suites OR mid-flow
├─ drivers           Android/iOS capability builders, AppSource resolution, DriverFactory
├─ devices ⚡         DeviceProvider SPI → local, browserstack, sauce, lambdatest
├─ observability     network logs, video, screenshots, split logs, device vitals, crash, accessibility
├─ visual            visual testing (baseline/compare/diff, design-as-baseline via reference knowledge)
├─ security ⚡        SecurityScanner SPI → MobSF, ZAP, Frida, dependency-scan (MASVS/MASTG + API Top 10)
├─ ai-spi ⚡          ElementHealer, SuiteSelector, FailureClassifier, TestGenerator interfaces
│   ├─ ai-heuristic    deterministic offline implementations (real fallback)
│   └─ ai-llm          LLM implementations (OpenAI/Anthropic/Bedrock/Ollama)
├─ knowledge         learned memory + reference knowledge base + skill sync
├─ reporting ⚡       Reporter SPI → extent, allure, custom
├─ distribution ⚡    DistributionProvider SPI → Play Store, App Store, TestFlight (direct vendor APIs)
├─ testmgmt ⚡        TestManagementProvider + DefectProvider SPI → TestRail/Zephyr/Xray/qTest, Jira/ADO
├─ secrets ⚡         SecretResolver SPI → env, Vault, AWS, Azure, GCP, file
├─ bootstrap-cli     env setup, emulator/simulator + appium server start/stop, ingestion, scaffolding
└─ examples          sample tests proving every feature
```

Plus cross-cutting **conventions** (not modules): per-module `DOCS.md`, code-quality gates (SonarQube, Checkstyle, PMD, SpotBugs, Spotless), CI templates (GitHub Actions, Jenkins, GitLab).

### Dependency flow (acyclic, one direction)

```
examples → core → {locators, actions, api, drivers, devices, observability, visual, security, reporting, ai-spi, knowledge, secrets}
ai-llm / ai-heuristic → ai-spi            (ServiceLoader picks at runtime)
device / report / security / distribution / testmgmt / secret providers → their SPI module
bootstrap-cli → standalone (no test-runtime deps)
```

**Boundary rule:** every subsystem hides behind an interface `core` knows. `core` never imports a concrete provider class.

### SPI seams (hybrid skeleton)

Multi-module skeleton + `ServiceLoader` only where swap matters: `ai-spi`, `devices`, `security`, `reporting`, `distribution`, `testmgmt`, `secrets`. Everything else is plain compile-time modules.

---

## 3. Module responsibilities (summary)

- **core** — config loading, per-thread context + isolation, driver lifecycle orchestration, SPI registry, event bus, parallel device allocation, base test/screen. Knows interfaces only. **Implemented + verified (28 tests).** Deep spec in §7.
- **locators** — hybrid: typed Screen/Page API over a central multi-candidate locator repo; smart-find ranks candidates by past success (`knowledge`); miss → `ElementHealer`. Enables no-AI heal and AI heal.
- **actions** — full Android+iOS action library: input, gestures (W3C + UIAutomator2/XCUITest), waits + global smart-sync, app lifecycle + multi-app/deeplink, device, system UI, SMS/calls, permissions, clipboard, files, biometric. `ActionSupport.isSupported` gates platform-specific actions.
- **api** — RestAssured client; pure API suites and mid-flow use; shares config/reporting/logging/secrets.
- **drivers** — capability builders; `AppSource` (file / url / installed-by-package-or-bundleId); `DriverFactory`; named app refs for multi-app.
- **devices ⚡** — `DeviceProvider` SPI: local, BrowserStack, Sauce, LambdaTest; report capacity to `DevicePool`.
- **observability** — network logs, video, screenshots (off/onAssertion/onFailure/onAction), split logs (appium/test/vitals), vitals, crash/ANR, accessibility; PII-masked; event-bus listeners.
- **visual** — baseline/compare/diff; baselines from prior screenshot OR Figma design (reference knowledge); heuristic + AI engines.
- **security ⚡** — `SecurityScanner` SPI; OWASP MASVS/MASTG + API Top 10; static (MobSF/semgrep/SCA), dynamic (TLS/pinning/storage/session/deeplink/WebView), runtime (Frida), API (ZAP). Authorized targets only. Findings → reporting + testmgmt + knowledge; CI gate with baseline-diff.
- **ai-spi (+ ai-heuristic, ai-llm)** — `ElementHealer`, `SuiteSelector`, `FailureClassifier`, `TestGenerator`. Heuristic default (offline, deterministic); LLM optional, selectable per concern.
- **knowledge** — learned memory (locators/heals/failures/history) + reference knowledge base (Figma/requirements/system-design/i2p/confluence/glossary, dual index) + skill sync. Feedback loop: AI solves once → persisted → heuristic reuses → AI only on miss.
- **reporting ⚡** — `Reporter` SPI: Extent, Allure, custom; fan-out from event bus.
- **distribution ⚡** — `DistributionProvider` SPI; direct vendor APIs (Play Developer API v3, App Store Connect, TestFlight); no Fastlane.
- **testmgmt ⚡** — `TestManagementProvider` + `DefectProvider` SPI; pull/track cases, auto script generation, case generation + push-up from reference knowledge, result push-back, defect automation (new→create, known→link). Closed loop PRD→defect.
- **secrets ⚡** — `SecretResolver` SPI: env, Vault, AWS, Azure, GCP, file; `${secret:...}`/`${vault:...}` placeholders auto-resolved; in-memory cache; auto-masking; fail-fast.
- **bootstrap-cli** — env setup (java/maven/appium/ANDROID_HOME/Xcode CLI/doctor/drivers), emulator/simulator + appium server start/stop, reference ingestion, scaffolding, app-crawl autogen, skill sync.
- **examples** — sample tests; CI smoke target; living docs.

### Additional in-scope features (folded into existing modules)

Test data factory (Faker + data-driven), WireMock mock server, DB validation utils, flaky management (retry/quarantine/score), Dockerized Appium grid, performance capture, accessibility audit scoring, localization/i18n, chaos/interrupt testing, optional BDD layer, live dashboard, Slack/Teams/email alerting, framework self-test/doctor, Requirement Traceability Matrix, PII masking, cost/usage dashboard, AI exploratory/monkey testing, auto-heal PR bot, auto-bisect, coverage heatmap, scheduled runs.

---

## 4. Configuration model

Layered override cascade (lowest → highest precedence):

```
packaged defaults → config/<env>.yaml → -Dkey=val (CLI) → ENV vars (secrets/CI)
```

YAML primary (profiles: smoke/regression/nightly). One typed, immutable `FrameworkConfig` loaded once, injected via context. Every key CI-overridable via dotted CLI (`-Dexecution.threads=8`) or ENV (`EXECUTION_THREADS=8`), no rebuild.

```yaml
env: staging
platform: android            # android | ios | both
execution: { mode: parallel, parallelBy: device, threads: 4 }
retry: { enabled: true, maxRetries: 2, retryOn: [infra, network, staleElement], quarantineAfter: 3 }
ai: { enabled: false, providers: { healer: heuristic, suiteSelector: heuristic, classifier: heuristic } }
device: { target: local }    # local | browserstack | sauce | lambdatest
capture: { screenshots: onAction, video: onFailure, network: true, vitals: true }
secrets: { provider: aws, aws: { region: us-east-1, prefix: myapp/staging/ } }
baseUrl: https://staging.app.com
users:
  standard: { username: ${secret:STD_USER}, password: ${secret:STD_PASS} }
```

**Test data**: env profiles + data factory (JSON/CSV/Excel/DB) → `@DataProvider`. **Secrets**: never in repo; resolved at runtime via `secrets`; config references `${secret:...}` only; per-env sets; auto-masked. **Retry**: only classified-transient failures retry (`retryOn`); assertion failures never; retried-but-passed = flaky → quarantine.

---

## 5. Parallel-execution model

Thread-per-device-session, isolated `DriverContext` in a `ThreadLocal`; TestNG drives threading. Five axes (`parallelBy`): test/method, class, suite, platform (Android+iOS concurrently), device (matrix fan-out). `DevicePool` = thread-safe `BlockingQueue<DeviceLease>` → no double-grab, auto port per session, blocks when saturated, respects cloud capacity. Per-thread driver + capture dirs + split logs; `knowledge` writes append-safe; reports merge at end. One worker crash returns its device and fails only that test.

---

## 7. Core-engine deep spec (v1 — IMPLEMENTED + VERIFIED)

### Packages (`io.framework.core`)

```
config/    FrameworkConfig (immutable), ConfigLoader (cascade), PlaceholderResolver, ValueResolver,
           Platform, Execution, Capture, RetryPolicy
context/   DriverContext, ContextManager (ThreadLocal)
spi/       ServiceRegistry (ServiceLoader wrapper + cache)
events/    EventBus, TestEvent, EventContext, EventListener
parallel/  DevicePool (BlockingQueue), DeviceLease
driver/    DriverProvider (SPI)
lifecycle/ DriverLifecycle
base/      BaseTest, BaseScreen
exception/ FrameworkException + ConfigException, DriverInitException, ElementNotFoundException,
           UnsupportedActionException, SecretResolutionException
```

### Key contracts (as implemented)

```java
final class FrameworkConfig { Platform platform(); Execution execution(); Capture capture();
                              RetryPolicy retry(); String env(); String string(String dottedKey); }

final class DriverContext {            // per-thread isolation boundary
    WebDriver driver();                // narrow interface — core only needs WebDriver (quit, etc.)
    AppiumDriver appiumDriver();        // convenience cast for Appium-specific modules
    DeviceLease device();
    FrameworkConfig config();
}
final class ContextManager { static DriverContext current(); static void set(..); static void clear(); }

final class ServiceRegistry { <T> T get(Class<T> spi); <T> List<T> all(Class<T> spi); }

interface DriverProvider { WebDriver create(Platform platform, Capabilities caps); }  // SPI

interface EventListener { void on(EventContext ctx); }
enum TestEvent { RUN_START, SUITE_START, TEST_START, BEFORE_ACTION, AFTER_ACTION,
                 ASSERTION, TEST_FAIL, TEST_PASS, TEST_END, SUITE_END, RUN_END }
```

> **Design note (amended during implementation):** the driver is held in `core` as the narrow
> `org.openqa.selenium.WebDriver` interface rather than the concrete `io.appium.java_client.AppiumDriver`.
> Core only needs the `WebDriver` contract (`quit()`); this keeps the type unit-test-mockable
> (the concrete Appium hierarchy references removed Selenium `html5` classes and cannot be
> instrumented by Mockito's inline maker) and is better Interface-Segregation design. Real driver
> providers return an `AppiumDriver` (which is a `WebDriver`); Appium-specific modules obtain it
> via `DriverContext.appiumDriver()`.

### Patterns
Strategy/SPI (`ServiceRegistry`, `DriverProvider`), Factory (`DriverFactory`, future), Builder (`DriverContext`), Observer (`EventBus`), ThreadLocal context (`ContextManager`), Facade (`ContextManager.current`, `BaseScreen`), Template Method (`BaseTest`).

### DSA
`BlockingQueue<DeviceLease>` (DevicePool), `ConcurrentHashMap` cache (ServiceRegistry), `CopyOnWriteArrayList` (EventBus), success-ranked locator candidate list (locators, later), LRU knowledge cache (knowledge, later).

### Lifecycle / data flow (one test, one thread)

```
@BeforeMethod → DevicePool.acquire → DriverProvider.create → build DriverContext → ContextManager.set
            → EventBus.emit(TEST_START)
@Test → BaseScreen API → smart-find → action → emit(BEFORE/AFTER_ACTION) ; assertion → emit(ASSERTION)
@AfterMethod fail? → emit(TEST_FAIL) → capture → ; emit(TEST_END) → driver.quit → DevicePool.release → clear
RUN_END → merge results → reporting fan-out → knowledge digest + skill sync
```

### Error handling
Typed tree (`FrameworkException` root). Fail-fast at startup (bad config / missing secret / no device); contained at runtime (one test fails, captured, others continue); evidence captured before propagation; retry only on classified-transient.

### Token optimization (enforced by core)
`knowledge`-first resolution before any AI; scoped element context (never page-source) to AI; heuristic default (`ai.enabled=false` → zero tokens); chunk-scoped reference-knowledge retrieval.

---

## 8. Conventions (cross-cutting)

Per-module `DOCS.md` (6 sections: what / maintain / add methods / structure / token-optimization / examples) aggregated by MkDocs Material. Quality: SonarQube gate, Checkstyle + PMD + SpotBugs + Spotless via Maven `verify`, pre-commit hooks, PR checks (build, tests, lint, Sonar, coverage, DOCS.md presence, dependency/secret scan), `CONTRIBUTING.md`. CI templates: GitHub Actions (platform×device matrix), Jenkins (parallel stages), GitLab.

---

## 9. Build order & v1 slice

The 17 modules are the **roadmap**; each gets its own spec → plan → implementation cycle.

**v1 core slice (Android + iOS, local):** `core` (done) + `secrets` (env) + `drivers` (Android/iOS + AppSource) + `devices` (local emulator + simulator) + `locators` + `ai-heuristic` (heal) + `actions` + `observability` (screenshots + basic logs) + `reporting` (one reporter) + `bootstrap-cli` (env + emulator/simulator + appium start/stop) + `examples` (one Android + one iOS smoke). Cross-platform `DriverFactory` + `ActionSupport` gating exercised from day one.

**Growth waves (each its own spec):**
1. Cloud device providers + full parallel matrix
2. Full observability (video/network/vitals/crash/a11y) + PII masking + flaky management
3. `knowledge` learned memory + reference ingestion + skill sync
4. `ai-llm` engines + `SuiteSelector` + `FailureClassifier`
5. Reporting (Extent+Allure+custom) + RTM + dashboard + alerting
6. `api` + mock server + data factory + DB validation
7. `visual` testing
8. `testmgmt` + `distribution`
9. `security` (static→dynamic→runtime→API)
10. Enhancements (exploratory, auto-bisect, auto-heal PR bot, coverage heatmap, BDD, scheduled runs, Docker grid, perf, i18n, chaos)

---

## 10. Backlog / non-goals

**Backlog:** version-compat matrix, feature-flag toggling, provider marketplace, multi-tenant config, report localization, cross-framework export.

**Non-goals / constraints:** authorized security testing only; no third-party distribution tooling (direct vendor APIs); no secrets in repo; heuristic-first / AI-optional (must work with `ai.enabled=false`); not desktop/web browser automation (mobile + in-app WebView only); the platform assists authoring, it does not autonomously own test correctness.

---

## 11. Implementation status

| Module | Status |
|--------|--------|
| core | ✅ implemented + verified (28 tests green on JDK 17) |
| secrets | ⏳ next |
| drivers, devices, locators, ai-heuristic, actions, observability, reporting, bootstrap-cli, examples | planned (v1) |
| remaining 6 modules + waves 1–10 | roadmap |
