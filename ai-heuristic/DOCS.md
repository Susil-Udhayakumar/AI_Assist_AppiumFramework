# ai-heuristic — module documentation

## 1. What it is
The deterministic, offline implementation of locators' `ElementHealer` (and, in later waves,
`SuiteSelector` / `FailureClassifier`). `HeuristicElementHealer` parses the page source and
heals a missing element to the on-screen node whose accessibility-id / resource-id / text best
matches the element name by token similarity. No model, no network — this is the no-AI path
that keeps the framework fully functional with `ai.enabled=false`.

## 2. How to maintain
- `Tokenizer` splits identifiers (camelCase + separators) and scores with Jaccard similarity.
- `HeuristicElementHealer.THRESHOLD` guards against confident-but-wrong heals; tune with care
  and add a test when you change it.
- XML parsing is hardened (external entities disabled) and fails closed: any parse error →
  `Optional.empty()`, never an exception out of `heal`.

## 3. How to add new methods
- New match signal (e.g. class name, bounds): add an `Attr` in `attributes(...)` with its own
  token set + `toBy()` mapping, and a healer test.
- New heuristic engine (SuiteSelector/FailureClassifier): add a class implementing the
  corresponding SPI; keep it deterministic and offline.

## 4. Coding structure
Patterns: Strategy/SPI (implements `ElementHealer`), small pure helpers (`Tokenizer`), and a
scored-candidate selection over parsed DOM nodes. Logic is separated from I/O so it is unit-
tested with sample XML strings; the SmartFinder integration test proves the end-to-end heal.

## 5. Token-optimization usage
This module is the reason AI tokens stay near zero: most heals resolve here deterministically.
When paired with an LLM healer, run heuristic first and only escalate on empty — and pass the
scoped `HealRequest` (tried candidates + page source) rather than re-deriving context.

## 6. Examples
```java
ElementHealer healer = new HeuristicElementHealer();              // no-AI default
SmartFinder finder = new SmartFinder(repo, new LocatorStats(), healer);
```
