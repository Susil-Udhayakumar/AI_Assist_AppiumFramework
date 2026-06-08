package io.framework.actions;

import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.FluentWait;

import java.util.function.Function;

/**
 * Global smart-sync: waits for a condition before acting, so tests never need Thread.sleep.
 * Ignores transient lookup/staleness exceptions while polling within the configured timeout.
 */
public final class Waits {

    private final WaitConfig config;

    public Waits(WaitConfig config) {
        this.config = config;
    }

    public <T> T until(WebDriver driver, Function<? super WebDriver, T> condition) {
        return new FluentWait<>(driver)
                .withTimeout(config.timeout())
                .pollingEvery(config.poll())
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .until(condition);
    }

    public WaitConfig config() {
        return config;
    }
}
