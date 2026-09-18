package com.ruthless.less.database;

import com.ruthless.less.database.dao.AppPolicyDao;
import com.ruthless.less.database.entities.AppPolicyEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Policy helpers. All DB work stays off the main thread.
 */
public final class PolicyRepository {

    public interface PolicyCallback {
        void onResult(AppPolicyEntity entity);
    }

    public interface PolicyListCallback {
        void onResult(List<AppPolicyEntity> entities);
    }

    public enum PolicyFilter {
        LIMITS, CONFUSE, DELAYS, CONFIRMATION, EXCLUSIONS, SESSION_TIMER
    }

    private final AppDatabase database;
    private final AppPolicyDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile Runnable afterMutation;

    public PolicyRepository(AppDatabase database) {
        this.database = database;
        this.dao = database.appPolicyDao();
    }

    /** Invoked after any policy write (e.g. refresh exclusion cache). */
    public void setAfterMutation(Runnable afterMutation) {
        this.afterMutation = afterMutation;
    }

    public void getOrCreate(String packageName, PolicyCallback callback) {
        executor.execute(() -> {
            AppPolicyEntity entity = dao.getByPackage(packageName);
            if (entity == null) {
                entity = AppPolicyEntity.createDefault(packageName, System.currentTimeMillis());
                dao.upsert(entity);
            }
            callback.onResult(entity);
        });
    }

    public void getByPackageBlockingAware(String packageName, PolicyCallback callback) {
        getOrCreate(packageName, callback);
    }

    public AppPolicyEntity getBlocking(String packageName) {
        return dao.getByPackage(packageName);
    }

    public void update(AppPolicyEntity entity, Runnable onDone) {
        executor.execute(() -> {
            entity.updatedAt = System.currentTimeMillis();
            dao.upsert(entity);
            Runnable hook = afterMutation;
            if (hook != null) {
                hook.run();
            }
            if (onDone != null) {
                onDone.run();
            }
        });
    }

    public void setConfused(String packageName, boolean confused, Runnable onDone) {
        getOrCreate(packageName, entity -> {
            entity.isConfused = confused;
            update(entity, onDone);
        });
    }

    public void setLaunchConfirmation(String packageName, boolean enabled, Runnable onDone) {
        getOrCreate(packageName, entity -> {
            entity.launchConfirmationEnabled = enabled;
            update(entity, onDone);
        });
    }

    public void setLaunchDelayRange(String packageName, String mode, int minMs, int maxMs, Runnable onDone) {
        getOrCreate(packageName, entity -> {
            entity.launchDelayMode = mode;
            entity.launchDelayMinMs = minMs;
            entity.launchDelayMaxMs = maxMs;
            update(entity, onDone);
        });
    }

    public void setDailyLimitMinutes(String packageName, int minutes, Runnable onDone) {
        getOrCreate(packageName, entity -> {
            entity.dailyLimitMinutes = Math.max(0, minutes);
            update(entity, onDone);
        });
    }

    public void setExcludedFromScreenTime(String packageName, boolean excluded, Runnable onDone) {
        getOrCreate(packageName, entity -> {
            entity.excludedFromScreenTime = excluded;
            update(entity, onDone);
        });
    }

    public void setSessionTimerEnabled(String packageName, boolean enabled, Runnable onDone) {
        getOrCreate(packageName, entity -> {
            entity.sessionTimerEnabled = enabled;
            update(entity, onDone);
        });
    }

    public void listFiltered(PolicyFilter filter, PolicyListCallback callback) {
        executor.execute(() -> {
            List<AppPolicyEntity> source;
            switch (filter) {
                case CONFUSE:
                    source = dao.getConfused();
                    break;
                case DELAYS:
                    source = dao.getWithLaunchDelay();
                    break;
                case CONFIRMATION:
                    source = dao.getWithConfirmation();
                    break;
                case EXCLUSIONS:
                    source = dao.getExcludedFromScreenTime();
                    break;
                case SESSION_TIMER:
                    source = dao.getWithSessionTimer();
                    break;
                case LIMITS:
                default:
                    source = dao.getWithLimits();
                    break;
            }
            callback.onResult(new ArrayList<>(source));
        });
    }

    public boolean hasNonDefaultPolicyBlocking(String packageName) {
        AppPolicyEntity e = dao.getByPackage(packageName);
        if (e == null) {
            return false;
        }
        return e.dailyLimitMinutes > 0
                || e.isConfused
                || e.launchConfirmationEnabled
                || (e.launchDelayMode != null && !"OFF".equals(e.launchDelayMode))
                || e.excludedFromScreenTime
                || e.sessionTimerEnabled;
    }

    public void clearAllPoliciesAsync(Runnable onDone) {
        executor.execute(() -> {
            dao.deleteAll();
            if (onDone != null) {
                onDone.run();
            }
        });
    }

    public void clearFrictionsAndHistoryAsync(Runnable onDone) {
        executor.execute(() -> {
            database.dailyUsageDao().deleteAll();
            database.dailyQuotaDao().deleteAll();
            database.confusePlacementDao().clearAll();
            database.boredomMessageHistoryDao().deleteAll();
            if (onDone != null) {
                onDone.run();
            }
        });
    }
}
