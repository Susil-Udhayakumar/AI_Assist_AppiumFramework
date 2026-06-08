package io.framework.knowledge;

import io.framework.locators.LocatorCandidate;
import io.framework.locators.LocatorRepository;
import io.framework.locators.SmartFinder;
import io.framework.locators.Strategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** LocatorMemory plugs into SmartFinder as the ranker, and the win it records survives a restart. */
class PersistentRankingIntegrationTest {

    @Test
    void smartFinderRecordsWinIntoPersistentMemory(@TempDir Path dir) {
        var id = new LocatorCandidate(Strategy.ID, "login");
        var repo = new LocatorRepository().register("Login", "btn", id);

        var ctx = mock(SearchContext.class);
        var el = mock(WebElement.class);
        when(ctx.findElement(By.id("login"))).thenReturn(el);

        var memory = new LocatorMemory(dir);                 // persistent ranker
        var finder = new SmartFinder(repo, memory, null);

        assertThat(finder.find(ctx, "Login", "btn")).isSameAs(el);

        // win persisted: a fresh memory instance (new run) sees it
        assertThat(new LocatorMemory(dir).wins("Login", "btn", id)).isEqualTo(1);
    }
}
