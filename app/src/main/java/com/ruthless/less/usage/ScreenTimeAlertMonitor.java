package com.ruthless.less.usage;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.ruthless.less.LessApplication;
import com.ruthless.less.launcher.LauncherActivity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Watches overall screen time (not a single app) and notifies at 10 / 20 / 30… minutes.
 * Room / UsageStats work runs off the main thread.
 */
public final class ScreenTimeAlertMonitor {

    private static final String PREFS = "less_screen_time_alerts";
    private static final String KEY_DAY = "day";
    private static final String KEY_LAST_DAILY = "last_daily_milestone";
    private static final String KEY_SESSION_START = "session_start_elapsed";
    private static final String KEY_LAST_SESSION = "last_session_milestone";

    private static final String CHANNEL_ID = "less_screen_time_alerts";
    private static final int NOTIF_BASE = 7100;
    private static final long CHECK_INTERVAL_MS = 60_000L;

    private static final String ACTION_CHECK = "com.ruthless.less.action.SCREEN_TIME_ALERT_CHECK";

    private static ScreenTimeAlertMonitor instance;

    private final Context context;
    private final SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean registered;
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)
                    || Intent.ACTION_USER_PRESENT.equals(action)) {
                onScreenBecameInteractive();
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                onScreenOff();
            }
        }
    };

    public static synchronized ScreenTimeAlertMonitor get(Context context) {
        if (instance == null) {
            instance = new ScreenTimeAlertMonitor(context.getApplicationContext());
        }
        return instance;
    }

    private ScreenTimeAlertMonitor(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Register listeners only; evaluation is always async. Safe from Application.onCreate. */
    public void start() {
        if (!registered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            ContextCompat.registerReceiver(context, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
            registered = true;
            ensureChannel();
        }
        if (isInteractive()) {
            onScreenBecameInteractive();
        }
    }

    public void checkNow() {
        executor.execute(() -> {
            if (!alertsEnabled()) {
                mainHandler.post(this::cancelAlarm);
                return;
            }
            if (!isInteractive()) {
                return;
            }
            evaluateAndNotify();
            mainHandler.post(this::scheduleNextCheck);
        });
    }

    void onAlarm() {
        checkNow();
    }

    private void onScreenBecameInteractive() {
        if (!alertsEnabled()) {
            return;
        }
        if (prefs.getLong(KEY_SESSION_START, 0L) <= 0L) {
            prefs.edit()
                    .putLong(KEY_SESSION_START, SystemClock.elapsedRealtime())
                    .putInt(KEY_LAST_SESSION, 0)
                    .apply();
        }
        checkNow();
    }

    private void onScreenOff() {
        cancelAlarm();
        prefs.edit()
                .putLong(KEY_SESSION_START, 0L)
                .putInt(KEY_LAST_SESSION, 0)
                .apply();
    }

    /** Must run on {@link #executor} — may touch Room. */
    private void evaluateAndNotify() {
        LessApplication app = application();
        if (app == null) {
            return;
        }
        UsageStatsReader reader = app.getUsageStatsReader();
        evaluateSession();
        if (reader.hasUsageAccess()) {
            evaluateDaily(reader);
        }
    }

    private void evaluateDaily(UsageStatsReader reader) {
        String today = UsageStatsReader.todayKey();
        String storedDay = prefs.getString(KEY_DAY, "");
        int last = prefs.getInt(KEY_LAST_DAILY, 0);
        if (!today.equals(storedDay)) {
            last = 0;
            prefs.edit().putString(KEY_DAY, today).putInt(KEY_LAST_DAILY, 0).apply();
        }
        int totalMinutes = (int) (reader.todayTotalMs(true) / 60_000L);
        List<Integer> pending = ScreenTimeAlertLogic.pendingMilestones(totalMinutes, last);
        int highest = last;
        for (int milestone : pending) {
            final int m = milestone;
            mainHandler.post(() -> postNotification(m, true));
            highest = milestone;
        }
        if (highest != last) {
            prefs.edit().putInt(KEY_LAST_DAILY, highest).apply();
        }
    }

    private void evaluateSession() {
        long start = prefs.getLong(KEY_SESSION_START, 0L);
        if (start <= 0L) {
            start = SystemClock.elapsedRealtime();
            prefs.edit().putLong(KEY_SESSION_START, start).putInt(KEY_LAST_SESSION, 0).apply();
        }
        int last = prefs.getInt(KEY_LAST_SESSION, 0);
        int minutes = (int) ((SystemClock.elapsedRealtime() - start) / 60_000L);
        List<Integer> pending = ScreenTimeAlertLogic.pendingMilestones(minutes, last);
        int highest = last;
        for (int milestone : pending) {
            final int m = milestone;
            mainHandler.post(() -> postNotification(m, false));
            highest = milestone;
        }
        if (highest != last) {
            prefs.edit().putInt(KEY_LAST_SESSION, highest).apply();
        }
    }

    private void postNotification(int milestoneMinutes, boolean dailyTotal) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        ensureChannel();
        Intent open = new Intent(context, LauncherActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                context,
                milestoneMinutes + (dailyTotal ? 500 : 0),
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = ScreenTimeAlertLogic.titleFor(milestoneMinutes);
        String body = ScreenTimeAlertLogic.bodyFor(milestoneMinutes, dailyTotal);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(title + "\n" + body))
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        nm.notify(NOTIF_BASE + (dailyTotal ? 500 : 0) + milestoneMinutes, builder.build());
    }

    private void scheduleNextCheck() {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        PendingIntent pi = checkPendingIntent();
        long at = SystemClock.elapsedRealtime() + CHECK_INTERVAL_MS;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi);
        } else {
            am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi);
        }
    }

    private void cancelAlarm() {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(checkPendingIntent());
        }
    }

    private PendingIntent checkPendingIntent() {
        Intent intent = new Intent(context, ScreenTimeAlertReceiver.class);
        intent.setAction(ACTION_CHECK);
        return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private boolean alertsEnabled() {
        LessApplication app = application();
        if (app == null) {
            return true;
        }
        return app.getSettingsRepository().isScreenTimeAlertsEnabled();
    }

    private LessApplication application() {
        Context appCtx = context.getApplicationContext();
        if (appCtx instanceof LessApplication) {
            return (LessApplication) appCtx;
        }
        return null;
    }

    private boolean isInteractive() {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isInteractive();
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Screen time alerts",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Overall screen time milestones");
        nm.createNotificationChannel(channel);
    }

    /** Exposed for tests / alarm action constant. */
    public static String checkAction() {
        return ACTION_CHECK;
    }
}
