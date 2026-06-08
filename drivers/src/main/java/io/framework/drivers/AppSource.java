package io.framework.drivers;

import io.framework.core.exception.ConfigException;
import org.openqa.selenium.MutableCapabilities;

import java.io.File;

/**
 * Where the app under test comes from. Four kinds:
 *   FILE             — apk/ipa on disk (validated to exist)
 *   URL              — apk/ipa fetched remotely (also how cloud providers want it)
 *   INSTALLED_ANDROID— attach to an already-installed Android app by package + activity
 *   INSTALLED_IOS    — attach to an already-installed iOS app by bundle id
 *
 * {@link #applyTo(MutableCapabilities)} writes the correct Appium capabilities and
 * {@link #validate()} fails fast on bad input before a session is ever requested.
 */
public final class AppSource {

    public enum Kind { FILE, URL, INSTALLED_ANDROID, INSTALLED_IOS }

    private final Kind kind;
    private final String location;     // file path or URL
    private final String appPackage;
    private final String appActivity;
    private final String bundleId;

    private AppSource(Kind kind, String location, String appPackage, String appActivity, String bundleId) {
        this.kind = kind;
        this.location = location;
        this.appPackage = appPackage;
        this.appActivity = appActivity;
        this.bundleId = bundleId;
    }

    public static AppSource file(String path) {
        return new AppSource(Kind.FILE, path, null, null, null);
    }

    public static AppSource url(String url) {
        return new AppSource(Kind.URL, url, null, null, null);
    }

    public static AppSource installedAndroid(String appPackage, String appActivity) {
        return new AppSource(Kind.INSTALLED_ANDROID, null, appPackage, appActivity, null);
    }

    public static AppSource installedIos(String bundleId) {
        return new AppSource(Kind.INSTALLED_IOS, null, null, null, bundleId);
    }

    public Kind kind() {
        return kind;
    }

    public void validate() {
        switch (kind) {
            case FILE -> {
                if (location == null || !new File(location).isFile()) {
                    throw new ConfigException("App file not found: " + location);
                }
            }
            case URL -> {
                if (location == null || location.isBlank()) {
                    throw new ConfigException("App URL is required for AppSource.url(...)");
                }
            }
            case INSTALLED_ANDROID -> {
                if (appPackage == null || appPackage.isBlank()) {
                    throw new ConfigException("appPackage is required for an installed Android app");
                }
            }
            case INSTALLED_IOS -> {
                if (bundleId == null || bundleId.isBlank()) {
                    throw new ConfigException("bundleId is required for an installed iOS app");
                }
            }
        }
    }

    public void applyTo(MutableCapabilities caps) {
        validate();
        switch (kind) {
            case FILE -> caps.setCapability("appium:app", new File(location).getAbsolutePath());
            case URL -> caps.setCapability("appium:app", location);
            case INSTALLED_ANDROID -> {
                caps.setCapability("appium:appPackage", appPackage);
                caps.setCapability("appium:appActivity", appActivity);
            }
            case INSTALLED_IOS -> caps.setCapability("appium:bundleId", bundleId);
        }
    }
}
