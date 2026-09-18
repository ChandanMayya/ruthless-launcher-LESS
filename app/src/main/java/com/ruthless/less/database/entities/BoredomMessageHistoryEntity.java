package com.ruthless.less.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "boredom_message_history")
public class BoredomMessageHistoryEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public int messageId;
    public long displayedAt;
}
