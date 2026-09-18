package com.ruthless.less.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "daily_usage", primaryKeys = {"packageName", "date"})
public class DailyUsageEntity {

    @NonNull
    public String packageName;

    /** Local calendar day as yyyy-MM-dd. */
    @NonNull
    public String date;

    public int foregroundMinutes;
    public int launchCount;
}
