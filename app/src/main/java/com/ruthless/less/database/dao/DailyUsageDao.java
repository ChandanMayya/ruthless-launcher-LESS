package com.ruthless.less.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.ruthless.less.database.entities.DailyUsageEntity;

import java.util.List;

@Dao
public interface DailyUsageDao {

    @Query("SELECT * FROM daily_usage WHERE date = :date ORDER BY foregroundMinutes DESC")
    List<DailyUsageEntity> getForDate(String date);

    @Query("SELECT * FROM daily_usage WHERE packageName = :packageName AND date = :date LIMIT 1")
    DailyUsageEntity get(String packageName, String date);

    @Query("SELECT * FROM daily_usage WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    List<DailyUsageEntity> getBetween(String startDate, String endDate);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(DailyUsageEntity entity);

    @Query("DELETE FROM daily_usage")
    void deleteAll();
}
