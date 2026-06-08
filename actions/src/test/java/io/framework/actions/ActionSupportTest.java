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
