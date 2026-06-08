# ai-llm — module documentation

## 1. What it is
LLM-backed implementations of the same SPIs the heuristic module implements:
`LlmElementHealer` (locators `ElementHealer`) and `LlmFailureClassifier` (core
`FailureClassifier`). They depend only on a tiny `LlmClient` text-completion interface, so
prompt-building and reply-parsing are deterministic and unit-tested with a fake client. **Off by
default** — selected by config when `ai.enabled=true`; the heuristic stays the fallback. Real
providers (OpenAI/Anthropic/Bedrock/Ollama) are thin `LlmClient` jars added later.

## 2. How to maintain
- Keep prompts scoped: `LlmElementHealer` truncates page source (`MAX_SOURCE_CHARS`) — never
  send the whole tree. Adjust with a token budget in mind.
- Parsers are tolerant by design (first `STRATEGY=value` line; category-substring match) and
  fail closed (`empty` / `UNKNOWN`). Add a test when you change a parser.
- Never put secrets in prompts — the `secrets` masker should scrub inputs upstream.

## 3. How to add new methods
- New LLM SPI (e.g. `TestGenerator`): add a class taking an `LlmClient`, build a prompt, parse
  the reply tolerantly, unit-test with a fake client.
- New provider: implement `LlmClient` in a separate jar; no change here.

## 4. Coding structure
Patterns: Strategy/SPI (implements `ElementHealer` / `FailureClassifier`), dependency injection
of `LlmClient` for testability, pure static prompt/parse helpers. No network in this module —
that lives in provider jars.

## 5. Token-optimization usage
This is the module to watch for spend. Pair it with the heuristic + knowledge memories: run
heuristic first, consult `HealMemory`/`FailureMemory`, and only call the LLM on a true miss —
then the memoizing wrappers persist the result so the model is never asked twice for the same
thing. Keep prompts scoped (truncated source, tried-locator hints) to minimize input tokens.

## 6. Examples
```java
LlmClient client = myProviderClient;                 // OpenAI/Anthropic/Ollama impl
ElementHealer healer = new MemoizingElementHealer(new LlmElementHealer(client), healMemory);
FailureClassifier classifier =
        new MemoizingFailureClassifier(new LlmFailureClassifier(client), failureMemory);
```
