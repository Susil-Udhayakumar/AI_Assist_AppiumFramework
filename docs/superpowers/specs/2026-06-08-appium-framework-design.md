# AI-Augmented Appium Mobile Test Automation Platform — Design

**Date:** 2026-06-08
**Status:** Approved (brainstorming complete, ready for implementation planning)
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

Plus cross-cutting **conventions** (not modules): per-module docs, code-quality gates, CI templates.

### Dependency flow (acyclic, one direction)

```
examples → core → {locators, actions, api, drivers, devices, observability, visual, security, reporting, ai-spi, knowledge, secrets}
ai-llm / ai-heuristic → ai-spi            (ServiceLoader picks at runtime)
device / report / security / distribution / testmgmt / secret providers → their SPI module
bootstrap-cli → standalone (no test-runtime deps)
```

**Boundary rule:** every subsystem hides behind an interface `core` knows. `core` never imports a concrete provider class.

### SPI seams (hybrid skeleton: multi-module + ServiceLoader only where swap matters)

`ai-spi`, `devices`, `security`, `reporting`, `distribution`, `testmgmt`, `secrets`. Everything else is plain compile-time modules (no SPI overhead).

---

## 3. Module responsibilities

### 3.1 core
The spine. Owns config loading, per-thread context and isolation, driver lifecycle orchestration, the SPI registry, the event/hook bus, parallel device allocation, and base test/screen classes. Knows interfaces only. **Deep spec in §7.**

### 3.2 locators (hybrid model)
Screen/Page classes expose typed, readable methods (POM API). Locators are **not** single annotations — they live in a central repository (JSON/YAML per screen), each element holding **multiple candidate locators** (id, accessibility-id, xpath, text, …). A smart-find wrapper tries candidates in order; order is ranked by past success from `knowledge`. All candidates missing → `ElementHealer` SPI (heuristic or LLM). This is the hook point that makes both no-AI self-heal (multi-candidate + success ranking) and AI heal possible.

### 3.3 actions (full action library, Android + iOS)
- **Input:** type, clearText, replaceValue, appendText, hideKeyboard, pressKey/keycode, longPressKey
- **Gestures (W3C Actions + UIAutomator2/XCUITest):** tap, doubleTap, longPress, swipe (directional + custom), scroll, scrollToElement, flick, dragAndDrop, pinch, zoom, multi-touch
- **Waits:** explicit, fluent, visibility/clickable/presence/staleness, custom conditions, polling config; global smart-sync auto-wait before every action (kills `Thread.sleep`)
- **App lifecycle:** install, launch, terminate, activate, background, reset, queryAppState, context switch (NATIVE ↔ WEBVIEW); multi-app ops (`activateApp`, `launchAppByDeeplink`, `switchToApp`) for cross-app/deeplink flows on one session
- **Device:** rotate/orientation, lock/unlock, shake, setGeoLocation, toggle wifi/data/airplane, network throttle, battery, time
- **System UI:** open/read/clear notifications, status bar
- **SMS / calls:** read/send SMS, simulate call (Android via adb/emulator; iOS limited → `UnsupportedActionException` where Apple blocks)
- **Permissions:** grant/revoke/reset (Android adb; iOS `simctl`)
- **Misc:** clipboard, push/pull files, deep link, biometric (fingerprint/faceID sim), element screenshot, page source

A capability matrix (`ActionSupport.isSupported(action, platform)`) gates platform-specific actions; AI/heuristic heal never invents unsupported gestures. Every action auto-fires observability hooks and is retry-wrappable.

### 3.4 api
RestAssured-based. Config-driven base URLs, auth (bearer/basic/oauth), request/response specs, JSON/schema validation, POJO mapping. Two uses: pure API suites (TestNG, parallel, own reporting) and mid-flow inside a mobile test (seed data, read OTP, verify backend). Shares config + reporting + logging + secrets backbone with UI.

### 3.5 drivers
Capability builders per platform. **AppSource** abstraction resolves the app under test: `file(path)` (apk/ipa in project), `url(u)` (remote/cloud), `installed(pkg, activity)` / `installed(bundleId)` (attach to installed app). Validates up front, fails fast. `DriverFactory` builds the platform `AppiumDriver` from `AppSource` + caps. Named app refs (`underTest`, `browser`, `email`, …) support multi-app flows.

### 3.6 devices ⚡
`DeviceProvider` SPI: `local`, `browserstack`, `sauce`, `lambdatest`. Providers report capacity to the `DevicePool`. New cloud vendor = new jar.

### 3.7 observability
Network logs, video recording (off/always/onFailure), screenshots (off/onAssertion/onFailure/onAction), **separate** logs per concern (Appium / test execution / device vitals), device vitals (CPU/memory/FPS/battery), crash/ANR capture, accessibility tree capture. Subscribes to the event bus; per-thread capture directories. PII masking applied to all captured artifacts.

### 3.8 visual
Baseline capture, compare, diff. Two baseline sources: prior-screenshot (regression) and **Figma design as source-of-truth** (via reference knowledge) — catches drift from spec, not just from last run. Heuristic (pixel/perceptual-hash + tolerance) and AI (semantic visual diff) behind a common interface.

### 3.9 security ⚡
`SecurityScanner` SPI. Aligned to OWASP MASVS/MASTG and API Security Top 10. **Authorized targets only** (apps you own / are permitted to test).
- **Static (APK/IPA):** manifest audit (exported components, debuggable, allowBackup, cleartext), permission over-ask, hardcoded secrets/keys, cert/signing, obfuscation presence, SCA for vulnerable dependencies (dependency-check/Snyk/Trivy), tools: MobSF/semgrep/apkanalyzer
- **Dynamic (running):** TLS cert pinning + cleartext + MITM resistance (proxy reuses network capture), insecure storage (prefs/sqlite/keystore/keychain/logs leaking PII), session/auth (token in logs, rotation, biometric bypass), deep-link/intent hijacking, exported-component abuse, WebView JS-bridge risks
- **Runtime (Frida, optional):** root/jailbreak detection bypass, anti-tamper/anti-debug, runtime crypto/pinning verification
- **API (ZAP):** OWASP API Top 10 — broken auth, BOLA/IDOR, injection, mass assignment, rate-limit, excessive data exposure — against the same endpoints API tests hit

Findings normalized (severity, MASVS/CWE id, evidence, remediation) → reporting (security section) + testmgmt (auto-file security defects) + knowledge (dedupe, track over time). CI security gate with baseline-diff.

### 3.10 ai-spi (+ ai-heuristic, ai-llm)
Interfaces: `ElementHealer`, `SuiteSelector`, `FailureClassifier`, `TestGenerator`.
- **ai-heuristic** (default, offline, deterministic): multi-candidate + success-ranked heal; git-diff static analysis + history weighting for suite selection; rule/pattern/fingerprint failure classification; keyword/template test generation.
- **ai-llm:** richer reasoning for the same interfaces; selectable per concern (e.g. local model for heal, cloud for failure analysis). Off by default.

### 3.11 knowledge
Project-local, git-committed, team-shared. **Two halves:**

```
knowledge/
├─ learned/                AUTO — written by runs
│   ├─ locators/<screen>.json     candidates + win-stats (smart-find ranking source)
│   ├─ heals/heal-log.json        broken→healed map (deterministic reuse, zero AI on hit)
│   ├─ failures/signatures.json   fingerprint→classification + linked defect
│   ├─ history/<date>-run.md      human-readable compressed run digest
│   └─ history/index.json         queryable trends, flakiness scores
└─ reference/              HUMAN — app-specific context (RAG grounding)
    ├─ design/                    Figma exports + API links
    ├─ requirements/              PRD, acceptance criteria (pdf/docx/md)
    ├─ system-design/             architecture, API contracts, data models
    ├─ i2p/                       intent-to-prototype docs
    ├─ confluence/                synced Confluence pages
    ├─ glossary/                  domain terms, screen/element naming conventions
    └─ index/                     searchable index (embeddings if AI; TF-IDF/keyword if not)
```

Feedback loop (benefits the no-AI path too): AI solves once → answer persisted → heuristic reuses forever → AI only on cache-miss. Reference knowledge grounds element identification (expected labels/components), test/case generation (real intent + assertions), failure analysis (bug vs expected per spec), and visual testing (design baseline). Retrieval is chunk-scoped → token saver.

**Skill sync:** after each run (auto) or on demand (CLI), accumulated learnings (stable locator strategies, top failure patterns, healed-locator map, flaky list) roll into the project's Claude skill/plugin docs → skill becomes project-specific, sharper, fewer tokens. Writes are diff-previewed and git-committed for team review. Toggle: auto / manual / off.

### 3.12 reporting ⚡
`Reporter` SPI: `extent`, `allure`, `custom`. Multiple reporters fan out in parallel from the event bus. Adding a custom report = new jar implementing `Reporter`.

### 3.13 distribution ⚡
`DistributionProvider` SPI, **direct vendor APIs, no Fastlane/third-party**:
- **Play Store** — Google Play Developer API v3, service-account JWT, upload aab/apk, tracks internal/alpha/beta/production, staged rollout, promote between tracks
- **App Store** — App Store Connect API, JWT (.p8), upload build, manage versions, submit, phased release
- **TestFlight** — App Store Connect API, internal/external tester groups, beta review, notify

Creds via `secrets`. New store (Huawei/Amazon) = new jar.

### 3.14 testmgmt ⚡
`TestManagementProvider` + `DefectProvider` SPI. Direct vendor APIs.
- **Providers:** TestRail, Zephyr, Xray, qTest (cases); Jira, Azure DevOps (defects)
- **Pull/track:** fetch manual cases + steps, map to automated tests by `@TmCase("C123")`, coverage dashboard
- **Auto script generation:** manual case steps → runnable skeleton (Screen page-object + TestNG test); heuristic (keyword→action templates) + LLM engines; reuses `knowledge` known-good locators
- **Case generation + push-up:** from reference knowledge (PRD/i2p/Figma/acceptance criteria) → structured cases (Gherkin or TM-native) → push to TM tool, organized into suites, deduped
- **Result push-back:** map results → case-ids, push status + duration + evidence links; auto-create test cycle per run
- **Defect automation:** on failure, `FailureClassifier` decides — NEW → auto-create defect (Jira/ADO) with stacktrace, classification, screenshot, video, device, logs; KNOWN → link existing, no duplicate; reopen on regression

Closed loop: PRD + Figma → generate cases → push to TM → generate scripts → run in CI → push results back → file/link defects → learnings to `knowledge` + skill sync.

### 3.15 secrets ⚡
`SecretResolver` SPI: `EnvSecretResolver`, `HashiCorpVaultResolver`, `AwsSecretsManagerResolver`, `AzureKeyVaultResolver`, `GcpSecretManagerResolver`, `FileSecretResolver` (dev only). Utilities:
- One call, backend-agnostic: `Secrets.get("LOGIN_PASSWORD")` or prefixed `Secrets.get("vault:app/login#apiKey")`
- Auto-resolve `${secret:KEY}` / `${vault:...}` placeholders in any config value at load
- In-memory caching (never written to disk), rotation-safe (resolved at runtime)
- Auto-register every resolved secret with the PII masker → scrubbed from logs/screenshots/video/reports
- Fail-fast on missing secret (names key + backend)

### 3.16 bootstrap-cli
Standalone CLI (no test-runtime deps):
- **Env setup:** install/verify java, maven, appium, ANDROID_HOME, Xcode command-line tools, appium-doctor, appium drivers
- **Device control:** start/stop Android emulators and iOS simulators
- **Appium server:** start/stop (port management)
- **Ingestion:** pull reference knowledge from Figma API, Confluence API, local files (pdf/docx/md/png) → index
- **Scaffolding:** generate new project / screen / test / SPI provider from templates (enforces structure + docs)
- **App-crawl autogen:** crawl screens (page-source walk) → auto-build locator repo + skeleton Screen classes
- **Skill sync:** roll learnings into skill docs

### 3.17 examples
Sample tests exercising every feature; doubles as living documentation and the CI smoke target.

---

## 4. Additional in-scope features

All folded into existing modules (no new architecture):

- **Test data factory** (data module/util in `core`/`api`): Faker + data-driven (JSON/CSV/Excel/DB), env profiles, entity builders
- **Mock/stub server** (WireMock, under `api`): service virtualization for offline/edge-case determinism
- **DB validation utils:** JDBC helpers to verify backend state after UI actions
- **Flaky management** (`knowledge` + `exec`): auto-retry + quarantine + flakiness score; quarantined tests flagged, not build-blocking
- **Dockerized Appium grid:** containerized parallel sessions for CI scale
- **Performance capture** (`observability`): app launch time, screen render, memory/CPU/FPS/jank trends with threshold asserts
- **Accessibility audit scoring** (`observability`/`visual`): WCAG rule checks + per-screen score
- **Localization/i18n testing:** run suite across languages/locales, catch truncation/missing strings
- **Chaos/interrupt testing:** inject call/SMS/network-drop/low-battery mid-test, assert recovery
- **BDD layer** (optional, Cucumber/Gherkin over the Screen API)
- **Live dashboard:** web UI of real-time progress + historic trends from `knowledge` (candidate cowork artifact)
- **Result alerting:** Slack/Teams/email on completion/failure with summary + report link
- **Framework self-test / doctor:** health-check versions, drivers, config sanity
- **Requirement Traceability Matrix (RTM):** requirement → case → script → run → defect, auto-built from the closed loop (extends to security requirements)
- **PII masking:** auto-redact sensitive fields in screenshots/video/logs (compliance/GDPR)
- **Cost/usage dashboard:** cloud device minutes + AI token spend per run/suite
- **AI exploratory / monkey testing:** autonomous crawl to hunt crashes/ANRs
- **Auto-heal PR bot:** healed locator raised as a reviewable PR (not silent)
- **Auto-bisect:** on regression, git-bisect to pin the breaking commit, attach to defect
- **Coverage heatmap:** tested vs untested screens/elements
- **Scheduled runs:** nightly/cron suites

---

## 5. Configuration model

Layered override cascade (lowest → highest precedence):

```
packaged defaults → config/<env>.yaml → -Dkey=val (CLI) → ENV vars (secrets/CI)
```

YAML primary (profiles: `smoke`, `regression`, `nightly` override blocks). One typed, immutable `FrameworkConfig` loaded once and injected via context — no scattered `System.getProperty`. Every key is CI-overridable via dotted CLI args (`-Dexecution.threads=8`) or ENV (`EXECUTION_THREADS=8`) with **no rebuild**.

```yaml
env: staging
platform: android            # android | ios | both
execution:
  mode: parallel             # sequential | parallel
  parallelBy: device         # test | class | suite | platform | device
  threads: 4
retry:
  enabled: true
  maxRetries: 2
  retryOn: [infra, network, staleElement]   # NOT assertion failures
  quarantineAfter: 3
ai:
  enabled: false             # false = heuristic engine everywhere
  providers: { healer: heuristic, suiteSelector: heuristic, classifier: heuristic }
device:
  target: local              # local | browserstack | sauce | lambdatest
capture:
  screenshots: onAction      # off | onAssertion | onFailure | onAction
  video: onFailure           # off | always | onFailure
  network: true
  vitals: true
secrets:
  provider: aws              # env | vault | aws | azure | gcp | file
  aws: { region: us-east-1, prefix: myapp/staging/ }
baseUrl: https://staging.app.com
users:
  standard: { username: ${secret:STD_USER},  password: ${secret:STD_PASS} }
  admin:    { username: ${secret:ADMIN_USER}, password: ${secret:ADMIN_PASS} }
apps:
  underTest: { source: file, path: apps/app.apk, package: com.x.app, activity: .Main }
  browser:   { source: installed, package: com.android.chrome }
```

### Test data & credentials

- **Non-secret data** (URLs, inputs): env profiles + data factory (JSON/CSV/Excel/DB) → TestNG `@DataProvider`. Switch env via `-Denv=staging`.
- **Secrets** (creds, keys): never in repo. Resolved at runtime from env/secret-manager via `secrets`. Config references only (`${secret:...}` / `${vault:...}`). Per-env credential sets. Auto-masked everywhere. Data-driven login pulls user pool from secure source.

### Retry

Smart, not blind: only classified-transient failures retry (`retryOn`); genuine assertion failures do not (don't hide bugs). Each attempt captured separately; self-heal runs on retry. Retried-but-passed = flaky → feeds flakiness score + quarantine.

---

## 6. Parallel-execution model

**Thread-per-device-session, isolated context, no shared mutable state.** Each worker owns a `DriverContext` in a `ThreadLocal`. TestNG drives threading; the framework guarantees isolation.

Five parallel axes (`parallelBy`): **test/method, class, suite, platform** (Android + iOS concurrently), **device** (fan a suite across a device matrix).

- **DevicePool** = thread-safe `BlockingQueue<DeviceLease>`; hands a free device per worker, returns on finish → no double-grab, auto port assignment per session, blocks when matrix saturated, respects cloud capacity.
- **Isolation:** per-thread driver, per-thread capture dirs (`reports/<run>/<device>/<test>/`), per-thread split logs; `knowledge` writes synchronized/append-safe; reports merge at the end.
- **Failure containment:** one worker crash returns its device to the pool and marks the test failed + captured; others continue.

```
Run → load config → resolve device matrix → DevicePool
  → TestNG spins N threads (threads = config)
  → each thread: acquire device → build DriverContext (ThreadLocal) → run test(s) → capture → release device
  → all done → merge results → reporting fan-out → knowledge digest + skill sync
```

---

## 7. Core-engine deep spec (v1 first build)

### Packages

```
core/
├─ config/       FrameworkConfig (immutable), ConfigLoader (cascade), PlaceholderResolver
├─ context/      DriverContext, ContextManager (ThreadLocal), RunContext
├─ spi/          ServiceRegistry (ServiceLoader wrapper + cache)
├─ events/       EventBus, TestEvent, EventListener
├─ lifecycle/    DriverLifecycle, hooks (before/after action/test/suite)
├─ parallel/     DevicePool (blocking), DeviceLease
├─ exec/         TestNG glue: listeners + RetryAnalyzer wiring
├─ base/         BaseTest, BaseScreen
└─ exception/    typed exceptions
```

### Key contracts

```java
final class FrameworkConfig { Platform platform(); Execution execution(); Capture capture(); /* immutable */ }

final class DriverContext {              // per-thread world; THE isolation boundary
    AppiumDriver driver();
    FrameworkConfig config();            // snapshot
    DeviceLease device();
    CaptureSink capture();               // per-thread dirs
    Logs logs();                         // appium/test/vitals split
    Knowledge knowledge();
}
final class ContextManager {             // ThreadLocal<DriverContext>
    static DriverContext current();
}

final class ServiceRegistry {            // ServiceLoader, cached
    <T> T get(Class<T> spi);
    <T> List<T> all(Class<T> spi);
}

interface EventListener { void on(TestEvent e); }
enum TestEvent { RUN_START, SUITE_START, TEST_START, BEFORE_ACTION, AFTER_ACTION,
                 ASSERTION, TEST_FAIL, TEST_PASS, TEST_END, SUITE_END, RUN_END }
```

### Design patterns

Strategy/SPI (every swappable concern), Factory (`DriverFactory`), Builder (config, capabilities, request specs), Observer (`EventBus` — adding a capture type = new listener, zero core change), ThreadLocal context (isolation without threading `driver` through APIs), Facade (`ContextManager.current()`, `Secrets.get()`, `BaseScreen`), Template method (`BaseTest` lifecycle skeleton).

### DSA

- **DevicePool** = `BlockingQueue<DeviceLease>` (thread-safe, blocking, no double-grab)
- **ServiceRegistry** = `Map<Class,Object>` cached after first `ServiceLoader` scan
- **Locator candidates** = ordered list ranked by `knowledge` success-rate (most-likely first)
- **Knowledge lookups** = LRU cache (heal/failure fingerprints) → O(1) hot path
- **EventBus** = copy-on-write listener list (safe under parallel)

### Lifecycle / data flow (one test, one thread)

```
@BeforeMethod
  → DevicePool.acquire(matrix)            (blocks if none free)
  → DriverFactory.create(appSource, caps) (drivers module)
  → build DriverContext → ContextManager.set()
  → EventBus.emit(TEST_START)             (listeners: log dir, video start)
@Test → BaseScreen API → smart-find (locators + knowledge)
  → action (actions) → emit(BEFORE/AFTER_ACTION)   (screenshot/network/vitals)
assertion → emit(ASSERTION)
@AfterMethod
  fail? → emit(TEST_FAIL) → classifier → capture video/source → defect/knowledge write
  → ContextManager.clear() → driver.quit() → DevicePool.release()
RUN_END → merge results → reporting fan-out → knowledge digest + skill sync
```

### Error handling

Typed tree: `FrameworkException` → `ConfigException`, `DriverInitException`, `ElementNotFoundException` (carries tried candidates), `UnsupportedActionException`, `SecretResolutionException`. **Fail-fast at startup** (bad config / missing secret / no device) vs **contained at runtime** (one test fails, captured, others continue). Every runtime failure auto-captures evidence before propagating. No silent catches; retry only on classified-transient failures.

### Testing the core

Unit: config cascade/override, placeholder resolver, DevicePool concurrency (stress acquire/release), ServiceRegistry resolution, EventBus dispatch order. Mock `AppiumDriver` for context/lifecycle tests. Contract tests per SPI (any impl must pass) → swap-safety. Smoke: one real local emulator round-trip in CI.

### Token optimization built into core

`knowledge`-first resolution (heal/classify lookup before any AI) enforced by core; smart-find passes scoped element context (never full page-source) to AI; heuristic default (zero tokens until AI explicitly on); reference-knowledge retrieval chunk-scoped.

---

## 8. Conventions (cross-cutting, not modules)

### Per-module documentation (mandatory `DOCS.md`)

Each module ships a six-section doc: (1) what it is + boundary, (2) how to maintain, (3) how to add new methods (extension recipe), (4) coding structure/patterns, (5) token-optimization usage guidance, (6) examples. A central MkDocs Material site aggregates all module docs + architecture map + guides, buildable and publishable in CI.

### Code quality & best-practice enforcement

- Static analysis: SonarQube/SonarCloud with a quality gate
- Lint/format: Checkstyle + PMD + SpotBugs + Spotless/google-java-format, wired into Maven `verify`
- Pre-commit hooks: format + lint + quick checks before commit
- Auto PR checks (block merge): build, unit+smoke tests, Checkstyle/PMD/SpotBugs, Sonar gate, coverage threshold, javadoc/DOCS.md presence check, dependency/secret scan; bot comments findings inline
- `CONTRIBUTING.md`: branch naming, conventional commits, design-pattern/DSA expectations, "every new public method needs javadoc + test + DOCS.md update"

### CI/CD templates

GitHub Actions (matrix platform × device, bootstrap env, start emulator, run suite, publish artifacts, comment results), Jenkins (declarative `Jenkinsfile`, parallel stages, Allure publish), GitLab CI (bonus). Hook to trigger AI/heuristic `SuiteSelector` on PR diff → run only impacted regression set.

---

## 9. Build order & v1 slice

The 17 modules are the **target roadmap**, each with its own spec → plan → implementation cycle. We do **not** build all at once.

**v1 core slice (first buildable end-to-end test — Android + iOS, local):**
`core` + `locators` + `actions` + `drivers` (Android + iOS capability builders + AppSource) + `devices` (local only — emulator + simulator) + `observability` (screenshots + basic logs) + `reporting` (one reporter) + `ai-heuristic` (heal only) + `secrets` (env resolver) + `bootstrap-cli` (env setup incl. Xcode CLI tools + emulator/simulator + appium start/stop) + `examples` (one Android + one iOS smoke test).

This proves: config cascade, cross-platform driver factory, parallel isolation, SPI registry, event bus, smart-find with heuristic heal, capture on action, one report — a real Android test on an emulator **and** a real iOS test on a simulator, running locally. Platform-specific action gating (`ActionSupport`) is exercised from day one since both OSes are present.

**Growth waves (each its own spec):**
1. Cloud device providers (BrowserStack/Sauce/LambdaTest) + full parallel device matrix (test/class/suite/platform/device axes)
2. Full observability (video, network, vitals, crash, accessibility) + PII masking + flaky management
3. `knowledge` learned memory + reference ingestion + skill sync
4. `ai-llm` engines + `ai-spi` SuiteSelector + FailureClassifier
5. `reporting` (Extent + Allure + custom) + RTM + dashboards + alerting
6. `api` + mock server + data factory + DB validation
7. `visual` testing
8. `testmgmt` (cases, scripts, results, defects) + `distribution` (Play/App Store/TestFlight)
9. `security` (static → dynamic → runtime → API)
10. Remaining enhancements (exploratory, auto-bisect, auto-heal PR bot, coverage heatmap, BDD, scheduled runs)

---

## 10. Backlog / future (captured, not blocking v1)

Framework-version compatibility matrix; feature-flag/A-B toggling support; provider plugin marketplace; multi-tenant/multi-project config; report localization; cross-framework script export. Pulled into scope only when justified.

---

## 11. Constraints & non-goals

- **Authorized security testing only** — apps owned by or permitted to the user.
- **No third-party distribution tooling** (no Fastlane) — direct vendor APIs only.
- **No secrets in repo, ever.**
- **Heuristic-first, AI-optional** — the platform must remain fully functional with `ai.enabled=false`.
- Not a goal: desktop/web browser automation (mobile + in-app webview only); replacing the test author (the platform assists, does not fully autonomously own test correctness).
