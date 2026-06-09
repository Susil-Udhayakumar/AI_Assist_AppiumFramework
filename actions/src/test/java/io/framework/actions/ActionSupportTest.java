package io.framework.actions;

import io.framework.core.config.Platform;
import io.framework.core.exception.UnsupportedActionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class ActionSupportTest {

    @Test
    void universalActionSupportedOnBothPlatforms() {
        assertThat(ActionSupport.isSupported(Action.TAP, Platform.ANDROID)).isTrue();
        assertThat(ActionSupport.isSupported(Action.TAP, Platform.IOS)).isTrue();
        assertThat(ActionSupport.isSupported(Action.TAP, Platform.BOTH)).isTrue();
    }

    @Test
    void readSmsIsAndroidOnly() {
        assertThat(ActionSupport.isSupported(Action.READ_SMS, Platform.ANDROID)).isTrue();
        assertThat(ActionSupport.isSupported(Action.READ_SMS, Platform.IOS)).isFalse();
    }

    @Test
    void shakeIsIosOnly() {
        assertThat(ActionSupport.isSupported(Action.SHAKE, Platform.IOS)).isTrue();
        assertThat(ActionSupport.isSupported(Action.SHAKE, Platform.ANDROID)).isFalse();
    }

    @Test
    void extendedGesturesSupportedOnBoth() {
        assertThat(ActionSupport.isSupported(Action.FLING, Platform.BOTH)).isTrue();
        assertThat(ActionSupport.isSupported(Action.SCROLL_TO_ELEMENT, Platform.ANDROID)).isTrue();
        assertThat(ActionSupport.isSupported(Action.TWO_FINGER_TAP, Platform.IOS)).isTrue();
    }

    @Test
    void toastAndWatcherAreAndroidOnly() {
        assertThat(ActionSupport.isSupported(Action.GET_TOAST, Platform.ANDROID)).isTrue();
        assertThat(ActionSupport.isSupported(Action.GET_TOAST, Platform.IOS)).isFalse();
        assertThat(ActionSupport.isSupported(Action.REGISTER_WATCHER, Platform.IOS)).isFalse();
    }

    @Test
    void siriAndPickerWheelAreIosOnly() {
        assertThat(ActionSupport.isSupported(Action.SIRI_COMMAND, Platform.IOS)).isTrue();
        assertThat(ActionSupport.isSupported(Action.SIRI_COMMAND, Platform.ANDROID)).isFalse();
        assertThat(ActionSupport.isSupported(Action.SELECT_PICKER_WHEEL, Platform.ANDROID)).isFalse();
    }

    @Test
    void requireThrowsForUnsupportedPlatform() {
        assertThatThrownBy(() -> ActionSupport.require(Action.READ_SMS, Platform.IOS))
                .isInstanceOf(UnsupportedActionException.class)
                .hasMessageContaining("READ_SMS")
                .hasMessageContaining("IOS");
    }

    @Test
    void requirePassesForSupported() {
        assertThatCode(() -> ActionSupport.require(Action.TAP, Platform.ANDROID))
                .doesNotThrowAnyException();
    }
}
