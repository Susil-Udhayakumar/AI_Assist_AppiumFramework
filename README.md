# AI-Assist Appium Framework

A modular, AI-augmented mobile test automation platform for **Android + iOS** on Appium 2.x.
Every smart capability (element heal, regression selection, failure analysis, visual testing)
has two interchangeable implementations behind one interface — an **LLM-backed** one and a
**deterministic heuristic** one — so the whole framework works fully offline with
`ai.enabled=false`. AI, when enabled, is consulted only after the knowledge store + heuristics,
keeping token usage low.

> Java 17 · Maven (multi-module) · TestNG runner · Appium java-client 9.x

## Status — implemented & verified

15 modules, all building with `mvn test` green (unit-tested without a device/network; the
on-device flow is the `examples` sample). The whole intelligence layer works **offline by
default** (heuristic), with LLM engines swappable by config.

| Module | Purpose |
|--------|---------|
| `core` | config cascade, per-thread context, SPI registry, event bus, device pool, lifecycle, base classes, `FailureClassifier` SPI, `SmartRetryAnalyzer` |
| `secrets` | `SecretResolver` SPI (env), masking, `${secret:...}` resolution |
| `drivers` | `AppSource`, capability builders, `DriverFactory` (real `DriverProvider`) |
| `devices` | `DeviceProvider` SPI, local emulator/simulator discovery, `DevicePool` bridge |
| `locators` | multi-candidate repo, success-ranked smart-find, `ElementHealer` SPI, `CandidateRanker` |
| `ai-heuristic` | deterministic `HeuristicElementHealer` + `HeuristicFailureClassifier` (no AI) |
| `ai-llm` | LLM-backed `LlmElementHealer` + `LlmFailureClassifier` over a tiny `LlmClient` |
| `actions` | `ActionSupport` platform matrix, gesture geometry, smart-sync waits, element actions |
| `api` | REST `ApiClient` over an injectable transport, JSON response helpers, JDK transport |
| `visual` | pixel `VisualComparator` + `VisualBaseline` (auto-create, diff artifacts) |
| `observability` | EventBus capture listener: screenshots + split logs |
| `reporting` | `Reporter` SPI + HTML reporter + fan-out + Requirement Traceability Matrix |
| `knowledge` | persistent `LocatorMemory`, `HealMemory`, `FailureMemory`, `ExecutionHistory` + memoizing wrappers |
| `security` | `SecurityScanner` SPI, manifest audit + secret scan (MASVS-tagged, redacted) |
| `examples` | wires it all; config-driven AI selection; end-to-end smoke test + on-device sample |

Cross-cutting (in `examples`/`Bootstrap`): config-driven heuristic-vs-LLM selection wrapped in
persistent memory, so the framework **learns across runs with zero AI** (winning locators
promoted, heals + failure classifications solved once then reused) and retries only
classified-transient failures (never assertions).

### Defined as SPI drop-in points (need vendor accounts/infra to implement)
Cloud `DeviceProvider`s (BrowserStack/Sauce/LambdaTest), `DistributionProvider`s
(Play/App Store/TestFlight), test-management/defect providers (TestRail/Jira/...), dynamic
security scanners (MobSF/ZAP/Frida), and real `LlmClient`/`HttpTransport` providers — each is a
thin jar against the existing interface.

## Build & test

```bash
mvn test          # build + run all module unit/integration tests (no device needed)
```

## Run on a device

1. Start an Appium 2.x server and an Android emulator (or iOS simulator).
2. Edit `examples/src/test/resources/locators.yaml` for your app and the capabilities in
   `LoginSmokeSample`.
3. `mvn -pl examples surefire:test -Dtest=LoginSmokeSample -Dappium.server.url=http://127.0.0.1:4723`

## Design

See `docs/superpowers/specs/2026-06-08-appium-framework-design.md` for the full 17-module target
architecture and the roadmap (waves 1–10: cloud devices, full observability, knowledge store,
LLM engines, RTM/dashboards, API + mock server, visual testing, test-management + distribution,
security, and enhancements). Each module carries a `DOCS.md` (what it is, how to maintain, how to
extend, token-optimization, examples).

## Principles

Swap by config + classpath (never recompile) · no-AI parity · self-improving without a model ·
per-worker isolation · narrow dependencies · documented patterns + quality gates.
