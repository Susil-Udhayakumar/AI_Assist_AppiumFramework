# locators — module documentation

## 1. What it is
The hybrid locator layer: a central multi-candidate `LocatorRepository`, success ranking
(`LocatorStats`), `SmartFinder` (tries candidates in ranked order, then an optional
`ElementHealer`), and the `ElementHealer` SPI hook. This is the no-AI self-heal: an element
with several candidate locators heals to the next working one automatically, and ranking makes
the best candidate win over time. **It does NOT** drive actions or own the driver — it resolves
`WebElement`s from a given `SearchContext`.

## 2. How to maintain
- `LocatorRepository` is screen → element → ordered candidates; load from YAML or build inline.
- `LocatorStats` is in-memory v1; cross-run persistence comes with the `knowledge` module — keep
  its API (`rank`, `recordSuccess`) stable so the swap is drop-in.
- `SmartFinder` order is the contract: ranked candidates first, healer last, then
  `ElementNotFoundException` with the full tried list. Don't swallow `NoSuchElementException`
  anywhere else.

## 3. How to add new methods
- New locator strategy: add to `Strategy`, map it in `LocatorCandidate.toBy()` and
  `Strategy.from()`, add a `LocatorCandidateTest` case.
- New heal source: implement `ElementHealer` in ai-heuristic/ai-llm (return empty = no heal).

## 4. Coding structure
Patterns: Strategy/SPI (`ElementHealer`), Repository (`LocatorRepository`), and a stable-sort
ranking (`LocatorStats`). `SmartFinder` separates ranked-retry (no AI) from the heal hook
(optional AI). `ElementHealer` lives here (not ai-spi) so module deps stay acyclic.

## 5. Token-optimization usage
Ranked multi-candidate retry resolves most elements with zero AI. The `HealRequest` passes the
tried candidates + (optional) page source so an AI healer gets scoped context, not a blind
prompt — keep page source out of the request when a cheaper signal suffices.

## 6. Examples
```java
LocatorRepository repo = LocatorRepository.fromYaml(stream);   // or .register(...)
SmartFinder finder = new SmartFinder(repo, new LocatorStats(), healerOrNull);
WebElement btn = finder.find(driver, "LoginScreen", "loginButton");
```
