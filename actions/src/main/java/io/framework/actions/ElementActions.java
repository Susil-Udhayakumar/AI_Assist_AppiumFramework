package io.framework.actions;

import org.openqa.selenium.WebElement;

/**
 * Thin, platform-neutral element interactions. These are supported on every platform, so they
 * do not gate through {@link ActionSupport}. Driver/gesture-bound actions (swipe, SMS, etc.)
 * live alongside and consult the support matrix.
 */
public final class ElementActions {

    public void tap(WebElement element) {
        element.click();
    }

    public void type(WebElement element, CharSequence text) {
        element.sendKeys(text);
    }

    public void clear(WebElement element) {
        element.clear();
    }

    public void replace(WebElement element, CharSequence text) {
        element.clear();
        element.sendKeys(text);
    }
}
