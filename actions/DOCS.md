# actions — module documentation

## 1. What it is
The driver action library foundations: `ActionSupport` (platform-capability matrix),
`Gestures` (pure swipe geometry), `Waits` + `WaitConfig` (global smart-sync, no Thread.sleep),
and `ElementActions` (tap/type/clear/replace). Platform-specific actions gate through
`ActionSupport` so unsupported gestures fail fast with `UnsupportedActionException`. **Driver-
bound execution** (issuing the actual W3C gesture, reading SMS, etc.) is performed against a
live `AppiumDriver` and is exercised by the examples module; the geometry/matrix/wait logic
here is fully unit-tested without a device.

## 2. How to maintain
- Add a new action to `Action`, then declare its platform support in `ActionSupport` (`both` /
  `android` / `ios`). Always `require(...)` before performing a gated action.
- Keep gesture math in `Gestures` pure (no driver) so it stays unit-testable.
- `Waits` ignores transient lookup/staleness during polling; don't catch those elsewhere.

## 3. How to add new methods
- New gesture: add geometry to `Gestures` (+ test), gate it in `ActionSupport`, then a driver-
  bound performer that calls `ActionSupport.require` first.
- New element interaction: add to `ElementActions` with a Mockito-verified test.

## 4. Coding structure
Patterns: capability table (`ActionSupport`), pure function + value object (`Gestures` →
`SwipeCoordinates`), small wrapper over Selenium `FluentWait` (`Waits`). I/O-free logic is
separated from driver calls so the bulk is unit-tested.

## 5. Token-optimization usage
Smart-sync waits cut flaky retries (less failure noise for the AI/heuristic layers). The
`ActionSupport` matrix also stops an AI healer from proposing an unsupported action, avoiding
wasted round-trips.

## 6. Examples
```java
ActionSupport.require(Action.READ_SMS, Platform.ANDROID);     // throws on iOS
SwipeCoordinates c = Gestures.swipe(width, height, SwipeDirection.UP);
new ElementActions().replace(field, "text");
new Waits(WaitConfig.defaults()).until(driver, d -> d.findElement(by).isDisplayed());
```
