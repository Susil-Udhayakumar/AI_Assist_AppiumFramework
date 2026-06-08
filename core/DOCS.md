# core — module documentation

## 1. What it is
The dependency root of the platform. Owns configuration loading, per-thread context
isolation, the SPI registry, the event bus, the device pool, driver lifecycle
orchestration, and the TestNG base classes. It knows interfaces only; concrete drivers,
clouds, reporters, AI, secrets, and security implementations live in other modules and
are discovered via ServiceLoader. **It does NOT** create real Appium drivers, capture
artifacts, or talk to devices itself.

## 2. How to maintain
- `config/` — immutable value types + ConfigLoader cascade (yaml -> CLI -> ENV).
  Invariant: FrameworkConfig is immutable; never mutate after load().
- `context/` — DriverContext is per-thread; ContextManager.clear() MUST run in teardown
  or threads leak contexts. DriverLifecycle.stop() handles this.
- `parallel/` — DevicePool removes a lease from the queue while held; release() returns it.
- `events/` — EventBus isolates listener failures; a broken listener never breaks a run.

## 3. How to add new methods
- New lifecycle event: add to TestEvent enum, emit from DriverLifecycle (or higher),
  add a test in EventBusTest/DriverLifecycleTest.
- New config block: add a record in config/, parse it in ConfigLoader.load(), expose a
  typed getter on FrameworkConfig, cover it in ConfigLoaderTest.
- New SPI: define the interface here (or in a dedicated SPI module), resolve via
  ServiceRegistry.get(YourSpi.class). Add a contract test.

## 4. Coding structure
Patterns: Strategy/SPI (ServiceRegistry + DriverProvider), Builder (DriverContext),
Observer (EventBus), ThreadLocal context (ContextManager), Template Method (BaseTest),
Facade (ContextManager.current, BaseScreen). DSA: BlockingQueue (DevicePool),
ConcurrentHashMap cache (ServiceRegistry), CopyOnWriteArrayList (EventBus).

## 5. Token-optimization usage
core enforces heuristic-first ordering and never sends page-source to AI. Higher modules
must pass scoped element context to AI healers, consult `knowledge` before AI, and keep
`ai.enabled=false` as the default so zero tokens are spent until AI is explicitly enabled.

## 6. Examples
```java
FrameworkConfig cfg = new ConfigLoader("config/staging.yaml",
        k -> System.getProperty(k), k -> System.getenv(k)).load();
ServiceRegistry registry = new ServiceRegistry();
DriverProvider provider = registry.get(DriverProvider.class);
DevicePool pool = new DevicePool(/* leases from devices module */);
EventBus bus = new EventBus();
DriverLifecycle lifecycle = new DriverLifecycle(pool, provider, bus);
```
