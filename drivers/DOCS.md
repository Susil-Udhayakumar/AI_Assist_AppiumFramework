# drivers — module documentation

## 1. What it is
Turns "what app, which platform, which device" into a live Appium session. Provides
`AppSource` (file / url / installed-Android / installed-iOS), `CapabilityBuilder` (W3C caps
for UiAutomator2 / XCUITest), and `DriverFactory` — the real `DriverProvider` implementation
registered via ServiceLoader. **It does NOT** manage device pools, start the Appium server, or
choose which device to run on (that is `devices` + `bootstrap-cli`).

## 2. How to maintain
- `AppSource.validate()` is the fail-fast gate; keep new kinds validated there and applied in
  `applyTo`.
- `CapabilityBuilder` emits W3C caps: standard `platformName`, everything else `appium:`-prefixed.
- `DriverFactory.create` opens a real session, so it is integration-only (needs a running
  Appium server + device). Resolve the server URL via `appium.server.url` / `APPIUM_SERVER_URL`
  / the local default.

## 3. How to add new methods
- New app source kind: add to `AppSource.Kind`, a factory method, a `validate` arm, an `applyTo`
  arm, and an `AppSourceTest` case.
- New platform/automation engine: extend `CapabilityBuilder.build` switch + add a test.
- New capability defaults: add to `CapabilityBuilder` (or pass via the `extra` map).

## 4. Coding structure
Patterns: Builder (`CapabilityBuilder`), Factory + Strategy/SPI (`DriverFactory` implements
core's `DriverProvider`, discovered by ServiceLoader). Capability/AppSource logic is pure and
unit-tested; session creation is isolated in `DriverFactory.create` for integration use.

## 5. Token-optimization usage
No AI here. Keep capabilities minimal and deterministic so sessions are reproducible — stable
sessions mean fewer flaky failures for the AI/heuristic layers to analyze later.

## 6. Examples
```java
AppSource app = AppSource.file("apps/app.apk");                 // or .installedAndroid(pkg, act)
Capabilities caps = CapabilityBuilder.build(
        Platform.ANDROID, "Pixel_7", "emulator-5554", app, Map.of());
WebDriver driver = new DriverFactory().create(Platform.ANDROID, caps);   // needs Appium server
```
