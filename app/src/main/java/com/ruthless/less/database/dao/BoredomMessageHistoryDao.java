package com.ruthless.less.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.ruthless.less.database.entities.BoredomMessageHistoryEntity;

import java.util.List;

@Dao
public interface BoredomMessageHistoryDao {

    @Insert
    void insert(BoredomMessageHistoryEntity entity);

    @Query("SELECT messageId FROM boredom_message_history ORDER BY displayedAt DESC LIMIT :limit")
    List<Integer> recentMessageIds(int limit);

    @Query("DELETE FROM boredom_message_history WHERE displayedAt < :before")
    void pruneBefore(long before);

    @Query("DELETE FROM boredom_message_history")
    void deleteAll();
}
