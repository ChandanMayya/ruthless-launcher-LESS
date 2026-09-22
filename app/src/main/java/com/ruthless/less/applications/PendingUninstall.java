package com.ruthless.less.applications;

/**
 * Holds a pending uninstall package so {@code LauncherActivity} can start the
 * system uninstaller while it is the resumed foreground activity.
 * Background starts (broadcast / post-dialog application context) are blocked on
 * Android 10+ / Samsung and appear to succeed without showing UI.
 */
public final class PendingUninstall {

    private static volatile String packageName;

    private PendingUninstall() {
    }

    public static void set(String pkg) {
        packageName = pkg;
    }

    public static String take() {
        String pkg = packageName;
        packageName = null;
        return pkg;
    }
}
