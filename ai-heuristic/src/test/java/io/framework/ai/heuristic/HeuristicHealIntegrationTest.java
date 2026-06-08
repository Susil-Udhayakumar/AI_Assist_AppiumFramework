package io.framework.ai.heuristic;

import io.framework.locators.LocatorCandidate;
import io.framework.locators.LocatorRepository;
import io.framework.locators.LocatorStats;
import io.framework.locators.SmartFinder;
import io.framework.locators.Strategy;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** SmartFinder + HeuristicElementHealer: all candidates miss, heal recovers from page source. */
class HeuristicHealIntegrationTest {

    @Test
    void smartFinderRecoversViaHeuristicHeal() {
        var driver = mock(WebDriver.class);
        var element = mock(WebElement.class);

        // registered candidates both miss
        when(driver.findElement(By.id("login"))).thenThrow(new NoSuchElementException("x"));
        when(driver.findElement(By.xpath("//Button"))).thenThrow(new NoSuchElementException("x"));
        // page source the healer will read
        when(driver.getPageSource()).thenReturn(
                "<hierarchy><android.widget.Button resource-id=\"com.x.app:id/login_button\"/></hierarchy>");
        // the healed locator resolves
        when(driver.findElement(By.id("com.x.app:id/login_button"))).thenReturn(element);

        var repo = new LocatorRepository().register("LoginScreen", "loginButton",
                new LocatorCandidate(Strategy.ID, "login"),
                new LocatorCandidate(Strategy.XPATH, "//Button"));
        var finder = new SmartFinder(repo, new LocatorStats(), new HeuristicElementHealer());

        assertThat(finder.find(driver, "LoginScreen", "loginButton")).isSameAs(element);
    }
}
