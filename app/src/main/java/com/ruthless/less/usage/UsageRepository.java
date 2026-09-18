package com.ruthless.less.usage;

import com.ruthless.less.database.AppDatabase;
import com.ruthless.less.database.dao.AppPolicyDao;
import com.ruthless.less.database.dao.DailyUsageDao;
import com.ruthless.less.database.entities.AppPolicyEntity;
import com.ruthless.less.database.entities.DailyUsageEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Aggregates stored daily usage for history views.
 */
public final class UsageRepository {

    public interface HistoryCallback {
        void onHistory(List<DayTotal> days);
    }

    public interface RangeCallback {
        void onRange(RangeSummary summary);
    }

    public static final class DayTotal {
        public final String date;
        public final int totalMinutes;

        public DayTotal(String date, int totalMinutes) {
            this.date = date;
            this.totalMinutes = totalMinutes;
        }
    }

    public static final class RangeSummary {
        public final List<DayTotal> days;
        public final int averageMinutes;
        public final int previousAverageMinutes;
        public final int deltaMinutes;

        public RangeSummary(
                List<DayTotal> days,
                int averageMinutes,
                int previousAverageMinutes,
                int deltaMinutes) {
            this.days = days;
            this.averageMinutes = averageMinutes;
            this.previousAverageMinutes = previousAverageMinutes;
            this.deltaMinutes = deltaMinutes;
        }
    }

    private final DailyUsageDao dao;
    private final AppPolicyDao policyDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public UsageRepository(AppDatabase database) {
        this.dao = database.dailyUsageDao();
        this.policyDao = database.appPolicyDao();
    }

    public void loadLastDays(int dayCount, HistoryCallback callback) {
        loadLastDays(dayCount, true, callback);
    }

    public void loadLastDays(int dayCount, boolean respectExclusions, HistoryCallback callback) {
        executor.execute(() -> {
            Set<String> excluded = respectExclusions ? excludedPackages() : Collections.emptySet();
            List<DayTotal> out = new ArrayList<>();
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            for (int i = 0; i < dayCount; i++) {
                String date = fmt.format(cal.getTime());
                out.add(new DayTotal(date, sumForDate(date, excluded)));
                cal.add(Calendar.DAY_OF_YEAR, -1);
            }
            callback.onHistory(out);
        });
    }

    /**
     * Loads a contiguous day window and compares average against the immediately
     * preceding window of the same length.
     */
    public void loadRangeSummary(int dayCount, RangeCallback callback) {
        executor.execute(() -> {
            Set<String> excluded = excludedPackages();
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Calendar cal = Calendar.getInstance();
            List<DayTotal> current = new ArrayList<>();
            for (int i = 0; i < dayCount; i++) {
                String date = fmt.format(cal.getTime());
                current.add(new DayTotal(date, sumForDate(date, excluded)));
                cal.add(Calendar.DAY_OF_YEAR, -1);
            }
            List<DayTotal> previous = new ArrayList<>();
            for (int i = 0; i < dayCount; i++) {
                String date = fmt.format(cal.getTime());
                previous.add(new DayTotal(date, sumForDate(date, excluded)));
                cal.add(Calendar.DAY_OF_YEAR, -1);
            }
            int curAvg = average(current);
            int prevAvg = average(previous);
            callback.onRange(new RangeSummary(current, curAvg, prevAvg, curAvg - prevAvg));
        });
    }

    public void clearAllUsageAsync(Runnable onDone) {
        executor.execute(() -> {
            dao.deleteAll();
            if (onDone != null) {
                onDone.run();
            }
        });
    }

    private int sumForDate(String date, Set<String> excluded) {
        List<DailyUsageEntity> rows = dao.getForDate(date);
        int sum = 0;
        for (DailyUsageEntity row : rows) {
            if (excluded.contains(row.packageName)) {
                continue;
            }
            sum += row.foregroundMinutes;
        }
        return sum;
    }

    private Set<String> excludedPackages() {
        Set<String> out = new HashSet<>();
        for (AppPolicyEntity e : policyDao.getExcludedFromScreenTime()) {
            out.add(e.packageName);
        }
        return out;
    }

    private static int average(List<DayTotal> days) {
        if (days == null || days.isEmpty()) {
            return 0;
        }
        int sum = 0;
        for (DayTotal d : days) {
            sum += d.totalMinutes;
        }
        return sum / days.size();
    }
}
