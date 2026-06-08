package io.framework.actions;

import io.framework.core.exception.ConfigException;

/**
 * Pure gesture geometry. Converts a viewport size + direction into swipe coordinates, kept
 * separate from the driver so the math is unit-tested without a device. {@code inset} is the
 * fraction of the screen left as margin at each end of the swipe (0 &lt; inset &lt; 0.5).
 */
public final class Gestures {

    public static final double DEFAULT_INSET = 0.2;

    private Gestures() {
    }

    public static SwipeCoordinates swipe(int width, int height, SwipeDirection direction, double inset) {
        if (width <= 0 || height <= 0) {
            throw new ConfigException("Viewport size must be positive, got " + width + "x" + height);
        }
        if (inset <= 0 || inset >= 0.5) {
            throw new ConfigException("inset must be between 0 and 0.5, got " + inset);
        }
        int cx = width / 2;
        int cy = height / 2;
        int top = (int) (height * inset);
        int bottom = (int) (height * (1 - inset));
        int left = (int) (width * inset);
        int right = (int) (width * (1 - inset));

        return switch (direction) {
            case UP -> new SwipeCoordinates(cx, bottom, cx, top);
            case DOWN -> new SwipeCoordinates(cx, top, cx, bottom);
            case LEFT -> new SwipeCoordinates(right, cy, left, cy);
            case RIGHT -> new SwipeCoordinates(left, cy, right, cy);
        };
    }

    public static SwipeCoordinates swipe(int width, int height, SwipeDirection direction) {
        return swipe(width, height, direction, DEFAULT_INSET);
    }
}
