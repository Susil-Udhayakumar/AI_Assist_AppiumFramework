package io.framework.devices.browserstack;

/** A configured cloud device: a device name and an optional OS version. */
public record CloudDevice(String name, String osVersion) {
}
