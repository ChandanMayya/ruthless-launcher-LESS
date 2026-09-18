package com.ruthless.less.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.ruthless.less.database.entities.ConfusePlacementEntity;

import java.util.List;

@Dao
public interface ConfusePlacementDao {

    @Query("SELECT * FROM confuse_placements WHERE sessionId = :sessionId")
    List<ConfusePlacementEntity> getForSession(String sessionId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ConfusePlacementEntity entity);

    @Query("DELETE FROM confuse_placements WHERE sessionId = :sessionId")
    void clearSession(String sessionId);

    @Query("DELETE FROM confuse_placements")
    void clearAll();
}
