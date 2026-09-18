package com.ruthless.less.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.ruthless.less.applications.AppRepository;
import com.ruthless.less.database.AppDatabase;
import com.ruthless.less.database.dao.AppPolicyDao;
import com.ruthless.less.database.entities.AppPolicyEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight preferences + Room-backed policies.
 * Phase 1 uses SharedPreferences for home/pins; Phase 2 Room mirrors and owns policy fields.
 */
public final class SettingsRepository {

    public static final String[] DEFAULT_HOME_ROLES = {
            "PHONE", "MESSAGES", "CONTACTS", "CAMERA", "CALENDAR", "MUSIC"
    };

    private static final String PREFS = "less_settings";
    private static final String KEY_HOME = "home_packages";
    private static final String KEY_PINNED = "pinned_packages";
    private static final String KEY_SEEDED = "defaults_seeded";
    private static final String KEY_LIST_STYLE = "list_style";
    private static final String KEY_BOREDOM = "boredom_mode";
    private static final String KEY_ONBOARDING = "onboarding_done";
    private static final String KEY_ONBOARDING_STEP = "onboarding_step";
    private static final String KEY_TEXT_SIZE = "text_size";
    private static final String KEY_FONT = "font";
    private static final String KEY_LAST_UNLOCK_AT = "last_unlock_at";
    private static final String KEY_LAST_OPENED_PKG = "last_opened_pkg";
    private static final String KEY_LAST_OPENED_LABEL = "last_opened_label";
    private static final String KEY_LAST_OPENED_AT = "last_opened_at";
    private static final String KEY_SCREEN_TIME_ALERTS = "screen_time_alerts";
    private static final String KEY_REMINDER_LONG_PRESS = "reminder_long_press";
    private static final String KEY_REMINDER_SCREEN_TIME = "reminder_screen_time";
    private static final String KEY_REMINDER_CONFUSE = "reminder_confuse";
    private static final String KEY_REMINDER_QUOTA = "reminder_quota";

    public static final String REMINDER_LONG_PRESS = "long_press";
    public static final String REMINDER_SCREEN_TIME = "screen_time";
    public static final String REMINDER_CONFUSE = "confuse";
    public static final String REMINDER_QUOTA = "quota";

    public enum ListStyle {
        COMPACT, NORMAL, SPACED, VERY_SPACED
    }

    public enum TextSize {
        SMALL, NORMAL, LARGE
    }

    public enum FontChoice {
        MONOSPACE, SANS
    }

    private final Context context;
    private final SharedPreferences prefs;
    private final AppDatabase database;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SettingsRepository(Context context, AppDatabase database) {
        this.context = context.getApplicationContext();
        this.database = database;
        this.prefs = this.context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void ensureDefaultsAsync() {
        executor.execute(() -> {
            if (!prefs.getBoolean(KEY_SEEDED, false)) {
                // Leave empty until apps are known; LauncherActivity will call seedDefaultsIfNeeded.
            }
            // Boredom ON by default.
            if (!prefs.contains(KEY_BOREDOM)) {
                prefs.edit().putBoolean(KEY_BOREDOM, true).apply();
            }
            if (!prefs.contains(KEY_LIST_STYLE)) {
                prefs.edit().putString(KEY_LIST_STYLE, ListStyle.NORMAL.name()).apply();
            }
            if (!prefs.contains(KEY_SCREEN_TIME_ALERTS)) {
                prefs.edit().putBoolean(KEY_SCREEN_TIME_ALERTS, true).apply();
            }
        });
    }

    public void seedDefaultsIfNeeded(AppRepository apps) {
        if (prefs.getBoolean(KEY_SEEDED, false) && !getHomePackages().isEmpty()) {
            return;
        }
        List<String> home = new ArrayList<>();
        for (String role : DEFAULT_HOME_ROLES) {
            String pkg = apps.resolveDefaultPackage(role);
            if (pkg != null && !home.contains(pkg)) {
                home.add(pkg);
            }
        }
        setHomePackages(home);
        prefs.edit().putBoolean(KEY_SEEDED, true).apply();
        syncHomeToRoom(home);
    }

    public List<String> getHomePackages() {
        return decodeList(prefs.getString(KEY_HOME, ""));
    }

    public void setHomePackages(List<String> packages) {
        prefs.edit().putString(KEY_HOME, encodeList(packages)).apply();
        syncHomeToRoom(packages);
    }

    public void addHomePackage(String packageName) {
        List<String> list = new ArrayList<>(getHomePackages());
        if (!list.contains(packageName)) {
            list.add(packageName);
            setHomePackages(list);
        }
    }

    public void removeHomePackage(String packageName) {
        List<String> list = new ArrayList<>(getHomePackages());
        if (list.remove(packageName)) {
            setHomePackages(list);
        }
    }

    public void moveHomePackage(int from, int to) {
        List<String> list = new ArrayList<>(getHomePackages());
        if (from < 0 || from >= list.size() || to < 0 || to >= list.size()) {
            return;
        }
        String item = list.remove(from);
        list.add(to, item);
        setHomePackages(list);
    }

    public void resetHomeDefaults(AppRepository apps) {
        prefs.edit().putBoolean(KEY_SEEDED, false).apply();
        seedDefaultsIfNeeded(apps);
    }

    public List<String> getPinnedPackages() {
        return decodeList(prefs.getString(KEY_PINNED, ""));
    }

    public void setPinnedPackages(List<String> packages) {
        prefs.edit().putString(KEY_PINNED, encodeList(packages)).apply();
        syncPinnedToRoom(packages);
    }

    public boolean isPinned(String packageName) {
        return getPinnedPackages().contains(packageName);
    }

    public void pin(String packageName) {
        List<String> list = new ArrayList<>(getPinnedPackages());
        if (!list.contains(packageName)) {
            list.add(packageName);
            setPinnedPackages(list);
        }
    }

    public void unpin(String packageName) {
        List<String> list = new ArrayList<>(getPinnedPackages());
        if (list.remove(packageName)) {
            setPinnedPackages(list);
        }
    }

    public void movePinned(int from, int to) {
        List<String> list = new ArrayList<>(getPinnedPackages());
        if (from < 0 || from >= list.size() || to < 0 || to >= list.size()) {
            return;
        }
        String item = list.remove(from);
        list.add(to, item);
        setPinnedPackages(list);
    }

    public boolean isHomeApp(String packageName) {
        return getHomePackages().contains(packageName);
    }

    public ListStyle getListStyle() {
        try {
            return ListStyle.valueOf(prefs.getString(KEY_LIST_STYLE, ListStyle.NORMAL.name()));
        } catch (IllegalArgumentException e) {
            return ListStyle.NORMAL;
        }
    }

    public void setListStyle(ListStyle style) {
        prefs.edit().putString(KEY_LIST_STYLE, style.name()).apply();
    }

    public boolean isBoredomModeEnabled() {
        return prefs.getBoolean(KEY_BOREDOM, true);
    }

    public void setBoredomModeEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BOREDOM, enabled).apply();
    }

    public boolean isScreenTimeAlertsEnabled() {
        return prefs.getBoolean(KEY_SCREEN_TIME_ALERTS, true);
    }

    public void setScreenTimeAlertsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SCREEN_TIME_ALERTS, enabled).apply();
    }

    public boolean isOnboardingDone() {
        return prefs.getBoolean(KEY_ONBOARDING, false);
    }

    public void setOnboardingDone(boolean done) {
        SharedPreferences.Editor edit = prefs.edit().putBoolean(KEY_ONBOARDING, done);
        if (done) {
            edit.remove(KEY_ONBOARDING_STEP);
        }
        edit.apply();
    }

    /** 0-based first-run tour index; survives relaunch when becoming default home. */
    public int getOnboardingStep() {
        return Math.max(0, prefs.getInt(KEY_ONBOARDING_STEP, 0));
    }

    public void setOnboardingStep(int step) {
        prefs.edit().putInt(KEY_ONBOARDING_STEP, Math.max(0, step)).apply();
    }

    public boolean hasSeenReminder(String reminderId) {
        return prefs.getBoolean(keyForReminder(reminderId), false);
    }

    public void markReminderSeen(String reminderId) {
        prefs.edit().putBoolean(keyForReminder(reminderId), true).apply();
    }

    private static String keyForReminder(String reminderId) {
        if (REMINDER_LONG_PRESS.equals(reminderId)) {
            return KEY_REMINDER_LONG_PRESS;
        }
        if (REMINDER_SCREEN_TIME.equals(reminderId)) {
            return KEY_REMINDER_SCREEN_TIME;
        }
        if (REMINDER_CONFUSE.equals(reminderId)) {
            return KEY_REMINDER_CONFUSE;
        }
        if (REMINDER_QUOTA.equals(reminderId)) {
            return KEY_REMINDER_QUOTA;
        }
        return "reminder_" + reminderId;
    }

    public TextSize getTextSize() {
        try {
            return TextSize.valueOf(prefs.getString(KEY_TEXT_SIZE, TextSize.NORMAL.name()));
        } catch (IllegalArgumentException e) {
            return TextSize.NORMAL;
        }
    }

    public void setTextSize(TextSize size) {
        prefs.edit().putString(KEY_TEXT_SIZE, size.name()).apply();
    }

    public float textSizeScale() {
        switch (getTextSize()) {
            case SMALL:
                return 0.9f;
            case LARGE:
                return 1.15f;
            case NORMAL:
            default:
                return 1.0f;
        }
    }

    public FontChoice getFont() {
        try {
            return FontChoice.valueOf(prefs.getString(KEY_FONT, FontChoice.MONOSPACE.name()));
        } catch (IllegalArgumentException e) {
            return FontChoice.MONOSPACE;
        }
    }

    public void setFont(FontChoice font) {
        prefs.edit().putString(KEY_FONT, font.name()).apply();
    }

    public void recordUnlock() {
        prefs.edit().putLong(KEY_LAST_UNLOCK_AT, System.currentTimeMillis()).apply();
    }

    public long getLastUnlockAt() {
        return prefs.getLong(KEY_LAST_UNLOCK_AT, 0L);
    }

    public void recordAppOpened(String packageName, String label) {
        prefs.edit()
                .putString(KEY_LAST_OPENED_PKG, packageName == null ? "" : packageName)
                .putString(KEY_LAST_OPENED_LABEL, label == null ? "" : label)
                .putLong(KEY_LAST_OPENED_AT, System.currentTimeMillis())
                .apply();
    }

    public String getLastOpenedPackage() {
        return prefs.getString(KEY_LAST_OPENED_PKG, "");
    }

    public String getLastOpenedLabel() {
        return prefs.getString(KEY_LAST_OPENED_LABEL, "");
    }

    public long getLastOpenedAt() {
        return prefs.getLong(KEY_LAST_OPENED_AT, 0L);
    }

    public void clearInteractionHistory() {
        prefs.edit()
                .remove(KEY_LAST_UNLOCK_AT)
                .remove(KEY_LAST_OPENED_PKG)
                .remove(KEY_LAST_OPENED_LABEL)
                .remove(KEY_LAST_OPENED_AT)
                .apply();
    }

    private void syncHomeToRoom(List<String> home) {
        executor.execute(() -> {
            AppPolicyDao dao = database.appPolicyDao();
            dao.clearHomeFlags();
            long now = System.currentTimeMillis();
            int order = 0;
            for (String pkg : home) {
                AppPolicyEntity entity = dao.getByPackage(pkg);
                if (entity == null) {
                    entity = AppPolicyEntity.createDefault(pkg, now);
                }
                entity.isHomeApp = true;
                entity.homeOrder = order++;
                entity.updatedAt = now;
                dao.upsert(entity);
            }
        });
    }

    private void syncPinnedToRoom(List<String> pinned) {
        executor.execute(() -> {
            AppPolicyDao dao = database.appPolicyDao();
            dao.clearPinnedFlags();
            long now = System.currentTimeMillis();
            int order = 0;
            for (String pkg : pinned) {
                AppPolicyEntity entity = dao.getByPackage(pkg);
                if (entity == null) {
                    entity = AppPolicyEntity.createDefault(pkg, now);
                }
                entity.isPinned = true;
                entity.pinOrder = order++;
                entity.updatedAt = now;
                dao.upsert(entity);
            }
        });
    }

    private static String encodeList(List<String> packages) {
        if (packages == null || packages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < packages.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(packages.get(i));
        }
        return sb.toString();
    }

    private static List<String> decodeList(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String[] parts = raw.split("\n");
        List<String> out = new ArrayList<>(parts.length);
        out.addAll(Arrays.asList(parts));
        out.removeAll(Collections.singleton(""));
        return out;
    }
}
