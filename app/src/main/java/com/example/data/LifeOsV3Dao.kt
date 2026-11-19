package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeOsV3Dao {
    // User Profile
    @Query("SELECT * FROM user_profiles WHERE userId = :uid LIMIT 1")
    fun observeUserProfile(uid: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE userId = :uid LIMIT 1")
    suspend fun getUserProfileOnce(uid: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfileEntity)

    // Finance Transactions
    @Query("SELECT * FROM finance_transactions WHERE userId = :uid ORDER BY dateIso DESC, createdAtMs DESC")
    fun observeTransactions(uid: String): Flow<List<FinanceTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(item: FinanceTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(items: List<FinanceTransactionEntity>)

    @Query("DELETE FROM finance_transactions WHERE userId = :uid AND id = :id")
    suspend fun deleteTransaction(uid: String, id: String)

    // Savings Goals
    @Query("SELECT * FROM savings_goals WHERE userId = :uid ORDER BY createdAtMs DESC")
    fun observeSavingsGoals(uid: String): Flow<List<SavingsGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavingsGoal(item: SavingsGoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavingsGoals(items: List<SavingsGoalEntity>)

    @Query("DELETE FROM savings_goals WHERE userId = :uid AND id = :id")
    suspend fun deleteSavingsGoal(uid: String, id: String)

    // Nutrition Entries
    @Query("SELECT * FROM nutrition_entries WHERE userId = :uid ORDER BY dateIso DESC, createdAtMs DESC")
    fun observeNutritionEntries(uid: String): Flow<List<NutritionEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNutritionEntry(item: NutritionEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNutritionEntries(items: List<NutritionEntryEntity>)

    @Query("DELETE FROM nutrition_entries WHERE userId = :uid AND id = :id")
    suspend fun deleteNutritionEntry(uid: String, id: String)

    // Learning Topics
    @Query("SELECT * FROM learning_topics WHERE userId = :uid ORDER BY category ASC, level ASC")
    fun observeLearningTopics(uid: String): Flow<List<LearningTopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLearningTopic(item: LearningTopicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLearningTopics(items: List<LearningTopicEntity>)

    // Class Schedules
    @Query("SELECT * FROM class_schedules WHERE userId = :uid ORDER BY dayOfWeek ASC, startTime ASC")
    fun observeClassSchedules(uid: String): Flow<List<ClassScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClassSchedule(item: ClassScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertClassSchedules(items: List<ClassScheduleEntity>)

    @Query("DELETE FROM class_schedules WHERE userId = :uid AND id = :id")
    suspend fun deleteClassSchedule(uid: String, id: String)

    // Job Applications & Interviews
    @Query("SELECT * FROM job_applications WHERE userId = :uid ORDER BY updatedAtMs DESC")
    fun observeJobApplications(uid: String): Flow<List<JobApplicationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJobApplication(item: JobApplicationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJobApplications(items: List<JobApplicationEntity>)

    @Query("DELETE FROM job_applications WHERE userId = :uid AND id = :id")
    suspend fun deleteJobApplication(uid: String, id: String)

    // Saved Media (News bookmarks, Watch Later videos, Favorite quotes)
    @Query("SELECT * FROM saved_media WHERE userId = :uid ORDER BY createdAtMs DESC")
    fun observeSavedMedia(uid: String): Flow<List<SavedMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedMedia(item: SavedMediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedMediaList(items: List<SavedMediaEntity>)

    @Query("DELETE FROM saved_media WHERE userId = :uid AND id = :id")
    suspend fun deleteSavedMedia(uid: String, id: String)

    // Private Memories (strictly separate from Notes)
    @Query("SELECT * FROM memories WHERE userId = :uid ORDER BY memoryDateIso DESC, createdAtMs DESC")
    fun observeMemories(uid: String): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemory(item: MemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemories(items: List<MemoryEntity>)

    @Query("DELETE FROM memories WHERE userId = :uid AND id = :id")
    suspend fun deleteMemory(uid: String, id: String)

    // Smart Notification Preferences
    @Query("SELECT * FROM notification_preferences WHERE userId = :uid LIMIT 1")
    fun observeNotificationPrefs(uid: String): Flow<NotificationPreferencesEntity?>

    @Query("SELECT * FROM notification_preferences WHERE userId = :uid LIMIT 1")
    suspend fun getNotificationPrefsOnce(uid: String): NotificationPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotificationPrefs(prefs: NotificationPreferencesEntity)

    // Notification History & Deduplication
    @Query("SELECT * FROM notification_logs WHERE userId = :uid ORDER BY createdAtMs DESC LIMIT 120")
    fun observeNotificationLogs(uid: String): Flow<List<NotificationLogEntity>>

    @Query("SELECT COUNT(*) FROM notification_logs WHERE userId = :uid AND dedupeKey = :dedupeKey")
    suspend fun countByDedupeKey(uid: String, dedupeKey: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationLog(log: NotificationLogEntity)

    @Query("DELETE FROM notification_logs WHERE userId = :uid")
    suspend fun clearNotificationLogs(uid: String)

    // Clear All V3 Tables for User
    @Query("DELETE FROM user_profiles WHERE userId = :uid")
    suspend fun clearUserProfile(uid: String)

    @Query("DELETE FROM finance_transactions WHERE userId = :uid")
    suspend fun clearTransactions(uid: String)

    @Query("DELETE FROM savings_goals WHERE userId = :uid")
    suspend fun clearSavingsGoals(uid: String)

    @Query("DELETE FROM nutrition_entries WHERE userId = :uid")
    suspend fun clearNutritionEntries(uid: String)

    @Query("DELETE FROM learning_topics WHERE userId = :uid")
    suspend fun clearLearningTopics(uid: String)

    @Query("DELETE FROM class_schedules WHERE userId = :uid")
    suspend fun clearClassSchedules(uid: String)

    @Query("DELETE FROM job_applications WHERE userId = :uid")
    suspend fun clearJobApplications(uid: String)

    @Query("DELETE FROM saved_media WHERE userId = :uid")
    suspend fun clearSavedMedia(uid: String)

    @Query("DELETE FROM memories WHERE userId = :uid")
    suspend fun clearMemories(uid: String)

    @Query("DELETE FROM notification_preferences WHERE userId = :uid")
    suspend fun clearNotificationPrefs(uid: String)

    suspend fun clearAllV3ForUser(uid: String) {
        clearUserProfile(uid)
        clearTransactions(uid)
        clearSavingsGoals(uid)
        clearNutritionEntries(uid)
        clearLearningTopics(uid)
        clearClassSchedules(uid)
        clearJobApplications(uid)
        clearSavedMedia(uid)
        clearMemories(uid)
        clearNotificationPrefs(uid)
        clearNotificationLogs(uid)
    }
}
