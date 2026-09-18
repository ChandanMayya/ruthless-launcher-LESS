package com.ruthless.less.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.ruthless.less.database.dao.AppPolicyDao;
import com.ruthless.less.database.dao.BoredomMessageHistoryDao;
import com.ruthless.less.database.dao.ConfusePlacementDao;
import com.ruthless.less.database.dao.DailyQuotaDao;
import com.ruthless.less.database.dao.DailyUsageDao;
import com.ruthless.less.database.entities.AppPolicyEntity;
import com.ruthless.less.database.entities.BoredomMessageHistoryEntity;
import com.ruthless.less.database.entities.ConfusePlacementEntity;
import com.ruthless.less.database.entities.DailyQuotaEntity;
import com.ruthless.less.database.entities.DailyUsageEntity;

@Database(
        entities = {
                AppPolicyEntity.class,
                DailyUsageEntity.class,
                DailyQuotaEntity.class,
                ConfusePlacementEntity.class,
                BoredomMessageHistoryEntity.class
        },
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract AppPolicyDao appPolicyDao();

    public abstract DailyUsageDao dailyUsageDao();

    public abstract DailyQuotaDao dailyQuotaDao();

    public abstract ConfusePlacementDao confusePlacementDao();

    public abstract BoredomMessageHistoryDao boredomMessageHistoryDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "less.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
