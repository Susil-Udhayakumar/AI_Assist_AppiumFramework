package io.framework.locators;

import java.util.List;

/**
 * Context handed to an {@link ElementHealer} when every registered candidate has missed.
 * pageSource may be empty when the search context is not a full driver.
 */
public record HealRequest(String screen, String elementName,
                          List<LocatorCandidate> tried, String pageSource) {
}
