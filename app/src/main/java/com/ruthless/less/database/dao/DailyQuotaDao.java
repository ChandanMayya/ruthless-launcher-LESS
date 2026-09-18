package com.ruthless.less.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.ruthless.less.database.entities.DailyQuotaEntity;

@Dao
public interface DailyQuotaDao {

    @Query("SELECT * FROM daily_quotas WHERE packageName = :packageName AND date = :date LIMIT 1")
    DailyQuotaEntity get(String packageName, String date);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(DailyQuotaEntity entity);

    @Query("DELETE FROM daily_quotas")
    void deleteAll();
}
