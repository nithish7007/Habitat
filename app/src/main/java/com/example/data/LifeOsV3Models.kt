package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class TransactionType { INCOME, EXPENSE }

enum class MealType(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACKS("Snacks");

    companion object {
        fun fromNameOrDefault(raw: String): MealType =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: BREAKFAST
    }
}

enum class LearningLevel(val label: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced");

    companion object {
        fun fromNameOrDefault(raw: String): LearningLevel =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: BEGINNER
    }
}

enum class JobApplicationStatus(val label: String) {
    APPLIED("Applied"),
    ASSESSMENT("Assessment"),
    INTERVIEW_SCHEDULED("Interview Scheduled"),
    SELECTED("Selected"),
    REJECTED("Rejected"),
    ON_HOLD("On Hold");

    companion object {
        fun fromNameOrDefault(raw: String): JobApplicationStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: APPLIED
    }
}

enum class MascotMood { HAPPY, NEUTRAL, SAD }

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val fullName: String = "Builder",
    val username: String = "",
    val bio: String = "",
    val avatarLocalPath: String = "",
    val initialBalance: Double = 10000.0,
    val monthlyBudget: Double = 15000.0,
    val dailySpendLimit: Double = 1000.0,
    val proteinTargetG: Int = 90,
    val calorieTargetKcal: Int = 2200,
    val streakFreezesAvailable: Int = 1,
    val waterReminderEnabled: Boolean = false,
    val waterReminderIntervalMin: Int = 120,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "finance_transactions",
    indices = [Index("userId"), Index("dateIso")]
)
data class FinanceTransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val type: String = TransactionType.EXPENSE.name,
    val amount: Double,
    val category: String = "Food",
    val note: String = "",
    val dateIso: String = DateUtils.todayIso(),
    val locationTag: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "savings_goals",
    indices = [Index("userId")]
)
data class SavingsGoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val targetDateIso: String = DateUtils.offsetDateIso(DateUtils.todayIso(), 90),
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "nutrition_entries",
    indices = [Index("userId"), Index("dateIso")]
)
data class NutritionEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val dateIso: String = DateUtils.todayIso(),
    val mealType: String = MealType.BREAKFAST.name,
    val foodName: String,
    val quantityLabel: String = "1 serving",
    val proteinG: Double = 0.0,
    val calories: Int = 0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val isEstimated: Boolean = true,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "learning_topics",
    indices = [Index("userId"), Index("category")]
)
data class LearningTopicEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val category: String,
    val level: String = LearningLevel.BEGINNER.name,
    val title: String,
    val completed: Boolean = false,
    val saved: Boolean = false,
    val studyMinutes: Int = 0,
    val notes: String = "",
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "class_schedules",
    indices = [Index("userId"), Index("dayOfWeek")]
)
data class ClassScheduleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val className: String,
    val subject: String = "",
    val instructor: String = "",
    val dayOfWeek: Int = 1, // 1 = Mon ... 7 = Sun
    val startTime: String = "09:00",
    val endTime: String = "10:00",
    val locationOrLink: String = "",
    val reminderEnabled: Boolean = true,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "job_applications",
    indices = [Index("userId"), Index("status")]
)
data class JobApplicationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val company: String,
    val role: String,
    val jobType: String = "Full-time",
    val appliedDateIso: String = DateUtils.todayIso(),
    val applicationLink: String = "",
    val status: String = JobApplicationStatus.APPLIED.name,
    val interviewDateIso: String = "",
    val interviewTime: String = "",
    val interviewType: String = "Technical",
    val meetingLink: String = "",
    val location: String = "",
    val contactInfo: String = "",
    val prepNotes: String = "",
    val prepChecklistCsv: String = "Resume tailored|Core DSA & CS fundamentals|Company research",
    val completedChecklistCsv: String = "",
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "saved_media",
    indices = [Index("userId"), Index("kind")]
)
data class SavedMediaEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val kind: String, // NEWS, VIDEO, QUOTE
    val title: String,
    val subtitleOrAuthor: String = "",
    val url: String = "",
    val category: String = "",
    val watchedOrRead: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
)

enum class NotificationCategory(
    val id: String,
    val label: String,
    val description: String,
    val channelId: String,
    val defaultDeepLink: String
) {
    WATER("water", "Water Reminders", "Hourly hydration prompts with quick +200/+250 ml actions", "lifeos_water", "water"),
    STEPS("steps", "Step & Walking Reminders", "Gentle nudges when daily step progress is behind goal", "lifeos_steps", "steps"),
    GOALS("goals", "Daily Goal Reminders", "Check-in reminders for active personal goals", "lifeos_goals", "goals"),
    TASKS("tasks", "Task Reminders", "Alerts for pending and due-today tasks", "lifeos_tasks", "plan"),
    HABITS("habits", "Habit Reminders", "Streak protection and unfinished habit nudges", "lifeos_habits", "track"),
    TECH_NEWS("tech_news", "IT & Technology News", "Hourly check for fresh software & AI headlines", "lifeos_news", "learn"),
    YOUTUBE_VIDEOS("youtube_videos", "Educational YouTube Videos", "Fresh learning videos opening in-app details", "lifeos_videos", "learn"),
    LEARNING("learning", "Learning Reminders", "Daily skill building & roadmap study check-in", "lifeos_learning", "learn"),
    CLASSES("classes", "Class Reminders", "Upcoming class timetable alerts", "lifeos_classes", "timetable"),
    INTERVIEWS("interviews", "Interview Reminders", "Upcoming interview & prep checklist alerts", "lifeos_interviews", "jobs"),
    FINANCE("finance", "Savings & Budget Reminders", "Daily budget awareness and savings goal nudges", "lifeos_finance", "money"),
    MOTIVATION("motivation", "Daily Motivation", "Fresh daily inspirational quote notification", "lifeos_motivation", "motivation_goal"),
    DAILY_SUMMARY("daily_summary", "Daily Summary", "End-of-day digest of tasks, water, steps, and spend", "lifeos_summary", "home")
}

@Entity(
    tableName = "memories",
    indices = [Index("userId"), Index("memoryDateIso")]
)
data class MemoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val description: String = "",
    val memoryDateIso: String = DateUtils.todayIso(),
    val memoryTimeText: String = "",
    val category: String = "Personal",
    val tagsCsv: String = "",
    val photoLocalPath: String = "",
    val favorite: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "notification_preferences")
data class NotificationPreferencesEntity(
    @PrimaryKey val userId: String,
    val pauseAll: Boolean = false,
    val waterEnabled: Boolean = true,
    val stepsEnabled: Boolean = true,
    val goalsEnabled: Boolean = true,
    val tasksEnabled: Boolean = true,
    val habitsEnabled: Boolean = true,
    val techNewsEnabled: Boolean = false,
    val youtubeVideosEnabled: Boolean = false,
    val learningEnabled: Boolean = true,
    val classesEnabled: Boolean = true,
    val interviewsEnabled: Boolean = true,
    val financeEnabled: Boolean = true,
    val motivationEnabled: Boolean = true,
    val dailySummaryEnabled: Boolean = true,
    val waterIntervalMinutes: Int = 60,
    val newsIntervalMinutes: Int = 60,
    val videoIntervalMinutes: Int = 60,
    val dailySummaryHour: Int = 20,
    val dailySummaryMinute: Int = 30,
    val quietHoursEnabled: Boolean = true,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val lastSeenNewsUrl: String = "",
    val lastSeenVideoUrl: String = "",
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    fun isCategoryEnabled(category: NotificationCategory): Boolean {
        if (pauseAll) return false
        return when (category) {
            NotificationCategory.WATER -> waterEnabled
            NotificationCategory.STEPS -> stepsEnabled
            NotificationCategory.GOALS -> goalsEnabled
            NotificationCategory.TASKS -> tasksEnabled
            NotificationCategory.HABITS -> habitsEnabled
            NotificationCategory.TECH_NEWS -> techNewsEnabled
            NotificationCategory.YOUTUBE_VIDEOS -> youtubeVideosEnabled
            NotificationCategory.LEARNING -> learningEnabled
            NotificationCategory.CLASSES -> classesEnabled
            NotificationCategory.INTERVIEWS -> interviewsEnabled
            NotificationCategory.FINANCE -> financeEnabled
            NotificationCategory.MOTIVATION -> motivationEnabled
            NotificationCategory.DAILY_SUMMARY -> dailySummaryEnabled
        }
    }
}

@Entity(
    tableName = "notification_logs",
    indices = [Index("userId"), Index("createdAtMs")]
)
data class NotificationLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val categoryId: String,
    val title: String,
    val message: String,
    val deepLinkRoute: String = "home",
    val deepLinkArg: String = "",
    val dedupeKey: String = "",
    val createdAtMs: Long = System.currentTimeMillis()
)


