# knowledge — module documentation

## 1. What it is
The project-local, git-committed learning store — the "self-improving without a model" layer.
`LocatorMemory` (persistent success ranking), `HealMemory` (reuse past heals → zero AI on a
known break), `FailureMemory` (fingerprint + classify recurring failures, link defects), and
`ExecutionHistory` (human-readable `.md` run digests + index). Backed by YAML/Markdown under
`{base}/learned/`. **This is the v1 learned half**; the reference-knowledge half (Figma / PRD /
Confluence ingestion + retrieval index) arrives in a later wave.

## 2. How to maintain
- All stores persist on write and reload on construction — keep `save()` calls after every
  mutation so a crash never loses learning.
- `FailureMemory.fingerprint` normalizes out numbers/hex/whitespace so similar failures group;
  changing it re-buckets history, so add a test when you do.
- Files are meant to be committed and PR-reviewed — keep them small and readable.

## 3. How to add new methods
- New memory (e.g. flakiness scores): add a class following the load-on-construct /
  save-on-write pattern + a `@TempDir` round-trip test.
- Wire `LocatorMemory` into locators' `SmartFinder` by generalizing `LocatorStats` to an
  interface (small change) so ranking persists across runs.
- Wire `HealMemory` into the heal path: check `lookup(signature)` before calling any healer;
  on a fresh heal, `record(...)`.

## 4. Coding structure
Patterns: Repository/persistence (each memory), value objects (`FailureRecord`, `RunDigest`),
a shared `YamlStore` + `Hashes`. Pure file I/O makes every store unit-tested with temp dirs and
cross-instance reload assertions (proving persistence).

## 5. Token-optimization usage
This is the token engine: AI solves once → persisted here → heuristics reuse forever → AI only
on cache-miss. Always consult `HealMemory`/`FailureMemory` before invoking a model, and feed an
AI only the scoped miss, never the whole history.

## 6. Examples
```java
KnowledgeStore k = new KnowledgeStore(Path.of("knowledge"));
k.locators().recordSuccess("LoginScreen", "loginButton", winningCandidate);
String sig = HealMemory.signature(screen, element, triedCandidates);
k.heals().lookup(sig).ifPresentOrElse(use, () -> { var healed = healer.heal(req); healed.ifPresent(h -> k.heals().record(sig, h)); });
k.history().append(new RunDigest(date, platform, suite, total, pass, fail, flaky, ms, failureLines));
```
