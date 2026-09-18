package com.ruthless.less.tour;

/**
 * Actions a tour beat may request. Host activity interprets these.
 */
public enum TourAction {
    NEXT,
    FINISH_ONBOARDING,
    OPEN_HOME_SETTINGS,
    OPEN_USAGE_ACCESS,
    OPEN_ACCESSIBILITY,
    OPEN_OVERLAY_SETTINGS,
    REQUEST_NOTIFICATIONS,
    OPEN_HOME_APPS,
    OPEN_PINNED_APPS,
    OPEN_SCREEN_TIME,
    OPEN_DATA,
    OPEN_POLICY_LIMITS,
    OPEN_POLICY_CONFUSE,
    OPEN_POLICY_DELAYS,
    OPEN_POLICY_CONFIRMATION,
    OPEN_POLICY_SESSION_TIMER,
    OPEN_POLICY_EXCLUSIONS,
    OPEN_FIELD_MANUAL,
    /** Open Field Manual to a specific chapter; beat.chapterId holds the target. */
    OPEN_FIELD_CHAPTER,
    START_BOUNDARY_PICK,
    BOUNDARY_CONFIRMATION,
    BOUNDARY_DELAY,
    SKIP_BOUNDARY,
    DISMISS
}
