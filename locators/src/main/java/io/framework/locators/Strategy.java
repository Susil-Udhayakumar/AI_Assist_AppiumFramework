package io.framework.locators;

import io.framework.core.exception.ConfigException;

/** Locator strategies supported across Android and iOS. */
public enum Strategy {
    ID, ACCESSIBILITY_ID, XPATH, CLASS_NAME, NAME,
    ANDROID_UIAUTOMATOR, IOS_PREDICATE, IOS_CLASS_CHAIN;

    /** Parse a strategy name from config (case/separator-insensitive). */
    public static Strategy from(String raw) {
        if (raw == null) {
            throw new ConfigException("locator strategy is required");
        }
        String norm = raw.trim().toLowerCase().replace("-", "").replace("_", "");
        return switch (norm) {
            case "id" -> ID;
            case "accessibilityid", "accessibility", "a11y" -> ACCESSIBILITY_ID;
            case "xpath" -> XPATH;
            case "classname", "class" -> CLASS_NAME;
            case "name" -> NAME;
            case "androiduiautomator", "uiautomator" -> ANDROID_UIAUTOMATOR;
            case "iospredicate", "predicate" -> IOS_PREDICATE;
            case "iosclasschain", "classchain" -> IOS_CLASS_CHAIN;
            default -> throw new ConfigException("Unknown locator strategy: " + raw);
        };
    }
}
