package io.framework.locators;

import io.framework.core.exception.ElementNotFoundException;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmartFinderTest {

    private final LocatorCandidate id = new LocatorCandidate(Strategy.ID, "login");
    private final LocatorCandidate xpath = new LocatorCandidate(Strategy.XPATH, "//Button");

    private LocatorRepository repo() {
        return new LocatorRepository().register("Login", "btn", id, xpath);
    }

    @Test
    void returnsFirstMatchingCandidate() {
        var ctx = mock(SearchContext.class);
        var el = mock(WebElement.class);
        when(ctx.findElement(By.id("login"))).thenReturn(el);

        var finder = new SmartFinder(repo(), new LocatorStats(), null);
        assertThat(finder.find(ctx, "Login", "btn")).isSameAs(el);
    }

    @Test
    void fallsThroughToNextCandidateWhenFirstMisses() {
        var ctx = mock(SearchContext.class);
        var el = mock(WebElement.class);
        when(ctx.findElement(By.id("login"))).thenThrow(new NoSuchElementException("nope"));
        when(ctx.findElement(By.xpath("//Button"))).thenReturn(el);

        var finder = new SmartFinder(repo(), new LocatorStats(), null);
        assertThat(finder.find(ctx, "Login", "btn")).isSameAs(el);
    }

    @Test
    void recordsSuccessForRanking() {
        var ctx = mock(SearchContext.class);
        var el = mock(WebElement.class);
        when(ctx.findElement(By.id("login"))).thenThrow(new NoSuchElementException("nope"));
        when(ctx.findElement(By.xpath("//Button"))).thenReturn(el);

        var stats = new LocatorStats();
        new SmartFinder(repo(), stats, null).find(ctx, "Login", "btn");

        assertThat(stats.wins("Login", "btn", xpath)).isEqualTo(1);
    }

    @Test
    void throwsWithTriedListWhenAllMissAndNoHealer() {
        var ctx = mock(SearchContext.class);
        when(ctx.findElement(By.id("login"))).thenThrow(new NoSuchElementException("nope"));
        when(ctx.findElement(By.xpath("//Button"))).thenThrow(new NoSuchElementException("nope"));

        var finder = new SmartFinder(repo(), new LocatorStats(), null);
        assertThatThrownBy(() -> finder.find(ctx, "Login", "btn"))
                .isInstanceOf(ElementNotFoundException.class)
                .hasMessageContaining("ID=login")
                .hasMessageContaining("XPATH=//Button");
    }

    @Test
    void healerRescuesWhenAllCandidatesMiss() {
        var ctx = mock(SearchContext.class);
        var el = mock(WebElement.class);
        when(ctx.findElement(By.id("login"))).thenThrow(new NoSuchElementException("nope"));
        when(ctx.findElement(By.xpath("//Button"))).thenThrow(new NoSuchElementException("nope"));
        when(ctx.findElement(By.id("healed"))).thenReturn(el);

        ElementHealer healer = request -> Optional.of(By.id("healed"));
        var finder = new SmartFinder(repo(), new LocatorStats(), healer);

        assertThat(finder.find(ctx, "Login", "btn")).isSameAs(el);
    }
}
