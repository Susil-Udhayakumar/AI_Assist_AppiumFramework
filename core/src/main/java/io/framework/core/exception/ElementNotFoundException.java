package io.framework.core.exception;

import java.util.List;

/** Thrown when no locator candidate (and no heal) resolves an element. */
public class ElementNotFoundException extends FrameworkException {
    private final String elementName;
    private final List<String> triedCandidates;

    public ElementNotFoundException(String elementName, List<String> triedCandidates) {
        super("Element '" + elementName + "' not found. Tried candidates: " + triedCandidates);
        this.elementName = elementName;
        this.triedCandidates = List.copyOf(triedCandidates);
    }

    public String elementName() { return elementName; }
    public List<String> triedCandidates() { return triedCandidates; }
}
