package io.framework.actions;

import io.framework.core.config.Platform;
import io.framework.core.exception.UnsupportedActionException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import static io.framework.actions.Action.*;

/**
 * Platform-capability matrix. Code asks {@link #isSupported} (or {@link #require}) before
 * performing a platform-specific action, so the heuristic/AI heal never invents an
 * unsupported gesture and tests fail fast with a clear {@link UnsupportedActionException}.
 */
public final class ActionSupport {

    private static final Map<Action, EnumSet<Platform>> SUPPORT = new EnumMap<>(Action.class);

    static {
        both(TAP, DOUBLE_TAP, LONG_PRESS, TYPE, CLEAR, REPLACE, HIDE_KEYBOARD, PRESS_KEY,
                SWIPE, SCROLL, PINCH, ZOOM, DRAG_AND_DROP,
                LAUNCH_APP, TERMINATE_APP, ACTIVATE_APP, BACKGROUND_APP, RESET_APP,
                ROTATE, LOCK, UNLOCK, SET_GEOLOCATION,
                CLIPBOARD_GET, CLIPBOARD_SET, PUSH_FILE, PULL_FILE, DEEP_LINK, BIOMETRIC,
                SCREENSHOT, PAGE_SOURCE);
        android(OPEN_NOTIFICATIONS, READ_NOTIFICATIONS, READ_SMS, SEND_SMS, SIMULATE_CALL,
                GRANT_PERMISSION, REVOKE_PERMISSION,
                TOGGLE_WIFI, TOGGLE_DATA, TOGGLE_AIRPLANE, NETWORK_THROTTLE);
        ios(SHAKE);
    }

    private ActionSupport() {
    }

    public static boolean isSupported(Action action, Platform platform) {
        EnumSet<Platform> set = SUPPORT.getOrDefault(action, EnumSet.noneOf(Platform.class));
        if (platform == Platform.BOTH) {
            return set.contains(Platform.ANDROID) && set.contains(Platform.IOS);
        }
        return set.contains(platform);
    }

    public static void require(Action action, Platform platform) {
        if (!isSupported(action, platform)) {
            throw new UnsupportedActionException(action.name(), platform.name());
        }
    }

    private static void both(Action... actions) {
        for (Action a : actions) {
            SUPPORT.put(a, EnumSet.of(Platform.ANDROID, Platform.IOS));
        }
    }

    private static void android(Action... actions) {
        for (Action a : actions) {
            SUPPORT.put(a, EnumSet.of(Platform.ANDROID));
        }
    }

    private static void ios(Action... actions) {
        for (Action a : actions) {
            SUPPORT.put(a, EnumSet.of(Platform.IOS));
        }
    }
}
