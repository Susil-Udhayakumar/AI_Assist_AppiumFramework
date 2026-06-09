package io.framework.actions;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConditionsTest {

    private final By by = By.id("x");

    @Test
    void visibleReturnsElementWhenDisplayed() {
        var driver = mock(WebDriver.class);
        var el = mock(WebElement.class);
        when(driver.findElement(by)).thenReturn(el);
        when(el.isDisplayed()).thenReturn(true);

        assertThat(Conditions.visible(by).apply(driver)).isSameAs(el);
    }

    @Test
    void visibleReturnsNullWhileHidden() {
        var driver = mock(WebDriver.class);
        var el = mock(WebElement.class);
        when(driver.findElement(by)).thenReturn(el);
        when(el.isDisplayed()).thenReturn(false);

        assertThat(Conditions.visible(by).apply(driver)).isNull();
    }

    @Test
    void goneTrueWhenElementAbsent() {
        var driver = mock(WebDriver.class);
        when(driver.findElement(by)).thenThrow(new NoSuchElementException("x"));

        assertThat(Conditions.gone(by).apply(driver)).isTrue();
    }

    @Test
    void clickableRequiresDisplayedAndEnabled() {
        var driver = mock(WebDriver.class);
        var el = mock(WebElement.class);
        when(driver.findElement(by)).thenReturn(el);
        when(el.isDisplayed()).thenReturn(true);
        when(el.isEnabled()).thenReturn(false);

        assertThat(Conditions.clickable(by).apply(driver)).isNull();
    }

    @Test
    void textPresentMatchesSubstring() {
        var driver = mock(WebDriver.class);
        var el = mock(WebElement.class);
        when(driver.findElement(by)).thenReturn(el);
        when(el.getText()).thenReturn("Welcome back, Alice");

        assertThat(Conditions.textPresent(by, "Welcome").apply(driver)).isSameAs(el);
    }
}
