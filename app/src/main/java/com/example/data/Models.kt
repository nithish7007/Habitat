package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class ThemeMode { SYSTEM, DARK, LIGHT }
// Dashboard widget types, WidgetState, and WidgetStateEntity are defined in DashboardWidgetModels.kt.

data class UserPreferencesModel(
    val enabledWidgets: List<DashboardWidgetType> = DashboardWidgetType.DEFAULT_ORDER,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val weightTasks: Int = 30,
    val weightHabits: Int = 30,
    val weightChallenges: Int = 20,
    val weightWater: Int = 10,
    val weightSteps: Int = 10,
    val waterTargetMl: Int = 2500,
    val stepsTarget: Int = 8000,
    val focusTargetMinutes: Int = 90,
    val quietHoursStart: Int = 22,
    val quietHoursEnd: Int = 7
)

enum class TaskPriority { HIGH, MEDIUM, LOW }
enum class TaskStatus { PENDING, IN_PROGRESS, COMPLETED }
enum class RecurrenceRule { NONE, DAILY, WEEKLY, MONTHLY }
enum class HabitScheduleType { DAILY, WEEKLY }
enum class HabitAutoSource(val label: String) {
    NONE("Manual"),
    STEPS("Auto: Steps"),
    WATER("Auto: Water"),
    FOCUS_MINUTES("Auto: Focus");

    companion object {
        fun fromNameOrDefault(raw: String): HabitAutoSource =
            entries.firstOrNull { it.name == raw } ?: NONE
    }
}
enum class ChallengeDayStatus { SUCCESS, FAILED, NOT_RECORDED }
enum class RoutineType { MORNING, STUDY, EXERCISE, SLEEP, CUSTOM }
enum class GoalStatus { ACTIVE, PAUSED, COMPLETED, FAILED }
enum class FocusMode { POMODORO, CUSTOM, STOPWATCH, COUNTDOWN }

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val priority: String = TaskPriority.MEDIUM.name,
    val status: String = TaskStatus.PENDING.name,
    val dueDateIso: String = DateUtils.todayIso(),
    val dueTimeText: String = "",
    val category: String = "Personal",
    val labelsCsv: String = "",
    val recurrence: String = RecurrenceRule.NONE.name,
    val timeBlockStart: String = "",
    val timeBlockEnd: String = "",
    val completedDateIso: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "",
    val scheduleType: String = HabitScheduleType.DAILY.name,
    val weekdaysMask: Int = 127,
    val startDateIso: String = DateUtils.todayIso(),
    val endDateIso: String = "",
    val reminderTime: String = "",
    val targetValue: Int = 1,
    val unit: String = "Check-in",
    val autoSource: String = HabitAutoSource.NONE.name,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "habit_logs")
data class HabitLogEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val habitId: String = "",
    val dateIso: String = DateUtils.todayIso(),
    val completed: Boolean = true,
    val progressValue: Int = 1,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val startDateIso: String = DateUtils.todayIso(),
    val endDateIso: String = DateUtils.offsetDateIso(DateUtils.todayIso(), 30),
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "challenge_logs")
data class ChallengeLogEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val challengeId: String = "",
    val dateIso: String = DateUtils.todayIso(),
    val status: String = ChallengeDayStatus.NOT_RECORDED.name,
    val auditHistory: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val folder: String = "Quick Notes",
    val labelsCsv: String = "",
    val pinned: Boolean = false,
    val favorite: Boolean = false,
    val isCodeNote: Boolean = false,
    val archived: Boolean = false,
    val attachmentUri: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "",
    val routineType: String = RoutineType.MORNING.name,
    val stepsCsv: String = "",
    val recurrence: String = "DAILY",
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "routine_logs")
data class RoutineLogEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val routineId: String = "",
    val dateIso: String = DateUtils.todayIso(),
    val completedStepsCsv: String = "",
    val completedAll: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val horizon: String = "30_DAYS",
    val priority: String = TaskPriority.HIGH.name,
    val status: String = GoalStatus.ACTIVE.name,
    val targetDateIso: String = DateUtils.offsetDateIso(DateUtils.todayIso(), 30),
    val progressPercent: Int = 0,
    val milestonesCsv: String = "",
    val completedMilestonesCsv: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val dateIso: String = DateUtils.todayIso(),
    val highlights: String = "",
    val learned: String = "",
    val wentWell: String = "",
    val wentWrong: String = "",
    val tomorrowPlan: String = "",
    val gratitude: String = "",
    val mood: String = "",
    val privateEntry: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val dateIso: String = DateUtils.todayIso(),
    val mode: String = FocusMode.POMODORO.name,
    val category: String = "DSA",
    val plannedMinutes: Int = 25,
    val actualSeconds: Int = 0,
    val distractionsCount: Int = 0,
    val notes: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "water_logs")
data class WaterLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val dateIso: String = DateUtils.todayIso(),
    val amountMl: Int = 250,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "step_logs")
data class StepLogEntity(
    @PrimaryKey val id: String = "",
    val userId: String = "",
    val dateIso: String = DateUtils.todayIso(),
    val steps: Int = 0,
    val source: String = "MANUAL",
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String = "",
    val moduleType: String = "TASK",
    val scheduledEpochMs: Long = System.currentTimeMillis() + 3_600_000L,
    val recurrence: String = "NONE",
    val enabled: Boolean = true,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

data class PersistedTimerState(
    val mode: FocusMode = FocusMode.POMODORO,
    val category: String = "DSA",
    val plannedMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val isBreakPhase: Boolean = false,
    val running: Boolean = false,
    val startedAtEpochMs: Long = 0L,
    val accumulatedElapsedSec: Int = 0,
    val distractionsCount: Int = 0,
    val sessionNotes: String = ""
)
