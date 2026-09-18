package com.ruthless.less.quota;

import com.ruthless.less.database.AppDatabase;
import com.ruthless.less.database.dao.AppPolicyDao;
import com.ruthless.less.database.dao.DailyQuotaDao;
import com.ruthless.less.database.entities.AppPolicyEntity;
import com.ruthless.less.database.entities.DailyQuotaEntity;
import com.ruthless.less.usage.UsageStatsReader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Daily per-app quotas + one five-minute extension per day.
 */
public final class QuotaManager {

    public static final int EXTENSION_MINUTES = 5;

    public static final class QuotaState {
        public final String packageName;
        public final int limitMinutes;
        public final int usedMinutes;
        public final int remainingMinutes;
        public final boolean exhausted;
        public final boolean extensionUsed;
        public final boolean extensionActive;
        public final int extensionRemainingMinutes;

        public QuotaState(
                String packageName,
                int limitMinutes,
                int usedMinutes,
                int remainingMinutes,
                boolean exhausted,
                boolean extensionUsed,
                boolean extensionActive,
                int extensionRemainingMinutes) {
            this.packageName = packageName;
            this.limitMinutes = limitMinutes;
            this.usedMinutes = usedMinutes;
            this.remainingMinutes = remainingMinutes;
            this.exhausted = exhausted;
            this.extensionUsed = extensionUsed;
            this.extensionActive = extensionActive;
            this.extensionRemainingMinutes = extensionRemainingMinutes;
        }
    }

    public interface StateCallback {
        void onState(QuotaState state);
    }

    private final AppPolicyDao policyDao;
    private final DailyQuotaDao quotaDao;
    private final UsageStatsReader usageReader;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public QuotaManager(AppDatabase database, UsageStatsReader usageReader) {
        this.policyDao = database.appPolicyDao();
        this.quotaDao = database.dailyQuotaDao();
        this.usageReader = usageReader;
    }

    public void evaluate(String packageName, int usedMinutesFromStats, StateCallback callback) {
        executor.execute(() -> {
            AppPolicyEntity policy = policyDao.getByPackage(packageName);
            int limit = policy == null ? 0 : policy.dailyLimitMinutes;
            boolean excluded = policy != null && policy.excludedFromScreenTime;
            if (limit <= 0 || excluded) {
                callback.onState(new QuotaState(packageName, 0, usedMinutesFromStats, Integer.MAX_VALUE,
                        false, false, false, 0));
                return;
            }

            String date = UsageStatsReader.todayKey();
            DailyQuotaEntity quota = quotaDao.get(packageName, date);
            if (quota == null) {
                quota = new DailyQuotaEntity();
                quota.packageName = packageName;
                quota.date = date;
                quota.limitMinutes = limit;
                quota.extensionUsed = false;
                quota.extensionMinutes = 0;
                quota.quotaExhausted = false;
            } else {
                quota.limitMinutes = limit;
            }

            int allowed = limit + (quota.extensionUsed ? quota.extensionMinutes : 0);
            int remaining = Math.max(0, allowed - usedMinutesFromStats);
            boolean exhausted = usedMinutesFromStats >= allowed;
            quota.quotaExhausted = exhausted;
            quotaDao.upsert(quota);

            boolean extensionActive = quota.extensionUsed
                    && usedMinutesFromStats < limit + quota.extensionMinutes
                    && usedMinutesFromStats >= limit;

            callback.onState(new QuotaState(
                    packageName,
                    limit,
                    usedMinutesFromStats,
                    remaining,
                    exhausted,
                    quota.extensionUsed,
                    extensionActive,
                    extensionActive ? remaining : 0
            ));
        });
    }

    /**
     * Grants the single daily five-minute extension if not already used.
     * @return true if granted
     */
    public boolean requestExtensionBlocking(String packageName) {
        String date = UsageStatsReader.todayKey();
        DailyQuotaEntity quota = quotaDao.get(packageName, date);
        if (quota == null) {
            AppPolicyEntity policy = policyDao.getByPackage(packageName);
            if (policy == null || policy.dailyLimitMinutes <= 0) {
                return false;
            }
            quota = new DailyQuotaEntity();
            quota.packageName = packageName;
            quota.date = date;
            quota.limitMinutes = policy.dailyLimitMinutes;
            quota.extensionUsed = false;
            quota.extensionMinutes = 0;
            quota.quotaExhausted = true;
        }
        if (quota.extensionUsed) {
            return false;
        }
        quota.extensionUsed = true;
        quota.extensionMinutes = EXTENSION_MINUTES;
        quota.quotaExhausted = false;
        quotaDao.upsert(quota);
        return true;
    }

    public void requestExtensionAsync(String packageName, Runnable onDone) {
        executor.execute(() -> {
            requestExtensionBlocking(packageName);
            if (onDone != null) {
                onDone.run();
            }
        });
    }
}
