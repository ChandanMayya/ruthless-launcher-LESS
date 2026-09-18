package com.ruthless.less.session;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.ruthless.less.R;
import com.ruthless.less.accessibility.UsageEnforcementAccessibilityService;
import com.ruthless.less.launcher.LauncherActivity;

/**
 * Shows a boring text countdown over the active app, then warns and returns home.
 * Leaving the timed app (home or another app) cancels the session early.
 *
 * Requires SYSTEM_ALERT_WINDOW for the overlay. Closing uses HOME intent + optional
 * Accessibility GLOBAL_ACTION_HOME. Not a hard force-stop of the other app.
 */
public class SessionTimerService extends Service {

    public static final String EXTRA_PACKAGE = "package";
    public static final String EXTRA_LABEL = "label";
    public static final String EXTRA_DURATION_MS = "duration_ms";

    private static final String CHANNEL_ID = "less_session_timer";
    private static final int NOTIF_ID = 42;
    private static final long WARN_CLOSE_MS = 4_000L;
    /** Ignore brief System UI / picker packages before treating the session as left. */
    private static final long LEAVE_GRACE_MS = 800L;

    private static volatile String activePackage;
    private static volatile long endsAtMs;
    private static volatile boolean warning;

    private static final Handler leaveHandler = new Handler(Looper.getMainLooper());
    private static Runnable pendingLeaveCancel;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private View overlay;
    private TextView timerText;
    private String packageName;
    private String label;
    private long endsAt;
    private boolean closing;

    public static boolean hasActiveSession() {
        return activePackage != null && System.currentTimeMillis() < endsAtMs + WARN_CLOSE_MS;
    }

    public static String getActivePackage() {
        return activePackage;
    }

    public static boolean isExpired() {
        return activePackage != null && System.currentTimeMillis() >= endsAtMs;
    }

    public static boolean isWarning() {
        return warning;
    }

    public static void start(Context context, String packageName, String label, long durationMs) {
        cancelPendingLeave();
        Intent intent = new Intent(context, SessionTimerService.class);
        intent.putExtra(EXTRA_PACKAGE, packageName);
        intent.putExtra(EXTRA_LABEL, label);
        intent.putExtra(EXTRA_DURATION_MS, durationMs);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        cancelPendingLeave();
        context.stopService(new Intent(context, SessionTimerService.class));
    }

    /**
     * Call when the foreground package changes. If the user left the timed app,
     * cancel the countdown (after a short grace for System UI).
     */
    public static void onForegroundPackage(Context context, String foregroundPackage) {
        String active = activePackage;
        if (context == null || active == null || foregroundPackage == null) {
            return;
        }
        if (active.equals(foregroundPackage)) {
            cancelPendingLeave();
            return;
        }
        if (isTransientUiPackage(foregroundPackage)) {
            return;
        }
        cancelPendingLeave();
        final Context appCtx = context.getApplicationContext();
        pendingLeaveCancel = () -> {
            pendingLeaveCancel = null;
            if (active.equals(activePackage)) {
                stop(appCtx);
            }
        };
        leaveHandler.postDelayed(pendingLeaveCancel, LEAVE_GRACE_MS);
    }

    private static void cancelPendingLeave() {
        if (pendingLeaveCancel != null) {
            leaveHandler.removeCallbacks(pendingLeaveCancel);
            pendingLeaveCancel = null;
        }
    }

    private static boolean isTransientUiPackage(String pkg) {
        return "com.android.systemui".equals(pkg)
                || "com.android.intentresolver".equals(pkg)
                || "android".equals(pkg)
                || pkg.contains("permissioncontroller")
                || pkg.contains("packageinstaller")
                || pkg.contains("systemui");
    }

    public static boolean canDrawOverlays(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    public static void openOverlaySettings(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        packageName = intent.getStringExtra(EXTRA_PACKAGE);
        label = intent.getStringExtra(EXTRA_LABEL);
        long duration = intent.getLongExtra(EXTRA_DURATION_MS, 0L);
        if (packageName == null || duration <= 0L) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (label == null) {
            label = packageName.toUpperCase();
        }

        endsAt = System.currentTimeMillis() + duration;
        endsAtMs = endsAt;
        activePackage = packageName;
        warning = false;
        closing = false;

        ensureChannel();
        startForeground(NOTIF_ID, buildNotification("SESSION  " + formatRemaining(duration)));
        showOverlay();
        handler.removeCallbacksAndMessages(null);
        handler.post(tick);

        return START_STICKY;
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            long left = endsAt - System.currentTimeMillis();
            if (closing) {
                return;
            }
            if (left <= 0) {
                beginCloseSequence();
                return;
            }
            String remaining = formatRemaining(left);
            if (timerText != null) {
                timerText.setText(remaining);
            }
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(NOTIF_ID, buildNotification("SESSION  " + remaining));
            }
            handler.postDelayed(this, 250L);
        }
    };

    private void beginCloseSequence() {
        if (closing) {
            return;
        }
        closing = true;
        warning = true;
        if (timerText != null) {
            timerText.setText("TIME UP\nCLOSING");
        }
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_ID, buildNotification("TIME UP — CLOSING"));
        }
        handler.postDelayed(this::forceHomeAndStop, WARN_CLOSE_MS);
    }

    private void forceHomeAndStop() {
        UsageEnforcementAccessibilityService.performHome();
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
        } catch (Exception ignored) {
        }
        stopSelf();
    }

    private void showOverlay() {
        if (!canDrawOverlays(this)) {
            return;
        }
        removeOverlay();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) {
            return;
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(Color.BLACK);
        box.setPadding(28, 16, 28, 16);
        box.setElevation(0f);

        timerText = new TextView(this);
        timerText.setTextColor(Color.WHITE);
        timerText.setTypeface(Typeface.MONOSPACE);
        timerText.setTextSize(16f);
        timerText.setGravity(Gravity.CENTER);
        timerText.setAllCaps(true);
        timerText.setText(formatRemaining(endsAt - System.currentTimeMillis()));
        box.addView(timerText);
        overlay = box;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = 48;
        try {
            windowManager.addView(overlay, params);
        } catch (Exception e) {
            overlay = null;
            timerText = null;
        }
    }

    private void removeOverlay() {
        if (windowManager != null && overlay != null) {
            try {
                windowManager.removeView(overlay);
            } catch (Exception ignored) {
            }
        }
        overlay = null;
        timerText = null;
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Session timer",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("LESS session countdown");
        channel.setSound(null, null);
        nm.createNotificationChannel(channel);
    }

    private Notification buildNotification(String content) {
        Intent open = new Intent(this, LauncherActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                this,
                0,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("LESS")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private static String formatRemaining(long ms) {
        long totalSec = Math.max(0, (ms + 999) / 1000);
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format("%d:%02d", min, sec);
    }

    @Override
    public void onDestroy() {
        cancelPendingLeave();
        handler.removeCallbacksAndMessages(null);
        removeOverlay();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NOTIF_ID);
        }
        activePackage = null;
        endsAtMs = 0;
        warning = false;
        closing = false;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
