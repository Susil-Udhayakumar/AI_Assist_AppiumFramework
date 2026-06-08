# reporting — module documentation

## 1. What it is
The `Reporter` SPI plus a built-in `SimpleHtmlReporter`, a `Reporters` fan-out, and the result
types (`TestResult`, `TestStatus`, `ResultSummary`). Multiple reporters run from one set of
results. v1 ships the HTML reporter; Extent, Allure, and custom reporters are separate jars
implementing `Reporter` in later waves. **It does NOT** decide test outcomes — it formats the
results a TestNG listener / runner feeds it.

## 2. How to maintain
- `Reporter.report(results, dir)` takes the full result set and writes its format; keep it
  idempotent and side-effect-free beyond the output dir.
- `SimpleHtmlReporter` escapes user/content text — never interpolate raw messages into HTML.
- `ResultSummary.of` is the single place that counts statuses.

## 3. How to add new methods
- New report format: implement `Reporter` in a new jar, register it in
  `META-INF/services/io.framework.reporting.Reporter`, add a unit test that asserts the output.
- New result field (e.g. evidence paths): extend `TestResult` + update `SimpleHtmlReporter`.

## 4. Coding structure
Patterns: Strategy/SPI (`Reporter`), Composite/fan-out (`Reporters`), value objects
(`TestResult`, `ResultSummary`). Pure formatting + file write keeps everything unit-testable
with temp dirs and fake reporters.

## 5. Token-optimization usage
Reports are derived artifacts, not AI inputs. When AI failure-analysis is added, feed it the
structured `TestResult` (status + message), not the rendered HTML — smaller, cleaner context.

## 6. Examples
```java
List<Reporter> reporters = new ServiceRegistry().all(Reporter.class); // html by default
new Reporters(reporters).report(results, Path.of("reports", runId));
```
