package io.framework.actions;

/** Catalogue of driver actions whose platform support is gated by {@link ActionSupport}. */
public enum Action {
    // input
    TAP, DOUBLE_TAP, LONG_PRESS, TYPE, CLEAR, REPLACE, HIDE_KEYBOARD, PRESS_KEY,
    // gestures
    SWIPE, SCROLL, PINCH, ZOOM, DRAG_AND_DROP,
    // app lifecycle
    LAUNCH_APP, TERMINATE_APP, ACTIVATE_APP, BACKGROUND_APP, RESET_APP,
    // device
    ROTATE, LOCK, UNLOCK, SHAKE, SET_GEOLOCATION,
    TOGGLE_WIFI, TOGGLE_DATA, TOGGLE_AIRPLANE, NETWORK_THROTTLE,
    // system / messaging
    OPEN_NOTIFICATIONS, READ_NOTIFICATIONS, READ_SMS, SEND_SMS, SIMULATE_CALL,
    // permissions
    GRANT_PERMISSION, REVOKE_PERMISSION,
    // misc
    CLIPBOARD_GET, CLIPBOARD_SET, PUSH_FILE, PULL_FILE, DEEP_LINK, BIOMETRIC,
    SCREENSHOT, PAGE_SOURCE
}
