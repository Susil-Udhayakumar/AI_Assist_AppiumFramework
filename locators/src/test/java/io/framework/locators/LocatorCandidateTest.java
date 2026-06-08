package io.framework.locators;

import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.assertj.core.api.Assertions.assertThat;

class LocatorCandidateTest {

    @Test
    void idMapsToSeleniumById() {
        assertThat(new LocatorCandidate(Strategy.ID, "login").toBy()).isEqualTo(By.id("login"));
    }

    @Test
    void accessibilityIdMapsToAppiumBy() {
        By by = new LocatorCandidate(Strategy.ACCESSIBILITY_ID, "login_btn").toBy();
        assertThat(by).isInstanceOf(AppiumBy.class);
        assertThat(by.toString()).contains("login_btn");
    }

    @Test
    void androidUiautomatorMapsToAppiumBy() {
        By by = new LocatorCandidate(Strategy.ANDROID_UIAUTOMATOR, "new UiSelector()").toBy();
        assertThat(by).isInstanceOf(AppiumBy.class);
        assertThat(by.toString()).contains("new UiSelector()");
    }

    @Test
    void keyIsStableIdentity() {
        assertThat(new LocatorCandidate(Strategy.ID, "login").key()).isEqualTo("ID=login");
    }
}
