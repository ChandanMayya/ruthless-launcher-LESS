package com.ruthless.less.usage;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import android.os.Process;
import android.provider.Settings;

import com.ruthless.less.database.AppDatabase;
import com.ruthless.less.database.dao.AppPolicyDao;
import com.ruthless.less.database.dao.DailyUsageDao;
import com.ruthless.less.database.entities.AppPolicyEntity;
import com.ruthless.less.database.entities.DailyUsageEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Reads UsageStatsManager when permission is granted.
 *
 * Limitation: Android may not retain historical usage across uninstall/reinstall
 * of a third-party app. LESS stores its own DailyUsageEntity snapshots when possible,
 * but cannot recover system usage the OS discarded.
 *
 * Pickup/unlock counts use UsageEvents (KEYGUARD_HIDDEN / SCREEN_INTERACTIVE) as a
 * best-effort proxy — OEM ROMs vary and exact unlock counts are not always available.
 */
public final class UsageStatsReader {

    public static final class UsageRow {
        public final String packageName;
        public final long totalTimeMs;

        public UsageRow(String packageName, long totalTimeMs) {
            this.packageName = packageName;
            this.totalTimeMs = totalTimeMs;
        }

        public int minutes() {
            return (int) (totalTimeMs / 60_000L);
        }
    }

    private final Context context;
    private final DailyUsageDao usageDao;
    private final AppPolicyDao policyDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    /** Snapshot of excluded packages — never read Room for this on the main thread. */
    private volatile Set<String> excludedCache = Collections.emptySet();

    public UsageStatsReader(Context context, AppDatabase database) {
        this.context = context.getApplicationContext();
        this.usageDao = database.dailyUsageDao();
        this.policyDao = database.appPolicyDao();
    }

    public boolean hasUsageAccess() {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) {
            return false;
        }
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public void openUsageAccessSettings() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /** Reload exclusion list off the main thread. Safe from Application / UI. */
    public void refreshExcludedCacheAsync() {
        executor.execute(this::reloadExcludedCacheBlocking);
    }

    /**
     * Reload exclusion list now. Must not run on the main thread.
     * Safe to call from other background executors (e.g. after a policy write).
     */
    public void refreshExcludedCacheBlocking() {
        if (isMainThread()) {
            refreshExcludedCacheAsync();
            return;
        }
        reloadExcludedCacheBlocking();
    }

    private void reloadExcludedCacheBlocking() {
        Set<String> out = new HashSet<>();
        for (AppPolicyEntity e : policyDao.getExcludedFromScreenTime()) {
            out.add(e.packageName);
        }
        excludedCache = Collections.unmodifiableSet(out);
    }

    /**
     * Packages with screen-time monitoring off.
     * On the main thread returns the last cache (never hits Room).
     */
    public Set<String> excludedPackagesBlocking() {
        if (isMainThread()) {
            return excludedCache;
        }
        reloadExcludedCacheBlocking();
        return excludedCache;
    }

    public interface LongCallback {
        void onResult(long value);
    }

    public interface TodayRowsCallback {
        void onResult(List<UsageRow> rows, long totalMs);
    }

    /** Async total that may touch Room for exclusions. */
    public void todayTotalMsAsync(boolean respectExclusions, LongCallback callback) {
        executor.execute(() -> {
            long total = todayTotalMs(respectExclusions);
            if (callback != null) {
                callback.onResult(total);
            }
        });
    }

    /** Async today rows + total for UI screens. */
    public void queryTodayAsync(boolean respectExclusions, TodayRowsCallback callback) {
        executor.execute(() -> {
            List<UsageRow> rows = queryToday(respectExclusions);
            long total = 0;
            for (UsageRow row : rows) {
                total += row.totalTimeMs;
            }
            if (callback != null) {
                callback.onResult(rows, total);
            }
        });
    }

    public List<UsageRow> queryToday() {
        return queryToday(false);
    }

    /** @param respectExclusions when true, omit apps marked excludedFromScreenTime from quota/totals. */
    public List<UsageRow> queryToday(boolean respectExclusions) {
        if (!hasUsageAccess()) {
            return Collections.emptyList();
        }
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) {
            return Collections.emptyList();
        }
        Calendar cal = Calendar.getInstance();
        long end = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        Map<String, UsageStats> stats = usm.queryAndAggregateUsageStats(start, end);
        Set<String> excluded = respectExclusions
                ? excludedPackagesBlocking()
                : Collections.emptySet();
        List<UsageRow> rows = new ArrayList<>();
        String self = context.getPackageName();
        for (Map.Entry<String, UsageStats> entry : stats.entrySet()) {
            if (self.equals(entry.getKey())) {
                continue;
            }
            if (excluded.contains(entry.getKey())) {
                continue;
            }
            long time = entry.getValue().getTotalTimeInForeground();
            if (time <= 0) {
                continue;
            }
            rows.add(new UsageRow(entry.getKey(), time));
        }
        Collections.sort(rows, Comparator.comparingLong((UsageRow r) -> r.totalTimeMs).reversed());
        return rows;
    }

    public long todayTotalMs() {
        return todayTotalMs(true);
    }

    public long todayTotalMs(boolean respectExclusions) {
        long total = 0;
        for (UsageRow row : queryToday(respectExclusions)) {
            total += row.totalTimeMs;
        }
        return total;
    }

    private static boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    public int minutesForPackageToday(String packageName) {
        for (UsageRow row : queryToday(false)) {
            if (row.packageName.equals(packageName)) {
                return row.minutes();
            }
        }
        return 0;
    }

    /** Persist a lightweight snapshot for history. Call on resume, not in a loop. */
    public void snapshotTodayAsync() {
        executor.execute(() -> {
            if (!hasUsageAccess()) {
                return;
            }
            String date = todayKey();
            // Store raw usage including excluded apps — do not silently delete history.
            for (UsageRow row : queryToday(false)) {
                DailyUsageEntity entity = usageDao.get(row.packageName, date);
                if (entity == null) {
                    entity = new DailyUsageEntity();
                    entity.packageName = row.packageName;
                    entity.date = date;
                    entity.launchCount = 0;
                }
                entity.foregroundMinutes = row.minutes();
                usageDao.upsert(entity);
            }
        });
    }

    public void recordLaunchAsync(String packageName) {
        executor.execute(() -> {
            String date = todayKey();
            DailyUsageEntity entity = usageDao.get(packageName, date);
            if (entity == null) {
                entity = new DailyUsageEntity();
                entity.packageName = packageName;
                entity.date = date;
                entity.foregroundMinutes = 0;
                entity.launchCount = 0;
            }
            entity.launchCount = entity.launchCount + 1;
            usageDao.upsert(entity);
        });
    }

    /**
     * Best-effort phone pickup / unlock proxy for a calendar day.
     * Prefers KEYGUARD_HIDDEN / SCREEN_INTERACTIVE events; falls back to activity-resume bursts.
     */
    public int countPickupsForDayOffset(int dayOffset) {
        if (!hasUsageAccess()) {
            return -1;
        }
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (usm == null) {
            return -1;
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -dayOffset);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        long end = cal.getTimeInMillis();

        UsageEvents events = usm.queryEvents(start, end);
        if (events == null) {
            return -1;
        }
        UsageEvents.Event event = new UsageEvents.Event();
        int keyguardOrScreen = 0;
        int activityBursts = 0;
        long lastBurst = 0;
        while (events.hasNextEvent()) {
            events.getNextEvent(event);
            int type = event.getEventType();
            if (type == UsageEvents.Event.KEYGUARD_HIDDEN
                    || (Build.VERSION.SDK_INT >= 29 && type == UsageEvents.Event.SCREEN_INTERACTIVE)) {
                keyguardOrScreen++;
            } else if (type == UsageEvents.Event.ACTIVITY_RESUMED
                    || type == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                long t = event.getTimeStamp();
                if (t - lastBurst > 60_000L) {
                    activityBursts++;
                    lastBurst = t;
                }
            }
        }
        return keyguardOrScreen > 0 ? keyguardOrScreen : activityBursts;
    }

    public static String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static String formatDuration(long millis) {
        long totalMinutes = Math.max(0, millis / 60_000L);
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours <= 0) {
            return minutes + "M";
        }
        return hours + "H " + minutes + "M";
    }

    public static String formatMinutes(int totalMinutes) {
        int hours = Math.max(0, totalMinutes) / 60;
        int minutes = Math.max(0, totalMinutes) % 60;
        if (hours <= 0) {
            return minutes + "M";
        }
        return hours + "H " + minutes + "M";
    }

    public static String formatRelative(long epochMs) {
        if (epochMs <= 0) {
            return "UNKNOWN";
        }
        long diff = Math.max(0, System.currentTimeMillis() - epochMs);
        long minutes = diff / 60_000L;
        if (minutes < 1) {
            return "JUST NOW";
        }
        if (minutes < 60) {
            return minutes + " MINUTES AGO";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " HOURS AGO";
        }
        return (hours / 24) + " DAYS AGO";
    }
}
