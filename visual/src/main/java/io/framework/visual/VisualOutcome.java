package io.framework.visual;

/** Result of a baseline check: a brand-new baseline was stored, or the actual matched / differed. */
public record VisualOutcome(Status status, VisualResult result) {

    public enum Status { NEW_BASELINE, MATCH, DIFF }

    public boolean isFailure() {
        return status == Status.DIFF;
    }
}
