package com.ruthless.less;

import android.app.Application;

import com.ruthless.less.applications.AppRepository;
import com.ruthless.less.boredom.BoredomMessageManager;
import com.ruthless.less.confuse.ConfuseManager;
import com.ruthless.less.database.AppDatabase;
import com.ruthless.less.database.PolicyRepository;
import com.ruthless.less.quota.QuotaManager;
import com.ruthless.less.settings.SettingsRepository;
import com.ruthless.less.usage.UsageRepository;
import com.ruthless.less.usage.UsageStatsReader;

/**
 * Application entry. Keep startup light — no network, no polling.
 */
public class LessApplication extends Application {

    private AppRepository appRepository;
    private SettingsRepository settingsRepository;
    private AppDatabase database;
    private PolicyRepository policyRepository;
    private UsageStatsReader usageStatsReader;
    private UsageRepository usageRepository;
    private QuotaManager quotaManager;
    private ConfuseManager confuseManager;
    private BoredomMessageManager boredomMessageManager;

    @Override
    public void onCreate() {
        super.onCreate();
        database = AppDatabase.getInstance(this);
        settingsRepository = new SettingsRepository(this, database);
        policyRepository = new PolicyRepository(database);
        usageStatsReader = new UsageStatsReader(this, database);
        usageRepository = new UsageRepository(database);
        quotaManager = new QuotaManager(database, usageStatsReader);
        confuseManager = new ConfuseManager(database);
        boredomMessageManager = new BoredomMessageManager(database);
        appRepository = new AppRepository(this, settingsRepository, confuseManager, policyRepository);
        settingsRepository.ensureDefaultsAsync();
        usageStatsReader.refreshExcludedCacheAsync();
        policyRepository.setAfterMutation(usageStatsReader::refreshExcludedCacheBlocking);
        com.ruthless.less.usage.ScreenTimeAlertMonitor.get(this).start();
    }

    public AppRepository getAppRepository() {
        return appRepository;
    }

    public SettingsRepository getSettingsRepository() {
        return settingsRepository;
    }

    public AppDatabase getDatabase() {
        return database;
    }

    public PolicyRepository getPolicyRepository() {
        return policyRepository;
    }

    public UsageStatsReader getUsageStatsReader() {
        return usageStatsReader;
    }

    public UsageRepository getUsageRepository() {
        return usageRepository;
    }

    public QuotaManager getQuotaManager() {
        return quotaManager;
    }

    public ConfuseManager getConfuseManager() {
        return confuseManager;
    }

    public BoredomMessageManager getBoredomMessageManager() {
        return boredomMessageManager;
    }
}
