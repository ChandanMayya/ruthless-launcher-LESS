package com.ruthless.less.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.ruthless.less.database.entities.AppPolicyEntity;

import java.util.List;

@Dao
public interface AppPolicyDao {

    @Query("SELECT * FROM app_policies WHERE packageName = :packageName LIMIT 1")
    AppPolicyEntity getByPackage(String packageName);

    @Query("SELECT * FROM app_policies")
    List<AppPolicyEntity> getAll();

    @Query("SELECT * FROM app_policies WHERE isPinned = 1 ORDER BY pinOrder ASC")
    List<AppPolicyEntity> getPinned();

    @Query("SELECT * FROM app_policies WHERE isHomeApp = 1 ORDER BY homeOrder ASC")
    List<AppPolicyEntity> getHomeApps();

    @Query("SELECT * FROM app_policies WHERE isConfused = 1")
    List<AppPolicyEntity> getConfused();

    @Query("SELECT * FROM app_policies WHERE dailyLimitMinutes > 0 ORDER BY packageName ASC")
    List<AppPolicyEntity> getWithLimits();

    @Query("SELECT * FROM app_policies WHERE launchDelayMode IS NOT NULL AND launchDelayMode != 'OFF'")
    List<AppPolicyEntity> getWithLaunchDelay();

    @Query("SELECT * FROM app_policies WHERE launchConfirmationEnabled = 1")
    List<AppPolicyEntity> getWithConfirmation();

    @Query("SELECT * FROM app_policies WHERE excludedFromScreenTime = 1")
    List<AppPolicyEntity> getExcludedFromScreenTime();

    @Query("SELECT * FROM app_policies WHERE sessionTimerEnabled = 1")
    List<AppPolicyEntity> getWithSessionTimer();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(AppPolicyEntity entity);

    @Query("UPDATE app_policies SET isHomeApp = 0, homeOrder = 0")
    void clearHomeFlags();

    @Query("UPDATE app_policies SET isPinned = 0, pinOrder = 0")
    void clearPinnedFlags();

    @Query("DELETE FROM app_policies")
    void deleteAll();
}
