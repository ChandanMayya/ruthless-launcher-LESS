package com.ruthless.less.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

import com.ruthless.less.LessApplication;
import com.ruthless.less.database.entities.AppPolicyEntity;
import com.ruthless.less.quota.QuotaManager;
import com.ruthless.less.usage.UsageStatsReader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Optional Accessibility hooks:
 * 1) Return home when a quota-exhausted app reaches the foreground.
 * 2) Lock the screen on double-tap from LESS home (GLOBAL_ACTION_LOCK_SCREEN).
 * 3) Open notification shade / quick settings from home swipe-down.
 *
 * Only the main Accessibility toggle must be ON. The system “LESS shortcut”
 * (floating accessibility button) is not used and should stay OFF.
 *
 * Android limitation: not a hard OS jail; users can revoke the service.
 * We never poll in a loop. Room / usage work runs off the main thread.
 */
public class UsageEnforcementAccessibilityService extends AccessibilityService {

    private static volatile UsageEnforcementAccessibilityService instance;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static boolean isConnected() {
        return instance != null;
    }

    /**
     * Best-effort screen lock. Requires this service enabled and API 28+.
     * @return true if the lock action was requested successfully
     */
    public static boolean lockScreen() {
        UsageEnforcementAccessibilityService svc = instance;
        if (svc == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false;
        }
        return svc.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
    }

    /** Best-effort return to home (session timer / quota). */
    public static boolean performHome() {
        UsageEnforcementAccessibilityService svc = instance;
        if (svc == null) {
            return false;
        }
        return svc.performGlobalAction(GLOBAL_ACTION_HOME);
    }

    /** Open the system notification shade. API 28+. */
    public static boolean openNotifications() {
        UsageEnforcementAccessibilityService svc = instance;
        if (svc == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false;
        }
        return svc.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
    }

    /** Open the system quick settings panel. API 28+. */
    public static boolean openQuickSettings() {
        UsageEnforcementAccessibilityService svc = instance;
        if (svc == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false;
        }
        return svc.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) {
            return;
        }
        final String pkg = pkgCs.toString();
        if (getPackageName().equals(pkg)) {
            // Back on LESS home — cancel any active session for another app.
            com.ruthless.less.session.SessionTimerService.onForegroundPackage(this, pkg);
            return;
        }

        // Session timer: leaving the timed app cancels the countdown.
        com.ruthless.less.session.SessionTimerService.onForegroundPackage(this, pkg);

        // Session timer expired — bounce home if still in the timed app.
        if (com.ruthless.less.session.SessionTimerService.isExpired()
                && pkg.equals(com.ruthless.less.session.SessionTimerService.getActivePackage())) {
            performGlobalAction(GLOBAL_ACTION_HOME);
            return;
        }

        executor.execute(() -> evaluateQuotaForPackage(pkg));
    }

    private void evaluateQuotaForPackage(String pkg) {
        try {
            LessApplication app = (LessApplication) getApplication();
            if (app == null) {
                return;
            }
            AppPolicyEntity policy = app.getDatabase().appPolicyDao().getByPackage(pkg);
            if (policy == null || policy.dailyLimitMinutes <= 0 || policy.excludedFromScreenTime) {
                return;
            }

            UsageStatsReader usage = app.getUsageStatsReader();
            int used = 0;
            for (UsageStatsReader.UsageRow row : usage.queryToday()) {
                if (pkg.equals(row.packageName)) {
                    used = row.minutes();
                    break;
                }
            }
            final int usedMinutes = used;
            QuotaManager quotas = app.getQuotaManager();
            quotas.evaluate(pkg, usedMinutes, state -> {
                if (state.exhausted) {
                    mainHandler.post(() -> {
                        UsageEnforcementAccessibilityService svc = instance;
                        if (svc != null) {
                            svc.performGlobalAction(GLOBAL_ACTION_HOME);
                        }
                    });
                }
            });
        } catch (Exception ignored) {
            // Never crash the accessibility service — Android marks it broken.
        }
    }

    @Override
    public void onInterrupt() {
        // Nothing continuous to stop — by design.
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        // Do not set FLAG_REQUEST_ACCESSIBILITY_BUTTON — that floating button is unused.
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        setServiceInfo(info);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (instance == this) {
            instance = null;
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        if (instance == this) {
            instance = null;
        }
        executor.shutdownNow();
        super.onDestroy();
    }
}
