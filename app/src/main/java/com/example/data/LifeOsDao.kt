package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeOsDao {
    // Tasks
    @Query("SELECT * FROM tasks WHERE userId = :uid ORDER BY dueDateIso ASC, createdAtMs DESC")
    fun observeTasks(uid: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE userId = :uid AND id = :id")
    suspend fun deleteTask(uid: String, id: String)

    // Habits & Logs
    @Query("SELECT * FROM habits WHERE userId = :uid ORDER BY createdAtMs ASC")
    fun observeHabits(uid: String): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabits(habits: List<HabitEntity>)

    @Query("DELETE FROM habits WHERE userId = :uid AND id = :id")
    suspend fun deleteHabit(uid: String, id: String)

    @Query("SELECT * FROM habit_logs WHERE userId = :uid ORDER BY dateIso DESC")
    fun observeHabitLogs(uid: String): Flow<List<HabitLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabitLog(log: HabitLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabitLogs(logs: List<HabitLogEntity>)

    @Query("DELETE FROM habit_logs WHERE userId = :uid AND id = :id")
    suspend fun deleteHabitLog(uid: String, id: String)

    // Challenges & Logs
    @Query("SELECT * FROM challenges WHERE userId = :uid ORDER BY createdAtMs DESC")
    fun observeChallenges(uid: String): Flow<List<ChallengeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChallenge(challenge: ChallengeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChallenges(challenges: List<ChallengeEntity>)

    @Query("DELETE FROM challenges WHERE userId = :uid AND id = :id")
    suspend fun deleteChallenge(uid: String, id: String)

    @Query("SELECT * FROM challenge_logs WHERE userId = :uid ORDER BY dateIso DESC")
    fun observeChallengeLogs(uid: String): Flow<List<ChallengeLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChallengeLog(log: ChallengeLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChallengeLogs(logs: List<ChallengeLogEntity>)

    // Notes
    @Query("SELECT * FROM notes WHERE userId = :uid ORDER BY pinned DESC, updatedAtMs DESC")
    fun observeNotes(uid: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotes(notes: List<NoteEntity>)

    @Query("DELETE FROM notes WHERE userId = :uid AND id = :id")
    suspend fun deleteNote(uid: String, id: String)

    // Routines & Logs
    @Query("SELECT * FROM routines WHERE userId = :uid ORDER BY createdAtMs ASC")
    fun observeRoutines(uid: String): Flow<List<RoutineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutine(routine: RoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutines(routines: List<RoutineEntity>)

    @Query("DELETE FROM routines WHERE userId = :uid AND id = :id")
    suspend fun deleteRoutine(uid: String, id: String)

    @Query("SELECT * FROM routine_logs WHERE userId = :uid ORDER BY dateIso DESC")
    fun observeRoutineLogs(uid: String): Flow<List<RoutineLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutineLog(log: RoutineLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRoutineLogs(logs: List<RoutineLogEntity>)

    // Goals
    @Query("SELECT * FROM goals WHERE userId = :uid ORDER BY createdAtMs DESC")
    fun observeGoals(uid: String): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: GoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoals(goals: List<GoalEntity>)

    @Query("DELETE FROM goals WHERE userId = :uid AND id = :id")
    suspend fun deleteGoal(uid: String, id: String)

    // Journal
    @Query("SELECT * FROM journal_entries WHERE userId = :uid ORDER BY dateIso DESC")
    fun observeJournalEntries(uid: String): Flow<List<JournalEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJournalEntry(entry: JournalEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJournalEntries(entries: List<JournalEntryEntity>)

    @Query("DELETE FROM journal_entries WHERE userId = :uid AND id = :id")
    suspend fun deleteJournalEntry(uid: String, id: String)

    // Focus Sessions
    @Query("SELECT * FROM focus_sessions WHERE userId = :uid ORDER BY createdAtMs DESC")
    fun observeFocusSessions(uid: String): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFocusSession(session: FocusSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFocusSessions(sessions: List<FocusSessionEntity>)

    @Query("DELETE FROM focus_sessions WHERE userId = :uid AND id = :id")
    suspend fun deleteFocusSession(uid: String, id: String)

    // Water Logs
    @Query("SELECT * FROM water_logs WHERE userId = :uid ORDER BY createdAtMs DESC")
    fun observeWaterLogs(uid: String): Flow<List<WaterLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWaterLog(log: WaterLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWaterLogs(logs: List<WaterLogEntity>)

    @Query("DELETE FROM water_logs WHERE userId = :uid AND id = :id")
    suspend fun deleteWaterLog(uid: String, id: String)

    // Step Logs
    @Query("SELECT * FROM step_logs WHERE userId = :uid ORDER BY dateIso DESC")
    fun observeStepLogs(uid: String): Flow<List<StepLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStepLog(log: StepLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStepLogs(logs: List<StepLogEntity>)

    // Reminders
    @Query("SELECT * FROM reminders WHERE userId = :uid ORDER BY scheduledEpochMs ASC")
    fun observeReminders(uid: String): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminders(reminders: List<ReminderEntity>)

    @Query("DELETE FROM reminders WHERE userId = :uid AND id = :id")
    suspend fun deleteReminder(uid: String, id: String)

    // Clear all local data for a user (used for Replace restore or account deletion)
    @Query("DELETE FROM tasks WHERE userId = :uid") suspend fun clearTasks(uid: String)
    @Query("DELETE FROM habits WHERE userId = :uid") suspend fun clearHabits(uid: String)
    @Query("DELETE FROM habit_logs WHERE userId = :uid") suspend fun clearHabitLogs(uid: String)
    @Query("DELETE FROM challenges WHERE userId = :uid") suspend fun clearChallenges(uid: String)
    @Query("DELETE FROM challenge_logs WHERE userId = :uid") suspend fun clearChallengeLogs(uid: String)
    @Query("DELETE FROM notes WHERE userId = :uid") suspend fun clearNotes(uid: String)
    @Query("DELETE FROM routines WHERE userId = :uid") suspend fun clearRoutines(uid: String)
    @Query("DELETE FROM routine_logs WHERE userId = :uid") suspend fun clearRoutineLogs(uid: String)
    @Query("DELETE FROM goals WHERE userId = :uid") suspend fun clearGoals(uid: String)
    @Query("DELETE FROM journal_entries WHERE userId = :uid") suspend fun clearJournalEntries(uid: String)
    @Query("DELETE FROM focus_sessions WHERE userId = :uid") suspend fun clearFocusSessions(uid: String)
    @Query("DELETE FROM water_logs WHERE userId = :uid") suspend fun clearWaterLogs(uid: String)
    @Query("DELETE FROM step_logs WHERE userId = :uid") suspend fun clearStepLogs(uid: String)
    @Query("DELETE FROM reminders WHERE userId = :uid") suspend fun clearReminders(uid: String)
}
