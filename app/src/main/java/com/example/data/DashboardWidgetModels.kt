package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

/**
 * All 9 supported LIFE OS customizable dashboard widgets.
 */
enum class DashboardWidgetType(
    val title: String,
    val subtitle: String,
    val category: String
) {
    DAILY_PRODUCTIVITY_SCORE(
        title = "Daily Productivity Score",
        subtitle = "Weighted score ring with formula breakdown & yesterday delta",
        category = "Overview"
    ),
    MOTIVATION_GOAL(
        title = "Motivation Goal",
        subtitle = "Daily changing English motivational quotes with author attribution",
        category = "Mindset"
    ),
    TASKS_SUMMARY(
        title = "Completed and Pending Tasks",
        subtitle = "Quick check-off, pending count, and overdue task alerts",
        category = "Planning"
    ),
    HABIT_PROGRESS(
        title = "Habit Progress",
        subtitle = "Daily streaks and one-tap habit check-ins",
        category = "Tracking"
    ),
    ACTIVE_CHALLENGES(
        title = "Active Challenges",
        subtitle = "Strict success/failed tracking with days remaining",
        category = "Tracking"
    ),
    WATER_INTAKE(
        title = "Water Intake",
        subtitle = "Hydration progress bar with +250 ml and +500 ml quick log",
        category = "Wellness"
    ),
    AUTOMATIC_STEPS(
        title = "Automatic Steps",
        subtitle = "Daily step goal, Health Connect status & manual fallback",
        category = "Wellness"
    ),
    FOCUS_TIME(
        title = "Focus Time",
        subtitle = "Deep work minutes, study categories & quick start timer",
        category = "Focus"
    ),
    UPCOMING_REMINDERS(
        title = "Upcoming Reminders",
        subtitle = "Next WorkManager smart alerts with snooze controls",
        category = "Planning"
    ),
    GOAL_PROGRESS(
        title = "Goal Progress",
        subtitle = "Multi-horizon milestone bars and target deadlines",
        category = "Strategy"
    );

    companion object {
        val DEFAULT_ORDER: List<DashboardWidgetType> = listOf(
            DAILY_PRODUCTIVITY_SCORE,
            MOTIVATION_GOAL,
            TASKS_SUMMARY,
            HABIT_PROGRESS,
            ACTIVE_CHALLENGES,
            WATER_INTAKE,
            AUTOMATIC_STEPS,
            FOCUS_TIME,
            UPCOMING_REMINDERS,
            GOAL_PROGRESS
        )

        fun fromNameOrNull(raw: String): DashboardWidgetType? =
            entries.firstOrNull { it.name == raw }
    }
}

/**
 * Visual density / span preference for a customizable dashboard widget card.
 */
enum class WidgetSpanSize {
    COMPACT,
    STANDARD,
    EXPANDED;

    companion object {
        fun fromNameOrDefault(raw: String): WidgetSpanSize =
            entries.firstOrNull { it.name == raw } ?: STANDARD
    }
}

/**
 * Domain & UI model representing a single customizable dashboard widget instance
 * and its persisted user preferences (visibility, display position, type, span, and config).
 */
data class WidgetState(
    val id: String,
    val userId: String,
    val type: DashboardWidgetType,
    val isVisible: Boolean = true,
    val position: Int = 0,
    val isPinned: Boolean = false,
    val spanSize: WidgetSpanSize = WidgetSpanSize.STANDARD,
    val customConfigJson: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    fun toEntity(): WidgetStateEntity = WidgetStateEntity(
        id = id,
        userId = userId,
        widgetType = type,
        isVisible = isVisible,
        position = position,
        isPinned = isPinned,
        spanSize = spanSize,
        customConfigJson = customConfigJson,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs
    )

    companion object {
        fun instanceId(userId: String, type: DashboardWidgetType): String =
            "${userId}_${type.name}"

        /**
         * Creates the default ordered list of all 9 LIFE OS dashboard widget instances for a user.
         */
        fun defaultWidgetsForUser(
            userId: String,
            nowMs: Long = System.currentTimeMillis()
        ): List<WidgetState> = DashboardWidgetType.DEFAULT_ORDER.mapIndexed { index, widgetType ->
            WidgetState(
                id = instanceId(userId, widgetType),
                userId = userId,
                type = widgetType,
                isVisible = true,
                position = index,
                isPinned = widgetType == DashboardWidgetType.DAILY_PRODUCTIVITY_SCORE,
                spanSize = WidgetSpanSize.STANDARD,
                customConfigJson = "",
                createdAtMs = nowMs,
                updatedAtMs = nowMs
            )
        }

        /**
         * Ensures contiguous 0-based positions: visible widgets come first in their current order,
         * followed by hidden widgets in their current order.
         */
        fun normalizePositions(
            states: List<WidgetState>,
            updatedAtMs: Long = System.currentTimeMillis()
        ): List<WidgetState> {
            val visible = states.filter { it.isVisible }.sortedBy { it.position }
            val hidden = states.filterNot { it.isVisible }.sortedBy { it.position }
            return (visible + hidden).mapIndexed { idx, item ->
                if (item.position == idx) item else item.copy(position = idx, updatedAtMs = updatedAtMs)
            }
        }

        /**
         * Reconciles a complete list of 9 [WidgetState] instances with a list of visible ordered
         * [DashboardWidgetType] values (e.g., from DataStore or Cloud Firestore sync), preserving
         * per-widget metadata such as [isPinned], [spanSize], and [customConfigJson].
         */
        fun reconcileWithEnabledOrder(
            userId: String,
            enabledOrder: List<DashboardWidgetType>,
            existing: List<WidgetState> = emptyList(),
            nowMs: Long = System.currentTimeMillis()
        ): List<WidgetState> {
            val existingByType = existing.associateBy { it.type }
            val distinctEnabled = enabledOrder.distinct()
            val hiddenTypes = DashboardWidgetType.DEFAULT_ORDER.filterNot { it in distinctEnabled }
            val orderedAll = distinctEnabled + hiddenTypes

            return orderedAll.mapIndexed { index, type ->
                val prev = existingByType[type]
                val visible = type in distinctEnabled
                if (prev != null) {
                    prev.copy(
                        userId = userId,
                        isVisible = visible,
                        position = index,
                        updatedAtMs = nowMs
                    )
                } else {
                    WidgetState(
                        id = instanceId(userId, type),
                        userId = userId,
                        type = type,
                        isVisible = visible,
                        position = index,
                        isPinned = type == DashboardWidgetType.DAILY_PRODUCTIVITY_SCORE,
                        spanSize = WidgetSpanSize.STANDARD,
                        createdAtMs = nowMs,
                        updatedAtMs = nowMs
                    )
                }
            }
        }
    }
}

/**
 * Room Entity persisting each user's dashboard widget instance configuration in SQLite.
 */
@Entity(
    tableName = "dashboard_widgets",
    indices = [
        Index(value = ["userId", "position"]),
        Index(value = ["userId", "widgetType"], unique = true)
    ]
)
data class WidgetStateEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val widgetType: DashboardWidgetType,
    val isVisible: Boolean = true,
    val position: Int = 0,
    val isPinned: Boolean = false,
    val spanSize: WidgetSpanSize = WidgetSpanSize.STANDARD,
    val customConfigJson: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    fun toWidgetState(): WidgetState = WidgetState(
        id = id,
        userId = userId,
        type = widgetType,
        isVisible = isVisible,
        position = position,
        isPinned = isPinned,
        spanSize = spanSize,
        customConfigJson = customConfigJson,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs
    )
}

/**
 * Room TypeConverters for strongly-typed dashboard widget enums.
 */
class DashboardWidgetConverters {
    @TypeConverter
    fun fromWidgetType(type: DashboardWidgetType): String = type.name

    @TypeConverter
    fun toWidgetType(raw: String): DashboardWidgetType =
        DashboardWidgetType.fromNameOrNull(raw) ?: DashboardWidgetType.DAILY_PRODUCTIVITY_SCORE

    @TypeConverter
    fun fromWidgetSpanSize(size: WidgetSpanSize): String = size.name

    @TypeConverter
    fun toWidgetSpanSize(raw: String): WidgetSpanSize =
        WidgetSpanSize.fromNameOrDefault(raw)
}

/**
 * Typed snapshot data models consumed by each customizable dashboard widget card.
 */
sealed interface DashboardWidgetPayload {
    val widgetType: DashboardWidgetType

    data class ProductivityScoreData(
        val dateIso: String,
        val scorePercent: Int?,
        val yesterdayDelta: Int?,
        val activeModuleCount: Int
    ) : DashboardWidgetPayload {
        override val widgetType = DashboardWidgetType.DAILY_PRODUCTIVITY_SCORE
    }

    data class MotivationGoalData(
        val dateIso: String,
        val quoteText: String,
        val author: String,
        val authorRole: String,
        val theme: String
    ) : DashboardWidgetPayload {
        override val widgetType = DashboardWidgetType.MOTIVATION_GOAL
    }

    data class TasksSummaryData(
        val completedCount: Int,
        val pendingCount: Int,
        val overdueCount: Int
    ) : DashboardWidgetPayload {
        override val widgetType = DashboardWidgetType.TASKS_SUMMARY
    }

    data class HabitProgressData(
        val completedToday: Int,
        val scheduledToday: Int,
        val bestActiveStreak: Int
    ) : DashboardWidgetPayload {
        override val widgetType = DashboardWidgetType.HABIT_PROGRESS
    }

    data class ActiveChallengesData(
        val activeCount: Int,
        val passedToday: Int,
        val failedToday: Int
    ) : DashboardWidgetPayload {
        override val widgetType = DashboardWidgetType.ACTIVE_CHALLENGES
    }

    data class WaterIntakeData(
        val consumedMl: Int,
        val targetMl: Int
    ) : DashboardWidgetPayload {
        override val widgetType = DashboardWidgetType.WATER_INTAKE
    }

    data class AutomaticStepsData(
        val steps: Int,
        val targetSteps: Int,
        val source: String
    ) : DashboardWidgetPayload {
        override val widgetType = DashboardWidgetType.AUTOMATIC_STEPS
    }

    data class FocusTimeData(
        val focusedMinutesToday: Int,
        val targetMinutes: Int,
        val sessionsCount: Int
    ) : DashboardWidgetPayload {
        override val widgetType = DashboardWidgetType.FOCUS_TIME
    }

    data class UpcomingRemindersData(
        val enabledCount: Int,
        val nextReminderTitle: String?,
        val nextScheduledEpochMs: Long?
    ) : DashboardWidgetPayload {
        override val widgetType = DashboardWidgetType.UPCOMING_REMINDERS
    }

    data class GoalProgressData(
        val activeGoalsCount: Int,
        val averageProgressPercent: Int
    ) : DashboardWidgetPayload {
        override val widgetType = DashboardWidgetType.GOAL_PROGRESS
    }
}
