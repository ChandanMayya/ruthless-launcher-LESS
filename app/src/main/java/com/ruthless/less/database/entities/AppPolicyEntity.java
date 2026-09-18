package com.ruthless.less.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Per-package policy. Survives uninstall/reinstall of the target app
 * (packageName is the stable key). Android usage history may still reset
 * after uninstall — LESS policy does not.
 */
@Entity(tableName = "app_policies")
public class AppPolicyEntity {

    @PrimaryKey
    @NonNull
    public String packageName;

    public int dailyLimitMinutes; // 0 = none
    public boolean isPinned;
    public int pinOrder;
    public boolean isHomeApp;
    public int homeOrder;
    public boolean isConfused;
    public boolean launchConfirmationEnabled;
    public String launchDelayMode; // OFF, RANGE, CUSTOM
    public int launchDelayMinMs;
    public int launchDelayMaxMs;
    public boolean excludedFromScreenTime; // false = monitoring on (default)
    /** When true, launch asks how long the session will last and enforces a countdown. */
    public boolean sessionTimerEnabled;
    public long createdAt;
    public long updatedAt;

    @NonNull
    public static AppPolicyEntity createDefault(@NonNull String packageName, long now) {
        AppPolicyEntity e = new AppPolicyEntity();
        e.packageName = packageName;
        e.dailyLimitMinutes = 0;
        e.isPinned = false;
        e.pinOrder = 0;
        e.isHomeApp = false;
        e.homeOrder = 0;
        e.isConfused = false;
        e.launchConfirmationEnabled = false;
        e.launchDelayMode = "OFF";
        e.launchDelayMinMs = 0;
        e.launchDelayMaxMs = 0;
        e.excludedFromScreenTime = false;
        e.sessionTimerEnabled = false;
        e.createdAt = now;
        e.updatedAt = now;
        return e;
    }
}
