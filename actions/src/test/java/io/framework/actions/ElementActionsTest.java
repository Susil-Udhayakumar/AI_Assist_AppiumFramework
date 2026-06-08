package io.framework.actions;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.openqa.selenium.WebElement;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ElementActionsTest {

    private final ElementActions actions = new ElementActions();

    @Test
    void tapClicks() {
        WebElement el = mock(WebElement.class);
        actions.tap(el);
        verify(el).click();
    }

    @Test
    void typeSendsKeys() {
        WebElement el = mock(WebElement.class);
        actions.type(el, "hello");
        verify(el).sendKeys("hello");
    }

    @Test
    void clearClears() {
        WebElement el = mock(WebElement.class);
        actions.clear(el);
        verify(el).clear();
    }

    @Test
    void replaceClearsThenTypes() {
        WebElement el = mock(WebElement.class);
        actions.replace(el, "new");
        InOrder order = inOrder(el);
        order.verify(el).clear();
        order.verify(el).sendKeys("new");
    }
}
