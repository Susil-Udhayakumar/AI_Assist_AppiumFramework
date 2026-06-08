package io.framework.examples;

import io.framework.actions.ElementActions;
import io.framework.core.context.ContextManager;
import io.framework.locators.SmartFinder;
import org.openqa.selenium.WebDriver;

/**
 * Sample hybrid page object: readable typed methods (POM API) over the central locator repo +
 * smart-find (multi-candidate, self-healing). Locators live in locators.yaml under the
 * "LoginScreen" key; this class never hardcodes a single brittle locator.
 */
public final class LoginScreen {

    private final SmartFinder finder;
    private final ElementActions actions;

    public LoginScreen(SmartFinder finder, ElementActions actions) {
        this.finder = finder;
        this.actions = actions;
    }

    private WebDriver driver() {
        return ContextManager.current().driver();
    }

    public LoginScreen enterUsername(String text) {
        actions.type(finder.find(driver(), "LoginScreen", "username"), text);
        return this;
    }

    public LoginScreen enterPassword(String text) {
        actions.type(finder.find(driver(), "LoginScreen", "password"), text);
        return this;
    }

    public void tapLogin() {
        actions.tap(finder.find(driver(), "LoginScreen", "loginButton"));
    }
}
