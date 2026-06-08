package io.framework.core.events;

public enum TestEvent {
    RUN_START, SUITE_START, TEST_START,
    BEFORE_ACTION, AFTER_ACTION, ASSERTION,
    TEST_FAIL, TEST_PASS, TEST_END,
    SUITE_END, RUN_END
}
