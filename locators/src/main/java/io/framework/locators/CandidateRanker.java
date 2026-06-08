package io.framework.locators;

import java.util.List;

/**
 * Orders an element's locator candidates by past success and records wins. Implemented by the
 * in-memory `LocatorStats` (here) and the persistent `LocatorMemory` (knowledge module), so
 * `SmartFinder` ranking can be swapped from in-process to cross-run by changing one wiring line.
 */
public interface CandidateRanker {

    List<LocatorCandidate> rank(String screen, String element, List<LocatorCandidate> candidates);

    void recordSuccess(String screen, String element, LocatorCandidate candidate);
}
