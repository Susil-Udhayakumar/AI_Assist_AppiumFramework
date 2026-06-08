package io.framework.knowledge;

import java.nio.file.Path;

/**
 * Project-local, git-committed learning store. Bundles the learned memories under one base dir:
 *   {base}/learned/locators.yaml, heals.yaml, failures.yaml, history/*.md
 * The reference-knowledge half (Figma/PRD/Confluence ingestion + index) arrives in a later wave.
 */
public final class KnowledgeStore {

    private final LocatorMemory locators;
    private final HealMemory heals;
    private final FailureMemory failures;
    private final ExecutionHistory history;

    public KnowledgeStore(Path baseDir) {
        this.locators = new LocatorMemory(baseDir);
        this.heals = new HealMemory(baseDir);
        this.failures = new FailureMemory(baseDir);
        this.history = new ExecutionHistory(baseDir);
    }

    public LocatorMemory locators() {
        return locators;
    }

    public HealMemory heals() {
        return heals;
    }

    public FailureMemory failures() {
        return failures;
    }

    public ExecutionHistory history() {
        return history;
    }
}
