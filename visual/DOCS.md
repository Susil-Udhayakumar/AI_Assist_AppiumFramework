# visual — module documentation

## 1. What it is
Visual regression: `VisualComparator` does a pure pixel comparison (matched when the fraction of
differing pixels is within tolerance, size mismatch = non-match) and produces a red diff image;
`VisualBaseline` stores baselines as committed PNGs, auto-creating one on first sight and writing
`actual` + `diff` artifacts on a mismatch. v1 compares against a prior-screenshot baseline; the
Figma "design-as-baseline" source (via reference knowledge) is a later wave. Pure JDK — no deps.

## 2. How to maintain
- Keep `VisualComparator` pure (no I/O) so it stays trivially unit-testable.
- Tolerance is a fraction of pixels (0..1); tune per screen and document why.
- Baselines are committed and reviewed — store them per platform/density to avoid false diffs.

## 3. How to add new methods
- Region ignore (mask dynamic areas): pre-paint ignored rectangles before compare, add tests.
- Perceptual/anti-alias tolerance: extend the per-pixel check with a channel threshold + tests.
- New baseline source (Figma export): add a loader that yields a `BufferedImage`, reuse the
  comparator unchanged.

## 4. Coding structure
Patterns: pure function + value object (`VisualComparator` → `VisualResult`), repository
(`VisualBaseline`). I/O (ImageIO) is isolated in `VisualBaseline`; comparison is deterministic
and tested with generated in-memory images.

## 5. Token-optimization usage
No AI in the deterministic path. If an AI visual-diff is added later, gate it behind the pixel
comparator: only escalate ambiguous diffs to a model, and send the cropped diff region, not the
whole screen.

## 6. Examples
```java
BufferedImage shot = ImageIO.read(new ByteArrayInputStream(
        ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
VisualOutcome outcome = new VisualBaseline(Path.of("baselines/android"))
        .compareOrCreate("LoginScreen", shot, 0.01);
assertThat(outcome.isFailure()).isFalse();
```
