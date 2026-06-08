# devices — module documentation

## 1. What it is
Discovers the devices a run can use and turns them into core `DeviceLease`s / a `DevicePool`.
Provides the `DeviceProvider` SPI, a `LocalDeviceProvider` (adb / simctl), and `DevicePools`
(bridge to core's `DevicePool`). **It does NOT** start emulators/simulators or the Appium
server (that is `bootstrap-cli`), nor create drivers (that is `drivers`).

## 2. How to maintain
- `LocalDeviceProvider` keeps parsing pure and static (`parseAdbDevices`,
  `parseBootedSimulators`); the external command is injected via `CommandRunner` so parsing is
  unit-testable without adb/xcrun.
- System ports are assigned sequentially from `BASE_SYSTEM_PORT` to avoid parallel clashes.
- `DevicePools.fromProvider` fails fast when nothing is discovered.

## 3. How to add new methods
- New device backend (BrowserStack/Sauce/LambdaTest): implement `DeviceProvider` in a new jar,
  register it in `META-INF/services/io.framework.devices.DeviceProvider`, return leases with the
  provider's capacity. No change here.
- New local parsing: extend the static parser + add a `LocalDeviceProviderTest` case with sample
  command output.

## 4. Coding structure
Patterns: Strategy/SPI (`DeviceProvider`), Adapter (`DevicePools` → core `DevicePool`), injected
collaborator (`CommandRunner`) for testability. Pure static parsers separate I/O from logic.

## 5. Token-optimization usage
No AI here. Accurate discovery keeps the device matrix correct so parallel runs don't waste
sessions — fewer wasted/timed-out sessions means less noise for later analysis layers.

## 6. Examples
```java
DeviceProvider provider = new ServiceRegistry().get(DeviceProvider.class); // "local" by default
DevicePool pool = DevicePools.fromProvider(provider, Platform.ANDROID);
// pool now feeds DriverLifecycle in core
```
