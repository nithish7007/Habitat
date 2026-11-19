package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        WidgetStateEntity::class,
        TaskEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        ChallengeEntity::class,
        ChallengeLogEntity::class,
        NoteEntity::class,
        RoutineEntity::class,
        RoutineLogEntity::class,
        GoalEntity::class,
        JournalEntryEntity::class,
        FocusSessionEntity::class,
        WaterLogEntity::class,
        StepLogEntity::class,
        ReminderEntity::class,
        UserProfileEntity::class,
        FinanceTransactionEntity::class,
        SavingsGoalEntity::class,
        NutritionEntryEntity::class,
        LearningTopicEntity::class,
        ClassScheduleEntity::class,
        JobApplicationEntity::class,
        SavedMediaEntity::class,
        MemoryEntity::class,
        NotificationPreferencesEntity::class,
        NotificationLogEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(DashboardWidgetConverters::class)
abstract class LifeOsDatabase : RoomDatabase() {
    abstract fun dao(): LifeOsDao
    abstract fun dashboardWidgetDao(): DashboardWidgetDao
    abstract fun v3Dao(): LifeOsV3Dao

    companion object {
        @Volatile
        private var INSTANCE: LifeOsDatabase? = null

        fun getInstance(context: Context): LifeOsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LifeOsDatabase::class.java,
                    "life_os_db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
