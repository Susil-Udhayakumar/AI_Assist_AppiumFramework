# examples — module documentation

## 1. What it is
The integration module that wires every v1 module together. `Bootstrap` assembles the full
stack from config + the locator repo (resolving driver / devices / healer / reporters via the
ServiceRegistry). `LoginScreen` is a sample hybrid page object. `EndToEndSmokeTest` proves the
whole flow with fakes (no device, runs in CI). `LoginSmokeSample` is the on-device TestNG
template (compiled, not auto-run). **It is the reference for how to use the framework.**

## 2. How to maintain
- `EndToEndSmokeTest` is the cross-module guard: keep it green: it must never need a device.
- `Bootstrap` is the real wiring; if a module's construction changes, update it here — its
  compilation is the integration check.
- Sample on-device tests are named `*Sample` so Surefire skips them; real automated suites a
  team adds would be `*Test` (JUnit) or run via a TestNG suite with the TestNG provider.

## 3. How to add new methods
- New screen: add a page object like `LoginScreen` + its locators under a screen key in
  `locators.yaml`.
- New end-to-end scenario: extend `EndToEndSmokeTest` (fakes) or add an on-device `*Sample`.

## 4. Coding structure
Patterns: Facade/assembly (`Bootstrap` → `FrameworkComponents`), Page Object (`LoginScreen`),
Template Method (on-device sample extends core `BaseTest`). The automated test uses mocks/fakes
so the integration is verified without infrastructure.

## 5. Token-optimization usage
The default wiring uses the heuristic healer (`ai.enabled=false` semantics) — zero AI tokens
until a team swaps in an LLM healer by classpath/config. Page objects pass scoped element
names, never raw page source, into the find/heal path.

## 6. Examples
```java
FrameworkComponents fw = Bootstrap.assemble(config, LocatorRepository.fromYaml(in), Path.of("reports"));
new LoginScreen(fw.finder(), fw.elementActions()).enterUsername("alice").tapLogin();
```
See `EndToEndSmokeTest` for the full assembled flow and `LoginSmokeSample` for an on-device run.
