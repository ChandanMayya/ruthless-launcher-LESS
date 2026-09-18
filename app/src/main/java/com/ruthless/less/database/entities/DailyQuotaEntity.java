package com.ruthless.less.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "daily_quotas", primaryKeys = {"packageName", "date"})
public class DailyQuotaEntity {

    @NonNull
    public String packageName;

    @NonNull
    public String date;

    public int limitMinutes;
    public boolean extensionUsed;
    public int extensionMinutes;
    public boolean quotaExhausted;
}
