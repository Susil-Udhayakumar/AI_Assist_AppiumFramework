package io.framework.reporting;

/** One test's outcome. message is null/empty unless the test failed. */
public record TestResult(String name, TestStatus status, long durationMs, String device, String message) {

    public static TestResult pass(String name, long durationMs, String device) {
        return new TestResult(name, TestStatus.PASS, durationMs, device, null);
    }

    public static TestResult fail(String name, long durationMs, String device, String message) {
        return new TestResult(name, TestStatus.FAIL, durationMs, device, message);
    }

    public static TestResult skip(String name, String device) {
        return new TestResult(name, TestStatus.SKIP, 0, device, null);
    }
}
