package io.framework.observability;

import io.framework.core.config.Capture;
import io.framework.core.events.TestEvent;

/** Pure decision: should a screenshot be taken for this event under this capture setting? */
public final class CapturePolicy {

    private CapturePolicy() {
    }

    public static boolean shouldScreenshot(Capture.When when, TestEvent event) {
        return switch (when) {
            case OFF -> false;
            case ON_FAILURE -> event == TestEvent.TEST_FAIL;
            case ON_ASSERTION -> event == TestEvent.ASSERTION || event == TestEvent.TEST_FAIL;
            case ON_ACTION -> event == TestEvent.AFTER_ACTION
                    || event == TestEvent.ASSERTION
                    || event == TestEvent.TEST_FAIL;
            case ALWAYS -> event == TestEvent.AFTER_ACTION
                    || event == TestEvent.ASSERTION
                    || event == TestEvent.TEST_FAIL
                    || event == TestEvent.TEST_PASS;
        };
    }
}
