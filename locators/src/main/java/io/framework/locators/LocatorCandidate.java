package io.framework.locators;

import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * One way to find an element: a strategy + its value. An element holds several candidates;
 * smart-find tries them in (success-ranked) order so a single broken locator self-heals to
 * the next working one without any AI.
 */
public record LocatorCandidate(Strategy strategy, String value) {

    public By toBy() {
        return switch (strategy) {
            case ID -> By.id(value);
            case ACCESSIBILITY_ID -> AppiumBy.accessibilityId(value);
            case XPATH -> By.xpath(value);
            case CLASS_NAME -> By.className(value);
            case NAME -> By.name(value);
            case ANDROID_UIAUTOMATOR -> AppiumBy.androidUIAutomator(value);
            case IOS_PREDICATE -> AppiumBy.iOSNsPredicateString(value);
            case IOS_CLASS_CHAIN -> AppiumBy.iOSClassChain(value);
        };
    }

    /** Stable identity used for success stats and "tried" reporting. */
    public String key() {
        return strategy + "=" + value;
    }
}
