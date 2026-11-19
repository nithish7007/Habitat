package com.example.domain

import com.example.data.ChallengeDayStatus
import com.example.data.ChallengeEntity
import com.example.data.ChallengeLogEntity
import com.example.data.DateUtils
import com.example.data.FocusSessionEntity
import com.example.data.GoalEntity
import com.example.data.HabitAutoSource
import com.example.data.HabitEntity
import com.example.data.HabitLogEntity
import com.example.data.StepLogEntity
import com.example.data.TaskEntity
import com.example.data.TaskStatus
import com.example.data.UserPreferencesModel
import com.example.data.WaterLogEntity
import kotlin.math.roundToInt

data class HabitDayProgress(
    val currentValue: Int,
    val targetValue: Int,
    val unit: String,
    val ratio: Float,
    val completed: Boolean,
    val autoSource: HabitAutoSource
) {
    val isAutoFilled: Boolean get() = autoSource != HabitAutoSource.NONE
    val formattedProgress: String
        get() = if (targetValue <= 1 && unit.equals("Check-in", ignoreCase = true) && !isAutoFilled) {
            if (completed) "Done" else "Pending"
        } else {
            "$currentValue / $targetValue $unit"
        }
}

data class CategoryScoreDetail(
    val name: String,
    val applicable: Boolean,
    val completedCount: Int,
    val totalCount: Int,
    val rawRatio: Float,
    val configuredWeight: Int,
    val effectiveWeightPercent: Float,
    val contributedPoints: Float,
    val summaryText: String
)

data class ProductivityScoreReport(
    val dateIso: String,
    val scorePercent: Int?,
    val categories: List<CategoryScoreDetail>,
    val hasAnyTrackedData: Boolean
)

data class AchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val xp: Int,
    val unlocked: Boolean,
    val progressLabel: String
)

data class InsightSuggestion(
    val title: String,
    val detail: String,
    val category: String
)

object ProductivityScoreEngine {

    fun resolveHabitDayProgress(
        habit: HabitEntity,
        dateIso: String,
        habitLogs: List<HabitLogEntity>,
        waterLogs: List<WaterLogEntity> = emptyList(),
        stepLogs: List<StepLogEntity> = emptyList(),
        focusSessions: List<FocusSessionEntity> = emptyList()
    ): HabitDayProgress {
        val target = habit.targetValue.coerceAtLeast(1)
        val unit = habit.unit.ifBlank { "Check-in" }
        val source = HabitAutoSource.fromNameOrDefault(habit.autoSource)

        val current = when (source) {
            HabitAutoSource.STEPS -> {
                stepLogs.filter { it.dateIso == dateIso }.maxOfOrNull { it.steps } ?: 0
            }
            HabitAutoSource.WATER -> {
                val totalMl = waterLogs.filter { it.dateIso == dateIso }.sumOf { it.amountMl }
                if (unit.equals("Glasses", ignoreCase = true)) totalMl / 250 else totalMl
            }
            HabitAutoSource.FOCUS_MINUTES -> {
                focusSessions.filter { it.dateIso == dateIso }.sumOf { it.actualSeconds } / 60
            }
            HabitAutoSource.NONE -> {
                val log = habitLogs.firstOrNull { it.habitId == habit.id && it.dateIso == dateIso }
                when {
                    log == null -> 0
                    log.progressValue > 0 -> log.progressValue
                    log.completed -> target
                    else -> 0
                }
            }
        }

        val ratio = (current.toFloat() / target).coerceIn(0f, 1f)
        val completed = when (source) {
            HabitAutoSource.NONE -> {
                val log = habitLogs.firstOrNull { it.habitId == habit.id && it.dateIso == dateIso }
                (log?.completed == true) || current >= target
            }
            else -> current >= target
        }

        return HabitDayProgress(
            currentValue = current,
            targetValue = target,
            unit = unit,
            ratio = ratio,
            completed = completed,
            autoSource = source
        )
    }

    fun calculateForDate(
        dateIso: String,
        prefs: UserPreferencesModel,
        tasks: List<TaskEntity>,
        habits: List<HabitEntity>,
        habitLogs: List<HabitLogEntity>,
        challenges: List<ChallengeEntity>,
        challengeLogs: List<ChallengeLogEntity>,
        waterLogs: List<WaterLogEntity>,
        stepLogs: List<StepLogEntity>,
        focusSessions: List<FocusSessionEntity> = emptyList()
    ): ProductivityScoreReport {
        val dayBit = DateUtils.dayOfWeekBit(dateIso)

        // 1. Tasks due on dateIso OR completed on dateIso
        val dayTasks = tasks.filter { it.dueDateIso == dateIso || it.completedDateIso == dateIso }
        val completedTasks = dayTasks.count { it.status == TaskStatus.COMPLETED.name }
        val tasksApplicable = dayTasks.isNotEmpty() && prefs.weightTasks > 0
        val tasksRatio = if (dayTasks.isNotEmpty()) completedTasks.toFloat() / dayTasks.size else 0f

        // 2. Habits scheduled on dateIso (includes custom units + Health/Metric auto-fill)
        val activeHabits = habits.filter { h ->
            h.startDateIso <= dateIso &&
                (h.endDateIso.isBlank() || dateIso <= h.endDateIso) &&
                ((h.weekdaysMask and dayBit) != 0)
        }
        val habitProgressList = activeHabits.map { h ->
            resolveHabitDayProgress(h, dateIso, habitLogs, waterLogs, stepLogs, focusSessions)
        }
        val completedHabits = habitProgressList.count { it.completed }
        val habitsApplicable = activeHabits.isNotEmpty() && prefs.weightHabits > 0
        val habitsRatio = if (activeHabits.isNotEmpty()) {
            habitProgressList.sumOf { it.ratio.toDouble() }.toFloat() / activeHabits.size
        } else 0f

        // 3. Challenges active on dateIso (excluding future dates)
        val activeChallenges = if (DateUtils.isFutureIso(dateIso)) {
            emptyList()
        } else {
            challenges.filter { c -> dateIso >= c.startDateIso && dateIso <= c.endDateIso }
        }
        val challengeSuccess = activeChallenges.count { c ->
            challengeLogs.any { it.challengeId == c.id && it.dateIso == dateIso && it.status == ChallengeDayStatus.SUCCESS.name }
        }
        val challengeFailed = activeChallenges.count { c ->
            challengeLogs.any { it.challengeId == c.id && it.dateIso == dateIso && it.status == ChallengeDayStatus.FAILED.name }
        }
        val challengesApplicable = activeChallenges.isNotEmpty() && prefs.weightChallenges > 0
        val challengesRatio = if (activeChallenges.isNotEmpty()) challengeSuccess.toFloat() / activeChallenges.size else 0f

        // 4. Water intake logged on dateIso
        val dayWaterMl = waterLogs.filter { it.dateIso == dateIso }.sumOf { it.amountMl }
        val waterApplicable = dayWaterMl > 0 && prefs.weightWater > 0 && prefs.waterTargetMl > 0
        val waterRatio = if (prefs.waterTargetMl > 0) (dayWaterMl.toFloat() / prefs.waterTargetMl).coerceIn(0f, 1f) else 0f

        // 5. Steps recorded on dateIso
        val daySteps = stepLogs.filter { it.dateIso == dateIso }.maxOfOrNull { it.steps } ?: 0
        val stepsApplicable = daySteps > 0 && prefs.weightSteps > 0 && prefs.stepsTarget > 0
        val stepsRatio = if (prefs.stepsTarget > 0) (daySteps.toFloat() / prefs.stepsTarget).coerceIn(0f, 1f) else 0f

        val rawList = listOf(
            RawCategory("Tasks", tasksApplicable, completedTasks, dayTasks.size, tasksRatio, prefs.weightTasks,
                if (tasksApplicable) "$completedTasks / ${dayTasks.size} completed" else "No tasks due"),
            RawCategory("Habits", habitsApplicable, completedHabits, activeHabits.size, habitsRatio, prefs.weightHabits,
                if (habitsApplicable) "$completedHabits / ${activeHabits.size} checked" else "No scheduled habits"),
            RawCategory("Challenges", challengesApplicable, challengeSuccess, activeChallenges.size, challengesRatio, prefs.weightChallenges,
                if (challengesApplicable) "$challengeSuccess passed, $challengeFailed failed" else "No active challenges"),
            RawCategory("Water", waterApplicable, dayWaterMl, prefs.waterTargetMl, waterRatio, prefs.weightWater,
                if (waterApplicable) "$dayWaterMl / ${prefs.waterTargetMl} ml" else "Not tracked today"),
            RawCategory("Steps", stepsApplicable, daySteps, prefs.stepsTarget, stepsRatio, prefs.weightSteps,
                if (stepsApplicable) "$daySteps / ${prefs.stepsTarget} steps" else "Not tracked today")
        )

        val activeWeightSum = rawList.filter { it.applicable }.sumOf { it.configuredWeight }
        if (activeWeightSum <= 0) {
            return ProductivityScoreReport(
                dateIso = dateIso,
                scorePercent = null,
                categories = rawList.map {
                    CategoryScoreDetail(it.name, false, it.done, it.total, it.ratio, it.configuredWeight, 0f, 0f, it.summary)
                },
                hasAnyTrackedData = false
            )
        }

        val details = rawList.map { c ->
            if (!c.applicable) {
                CategoryScoreDetail(c.name, false, c.done, c.total, c.ratio, c.configuredWeight, 0f, 0f, c.summary)
            } else {
                val effWeight = (c.configuredWeight.toFloat() / activeWeightSum) * 100f
                val points = c.ratio * effWeight
                CategoryScoreDetail(c.name, true, c.done, c.total, c.ratio, c.configuredWeight, effWeight, points, c.summary)
            }
        }

        val totalScore = details.sumOf { it.contributedPoints.toDouble() }.roundToInt().coerceIn(0, 100)
        return ProductivityScoreReport(
            dateIso = dateIso,
            scorePercent = totalScore,
            categories = details,
            hasAnyTrackedData = true
        )
    }

    fun calculateHabitCurrentStreak(
        habit: HabitEntity,
        logs: List<HabitLogEntity>,
        todayIso: String = DateUtils.todayIso(),
        waterLogs: List<WaterLogEntity> = emptyList(),
        stepLogs: List<StepLogEntity> = emptyList(),
        focusSessions: List<FocusSessionEntity> = emptyList()
    ): Int {
        var streak = 0
        var cursor = todayIso
        for (i in 0 until 365) {
            if (cursor < habit.startDateIso) break
            val scheduled = (habit.weekdaysMask and DateUtils.dayOfWeekBit(cursor)) != 0
            if (scheduled) {
                val done = resolveHabitDayProgress(habit, cursor, logs, waterLogs, stepLogs, focusSessions).completed
                if (done) {
                    streak++
                } else if (i > 0) {
                    break
                }
            }
            cursor = DateUtils.offsetDateIso(cursor, -1)
        }
        return streak
    }

    fun calculateHabitLongestStreak(
        habit: HabitEntity,
        logs: List<HabitLogEntity>,
        todayIso: String = DateUtils.todayIso(),
        waterLogs: List<WaterLogEntity> = emptyList(),
        stepLogs: List<StepLogEntity> = emptyList(),
        focusSessions: List<FocusSessionEntity> = emptyList()
    ): Int {
        var best = 0
        var current = 0
        val start = habit.startDateIso
        val end = if (habit.endDateIso.isNotBlank() && habit.endDateIso < todayIso) habit.endDateIso else todayIso
        val days = DateUtils.dateRange(start, end, maxDays = 365)
        for (d in days) {
            val scheduled = (habit.weekdaysMask and DateUtils.dayOfWeekBit(d)) != 0
            if (!scheduled) continue
            val done = resolveHabitDayProgress(habit, d, logs, waterLogs, stepLogs, focusSessions).completed
            if (done) {
                current++
                if (current > best) best = current
            } else {
                current = 0
            }
        }
        return best
    }

    fun calculateHabitCompletionRate(
        habit: HabitEntity,
        logs: List<HabitLogEntity>,
        endDateIso: String = DateUtils.todayIso(),
        lookbackDays: Int = 30,
        waterLogs: List<WaterLogEntity> = emptyList(),
        stepLogs: List<StepLogEntity> = emptyList(),
        focusSessions: List<FocusSessionEntity> = emptyList()
    ): Pair<Int, Int> { // returns (completionPercent, missedScheduledDays)
        val days = DateUtils.lastNDaysIso(lookbackDays, endDateIso).filter { d ->
            d >= habit.startDateIso &&
                (habit.endDateIso.isBlank() || d <= habit.endDateIso) &&
                ((habit.weekdaysMask and DateUtils.dayOfWeekBit(d)) != 0)
        }
        if (days.isEmpty()) return 0 to 0
        val doneCount = days.count { d ->
            resolveHabitDayProgress(habit, d, logs, waterLogs, stepLogs, focusSessions).completed
        }
        val missed = (days.size - doneCount).coerceAtLeast(0)
        val pct = ((doneCount.toFloat() / days.size) * 100f).roundToInt().coerceIn(0, 100)
        return pct to missed
    }

    fun evaluateAchievements(
        tasks: List<TaskEntity>,
        habits: List<HabitEntity>,
        habitLogs: List<HabitLogEntity>,
        challenges: List<ChallengeEntity>,
        challengeLogs: List<ChallengeLogEntity>,
        focusSessions: List<FocusSessionEntity>,
        waterLogs: List<WaterLogEntity>,
        prefs: UserPreferencesModel
    ): List<AchievementItem> {
        val completedTasks = tasks.count { it.status == TaskStatus.COMPLETED.name }
        val maxHabitStreak = habits.maxOfOrNull { calculateHabitLongestStreak(it, habitLogs) } ?: 0
        val totalFocusMinutes = focusSessions.sumOf { it.actualSeconds } / 60
        val challengeSuccessDays = challengeLogs.count { it.status == ChallengeDayStatus.SUCCESS.name }
        val waterGoalDays = waterLogs.groupBy { it.dateIso }.count { (_, logs) -> logs.sumOf { it.amountMl } >= prefs.waterTargetMl }

        return listOf(
            AchievementItem(
                id = "first_task",
                title = "First Ignition",
                description = "Complete your first task in LIFE OS",
                xp = 50,
                unlocked = completedTasks >= 1,
                progressLabel = "${completedTasks.coerceAtMost(1)}/1 task"
            ),
            AchievementItem(
                id = "streak_7",
                title = "7-Day Consistency",
                description = "Maintain a 7-day habit streak",
                xp = 150,
                unlocked = maxHabitStreak >= 7,
                progressLabel = "${maxHabitStreak.coerceAtMost(7)}/7 days"
            ),
            AchievementItem(
                id = "streak_30",
                title = "Iron Discipline",
                description = "Maintain a 30-day habit streak",
                xp = 500,
                unlocked = maxHabitStreak >= 30,
                progressLabel = "${maxHabitStreak.coerceAtMost(30)}/30 days"
            ),
            AchievementItem(
                id = "tasks_100",
                title = "Centurion Executor",
                description = "Complete 100 tasks across your life workspace",
                xp = 600,
                unlocked = completedTasks >= 100,
                progressLabel = "${completedTasks.coerceAtMost(100)}/100 tasks"
            ),
            AchievementItem(
                id = "focus_10h",
                title = "Deep Work Master",
                description = "Accumulate 10 hours (600 min) of tracked Focus Studio time",
                xp = 400,
                unlocked = totalFocusMinutes >= 600,
                progressLabel = "${totalFocusMinutes.coerceAtMost(600)}/600 min"
            ),
            AchievementItem(
                id = "challenge_warrior",
                title = "Challenge Finisher",
                description = "Log 14 verified success days on active challenges",
                xp = 300,
                unlocked = challengeSuccessDays >= 14,
                progressLabel = "${challengeSuccessDays.coerceAtMost(14)}/14 days"
            ),
            AchievementItem(
                id = "hydration_pro",
                title = "Hydration Pro",
                description = "Hit your daily water target on 7 separate days",
                xp = 200,
                unlocked = waterGoalDays >= 7,
                progressLabel = "${waterGoalDays.coerceAtMost(7)}/7 days"
            )
        )
    }

    fun generateInsights(
        todayReport: ProductivityScoreReport,
        yesterdayReport: ProductivityScoreReport,
        tasks: List<TaskEntity>,
        focusSessions: List<FocusSessionEntity>,
        goals: List<GoalEntity>,
        todayIso: String = DateUtils.todayIso()
    ): List<InsightSuggestion> {
        val list = mutableListOf<InsightSuggestion>()
        val overdue = tasks.count { it.status != TaskStatus.COMPLETED.name && it.dueDateIso < todayIso }
        if (overdue > 0) {
            list += InsightSuggestion(
                title = "Clear $overdue Overdue Task${if (overdue > 1) "s" else ""}",
                detail = "Reschedule or complete overdue items in Plan so your daily score stays accurate.",
                category = "Tasks"
            )
        }
        val todayScore = todayReport.scorePercent
        val yesterdayScore = yesterdayReport.scorePercent
        if (todayScore != null && yesterdayScore != null) {
            val delta = todayScore - yesterdayScore
            if (delta >= 5) {
                list += InsightSuggestion(
                    title = "Momentum Up +$delta% vs Yesterday",
                    detail = "Today's weighted score ($todayScore%) is outperforming yesterday ($yesterdayScore%).",
                    category = "Trend"
                )
            } else if (delta <= -5) {
                list += InsightSuggestion(
                    title = "Score Dip ($delta% vs Yesterday)",
                    detail = "Inspect your score breakdown to reclaim high-weight Tasks or Habits before midnight.",
                    category = "Trend"
                )
            }
        }
        val recentDistractions = focusSessions.filter { it.dateIso == todayIso }.sumOf { it.distractionsCount }
        if (recentDistractions >= 3) {
            list += InsightSuggestion(
                title = "Reduce Mid-Session Distractions",
                detail = "You logged $recentDistractions distractions today. Try shorter 20-minute focus sprints.",
                category = "Focus"
            )
        }
        val activeGoals = goals.filter { it.status == "ACTIVE" && it.progressPercent < 30 }
        if (activeGoals.isNotEmpty()) {
            list += InsightSuggestion(
                title = "Advance '${activeGoals.first().title}'",
                detail = "Break this goal into smaller milestones in Goal Center to build steady progress.",
                category = "Goals"
            )
        }
        if (list.isEmpty()) {
            list += InsightSuggestion(
                title = "Build Your Baseline",
                detail = "Log tasks, habits, focus sessions, water, or steps today to unlock data-driven recommendations.",
                category = "Overview"
            )
        }
        return list
    }

    private data class RawCategory(
        val name: String,
        val applicable: Boolean,
        val done: Int,
        val total: Int,
        val ratio: Float,
        val configuredWeight: Int,
        val summary: String
    )
}
