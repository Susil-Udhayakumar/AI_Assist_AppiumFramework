package io.framework.drivers;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.framework.core.config.Platform;
import io.framework.core.driver.DriverProvider;
import io.framework.core.exception.DriverInitException;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Real {@link DriverProvider}: creates an Android/iOS Appium session against an Appium server.
 * Discovered by core's ServiceRegistry via ServiceLoader (public no-arg constructor), which
 * resolves the server URL from the {@code appium.server.url} system property, the
 * {@code APPIUM_SERVER_URL} env var, or the local default.
 *
 * Note: {@link #create} opens a real session, so it requires a running Appium server + device.
 * The capability/AppSource logic is unit-tested separately; this class is exercised by the
 * examples smoke tests against a local emulator/simulator.
 */
public final class DriverFactory implements DriverProvider {

    public static final String DEFAULT_SERVER = "http://127.0.0.1:4723";

    private final URL serverUrl;

    public DriverFactory() {
        this(resolveServerUrl());
    }

    DriverFactory(URL serverUrl) {
        this.serverUrl = serverUrl;
    }

    private static URL resolveServerUrl() {
        String raw = System.getProperty("appium.server.url",
                System.getenv().getOrDefault("APPIUM_SERVER_URL", DEFAULT_SERVER));
        try {
            return new URL(raw);
        } catch (MalformedURLException e) {
            throw new DriverInitException("Invalid Appium server URL: " + raw, e);
        }
    }

    @Override
    public WebDriver create(Platform platform, Capabilities caps) {
        try {
            return switch (platform) {
                case ANDROID -> new AndroidDriver(serverUrl, caps);
                case IOS -> new IOSDriver(serverUrl, caps);
                case BOTH -> throw new DriverInitException(
                        "DriverFactory requires a concrete platform (ANDROID or IOS), got BOTH", null);
            };
        } catch (DriverInitException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new DriverInitException(
                    "Failed to create " + platform + " driver at " + serverUrl, e);
        }
    }

    public URL serverUrl() {
        return serverUrl;
    }
}
