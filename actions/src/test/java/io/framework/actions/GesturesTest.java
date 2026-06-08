package io.framework.actions;

import io.framework.core.exception.ConfigException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GesturesTest {

    @Test
    void swipeUpMovesFromBottomToTopAlongCentre() {
        SwipeCoordinates c = Gestures.swipe(1000, 2000, SwipeDirection.UP, 0.2);
        assertThat(c).isEqualTo(new SwipeCoordinates(500, 1600, 500, 400));
    }

    @Test
    void swipeRightMovesFromLeftToRightAlongMiddle() {
        SwipeCoordinates c = Gestures.swipe(1000, 2000, SwipeDirection.RIGHT, 0.2);
        assertThat(c).isEqualTo(new SwipeCoordinates(200, 1000, 800, 1000));
    }

    @Test
    void defaultInsetIsApplied() {
        assertThat(Gestures.swipe(1000, 2000, SwipeDirection.DOWN))
                .isEqualTo(Gestures.swipe(1000, 2000, SwipeDirection.DOWN, Gestures.DEFAULT_INSET));
    }

    @Test
    void rejectsBadInset() {
        assertThatThrownBy(() -> Gestures.swipe(1000, 2000, SwipeDirection.UP, 0.6))
                .isInstanceOf(ConfigException.class);
    }

    @Test
    void rejectsNonPositiveViewport() {
        assertThatThrownBy(() -> Gestures.swipe(0, 2000, SwipeDirection.UP))
                .isInstanceOf(ConfigException.class);
    }
}
