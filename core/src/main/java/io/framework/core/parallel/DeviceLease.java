package io.framework.core.parallel;

/** A device a worker may hold for the duration of a test. systemPort is the per-session Appium port. */
public record DeviceLease(String deviceId, int systemPort, String providerName) { }
