package com.ruthless.less.system;

import android.content.Context;

import com.ruthless.less.accessibility.UsageEnforcementAccessibilityService;

import java.lang.reflect.Method;

/**
 * Opens the system notification shade or quick settings.
 * Prefers Accessibility global actions; falls back to StatusBarManager reflection
 * (works on many devices without special permissions; may fail on some OEMs).
 */
public final class SystemPanels {

    private SystemPanels() {
    }

    public static boolean openNotifications(Context context) {
        if (UsageEnforcementAccessibilityService.openNotifications()) {
            return true;
        }
        return expandViaReflection(context, true);
    }

    public static boolean openQuickSettings(Context context) {
        if (UsageEnforcementAccessibilityService.openQuickSettings()) {
            return true;
        }
        return expandViaReflection(context, false);
    }

    private static boolean expandViaReflection(Context context, boolean notifications) {
        if (context == null) {
            return false;
        }
        try {
            Object statusBar = context.getApplicationContext().getSystemService("statusbar");
            if (statusBar == null) {
                return false;
            }
            String[] names = notifications
                    ? new String[]{"expandNotificationsPanel"}
                    : new String[]{"expandSettingsPanel", "expandQuickSettingsPanel"};
            for (String name : names) {
                try {
                    Method method = statusBar.getClass().getMethod(name);
                    method.invoke(statusBar);
                    return true;
                } catch (NoSuchMethodException ignored) {
                    // try next
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }
}
