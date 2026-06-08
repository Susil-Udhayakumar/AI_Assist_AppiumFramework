# AI-Assist Appium Framework

A modular, AI-augmented mobile test automation platform for **Android + iOS** on Appium 2.x.
Every smart capability (element heal, regression selection, failure analysis, visual testing)
has two interchangeable implementations behind one interface — an **LLM-backed** one and a
**deterministic heuristic** one — so the whole framework works fully offline with
`ai.enabled=false`. AI, when enabled, is consulted only after the knowledge store + heuristics,
keeping token usage low.

> Java 17 · Maven (multi-module) · TestNG runner · Appium java-client 9.x

## v1 status — implemented & verified

A thin, runnable slice through the architecture. Every module builds with `mvn test` green
(unit-tested without a device; on-device flow is the `examples` sample).

| Module | Purpose |
|--------|---------|
| `core` | config cascade, per-thread context, SPI registry, event bus, device pool, lifecycle, base classes |
| `secrets` | `SecretResolver` SPI (env), masking, `${secret:...}` resolution |
| `drivers` | `AppSource`, capability builders, `DriverFactory` (real `DriverProvider`) |
| `devices` | `DeviceProvider` SPI, local emulator/simulator discovery, `DevicePool` bridge |
| `locators` | multi-candidate repo, success-ranked smart-find, `ElementHealer` SPI (no-AI self-heal) |
| `ai-heuristic` | deterministic `HeuristicElementHealer` (page-source token match) |
| `actions` | `ActionSupport` platform matrix, gesture geometry, smart-sync waits, element actions |
| `observability` | EventBus capture listener: screenshots + split logs |
| `reporting` | `Reporter` SPI + HTML reporter + fan-out |
| `examples` | wires it all; end-to-end smoke test + on-device sample |

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
