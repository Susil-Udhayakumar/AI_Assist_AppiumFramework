package io.framework.actions;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.function.Function;

/**
 * Common smart-sync wait conditions, usable with {@link Waits#until}. Each returns a function
 * that yields a truthy result once satisfied and null/false while waiting (so FluentWait keeps
 * polling). Mirrors the explicit waits Appium drivers rely on (visible / gone / clickable /
 * text-present / attribute), since neither UiAutomator2 nor XCUITest expose them natively.
 */
public final class Conditions {

    private Conditions() {
    }

    public static Function<WebDriver, WebElement> visible(By by) {
        return driver -> {
            WebElement e = driver.findElement(by);
            return e.isDisplayed() ? e : null;
        };
    }

    public static Function<WebDriver, Boolean> gone(By by) {
        return driver -> {
            try {
                return !driver.findElement(by).isDisplayed();
            } catch (NoSuchElementException absent) {
                return Boolean.TRUE;
            }
        };
    }

    public static Function<WebDriver, WebElement> clickable(By by) {
        return driver -> {
            WebElement e = driver.findElement(by);
            return (e.isDisplayed() && e.isEnabled()) ? e : null;
        };
    }

    public static Function<WebDriver, WebElement> textPresent(By by, String text) {
        return driver -> {
            WebElement e = driver.findElement(by);
            String actual = e.getText();
            return (actual != null && actual.contains(text)) ? e : null;
        };
    }

    public static Function<WebDriver, WebElement> attribute(By by, String name, String value) {
        return driver -> {
            WebElement e = driver.findElement(by);
            return value.equals(e.getAttribute(name)) ? e : null;
        };
    }
}
