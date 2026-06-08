# observability — module documentation

## 1. What it is
Capture wired to core's EventBus. `CaptureListener` logs every lifecycle event to a split
`test.log` and takes screenshots per `CapturePolicy` / the `Capture` setting, using a
`Screenshotter`, a per-test `CaptureLayout` directory, and a `SplitLogWriter` (appium / test /
vitals channels). v1 covers screenshots + split logs; video, network, vitals, crash, and
accessibility capture are added as additional listeners in wave 2. **It does NOT** own the
driver — the screenshot source is injected via `ScreenshotProvider`.

## 2. How to maintain
- `CapturePolicy` is the single source of truth for "screenshot or not"; keep it pure and
  table-like.
- `CaptureListener` must never throw out of `on(...)` (the EventBus isolates listeners, but
  failing capture should not abort a run) — keep new capture work defensive.
- `CaptureLayout.sanitize` guards filesystem-unsafe device/test names.

## 3. How to add new methods
- New capture type (video/network/vitals): add a new `EventListener` (or extend
  `CaptureListener`) that reacts to the relevant `TestEvent`s, writing under the same test dir
  and using the right `SplitLogWriter.Channel`.
- New policy trigger: extend `CapturePolicy.shouldScreenshot` + a `CapturePolicyTest` case.

## 4. Coding structure
Patterns: Observer (`CaptureListener` implements core `EventListener`), pure policy
(`CapturePolicy`), value/utility classes for layout, screenshotting, and logging. Driver access
is injected (`ScreenshotProvider`) so everything is unit-tested with temp dirs + a mock
`TakesScreenshot`.

## 5. Token-optimization usage
PII masking (from `secrets`) should wrap log/screenshot-metadata writes before emit so secrets
never reach disk or an AI failure-analysis prompt. Capture only what the policy needs — fewer,
relevant artifacts mean cheaper, more focused AI analysis later.

## 6. Examples
```java
Path dir = new CaptureLayout(Path.of("reports")).testDir(runId, device, testName);
var listener = new CaptureListener(config.capture(), dir, new Screenshotter(),
        () -> Optional.of((TakesScreenshot) ContextManager.current().driver()),
        new SplitLogWriter(dir));
eventBus.subscribe(listener);
```
