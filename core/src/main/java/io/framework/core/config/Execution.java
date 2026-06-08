package io.framework.core.config;

public record Execution(Mode mode, ParallelBy parallelBy, int threads) {
    public enum Mode { SEQUENTIAL, PARALLEL }
    public enum ParallelBy { TEST, CLASS, SUITE, PLATFORM, DEVICE }

    public Execution {
        if (threads < 1) throw new IllegalArgumentException("threads must be >= 1");
    }
}
