# devices-browserstack — module documentation

## 1. What it is
A cloud `DeviceProvider` for BrowserStack (a drop-in jar implementing the `devices` SPI).
`BrowserStackCapabilities` builds the W3C caps + `bstack:options` block; `BrowserStackDeviceProvider`
turns a configured device matrix into `DeviceLease`s. Selected by `device.target=browserstack`.
**It is only on the classpath when a team wants cloud runs** — the local provider stays the default.

## 2. How to run (needs a BrowserStack account)
1. Upload the app to BrowserStack → get a `bs://...` app id.
2. Load credentials from `secrets` (e.g. `BROWSERSTACK_USERNAME` / `BROWSERSTACK_ACCESS_KEY`).
3. Configure the matrix: `BROWSERSTACK_DEVICES="Google Pixel 7:13.0, iPhone 15:17"`.
4. Point the driver at the hub: `-Dappium.server.url=https://hub.browserstack.com/wd/hub`
   (the existing `DriverFactory` reuses this), passing caps from `BrowserStackCapabilities.build(...)`.

## 3. Selecting among multiple providers
With both `devices` (local) and this jar on the classpath, resolve by name rather than
`ServiceRegistry.get`: `registry.all(DeviceProvider.class)` then pick the one whose `name()`
matches `config device.target`. (A one-line helper in `Bootstrap` when cloud is enabled.)

## 4. How to add more
- Other clouds (Sauce, LambdaTest): mirror this module — a caps builder + a provider + a
  `META-INF/services` entry. No change to `core`/`devices`.
- App upload automation: add a small uploader hitting the BrowserStack REST API (credentialed,
  integration-only) and feed its returned app id into the caps.

## 5. Token-optimization usage
None directly. Cloud runs widen the device matrix; keep parallel sane so AI/heuristic analysis
isn't drowned in environment-specific noise.

## 6. Examples
```java
var creds = new CloudCredentials(secrets.get("BROWSERSTACK_USERNAME"), secrets.get("BROWSERSTACK_ACCESS_KEY"));
var device = new CloudDevice("Google Pixel 7", "13.0");
Capabilities caps = BrowserStackCapabilities.build(Platform.ANDROID, device, "bs://app123", creds, "Proj", "build-42");
// DriverFactory with -Dappium.server.url=https://hub.browserstack.com/wd/hub creates the session
```
