package com.ruthless.less.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "confuse_placements")
public class ConfusePlacementEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String packageName = "";

    @NonNull
    public String sessionId = "";

    public int targetScroll;
    public int targetPosition;
    public int previousPosition;
}
