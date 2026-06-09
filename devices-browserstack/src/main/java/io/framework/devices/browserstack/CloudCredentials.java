package io.framework.devices.browserstack;

/** BrowserStack credentials. Load these from the `secrets` module — never hardcode. */
public record CloudCredentials(String userName, String accessKey) {
}
