package io.framework.observability;

import io.framework.core.config.Capture;
import io.framework.core.events.TestEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CapturePolicyTest {

    @Test
    void offNeverShoots() {
        assertThat(CapturePolicy.shouldScreenshot(Capture.When.OFF, TestEvent.TEST_FAIL)).isFalse();
    }

    @Test
    void onFailureOnlyOnFail() {
        assertThat(CapturePolicy.shouldScreenshot(Capture.When.ON_FAILURE, TestEvent.TEST_FAIL)).isTrue();
        assertThat(CapturePolicy.shouldScreenshot(Capture.When.ON_FAILURE, TestEvent.AFTER_ACTION)).isFalse();
    }

    @Test
    void onActionShootsOnActionAndFailure() {
        assertThat(CapturePolicy.shouldScreenshot(Capture.When.ON_ACTION, TestEvent.AFTER_ACTION)).isTrue();
        assertThat(CapturePolicy.shouldScreenshot(Capture.When.ON_ACTION, TestEvent.TEST_FAIL)).isTrue();
        assertThat(CapturePolicy.shouldScreenshot(Capture.When.ON_ACTION, TestEvent.TEST_START)).isFalse();
    }

    @Test
    void alwaysShootsOnPass() {
        assertThat(CapturePolicy.shouldScreenshot(Capture.When.ALWAYS, TestEvent.TEST_PASS)).isTrue();
    }
}
