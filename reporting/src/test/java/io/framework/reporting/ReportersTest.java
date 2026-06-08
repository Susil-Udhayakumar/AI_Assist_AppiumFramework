package io.framework.reporting;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportersTest {

    @Test
    void fansOutToEveryReporter() {
        List<String> called = new ArrayList<>();
        Reporter a = recording("a", called);
        Reporter b = recording("b", called);

        new Reporters(List.of(a, b)).report(List.of(), Path.of("out"));

        assertThat(called).containsExactly("a", "b");
    }

    private Reporter recording(String name, List<String> sink) {
        return new Reporter() {
            public String name() { return name; }
            public void report(List<TestResult> results, Path outputDir) { sink.add(name); }
        };
    }
}
