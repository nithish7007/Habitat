package com.example

import com.example.data.ChallengeDayStatus
import com.example.data.ChallengeEntity
import com.example.data.ChallengeLogEntity
import com.example.data.DashboardWidgetConverters
import com.example.data.DashboardWidgetType
import com.example.data.HabitEntity
import com.example.data.HabitLogEntity
import com.example.data.StepLogEntity
import com.example.data.TaskEntity
import com.example.data.TaskStatus
import com.example.data.UserPreferencesModel
import com.example.data.WaterLogEntity
import com.example.data.WidgetSpanSize
import com.example.data.WidgetState
import com.example.domain.MotivationQuotesProvider
import com.example.domain.ProductivityScoreEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeOsDomainAndWidgetTest {

    @Test
    fun dashboardWidgets_containsAllTenWidgetsIncludingMotivationGoal() {
        val expected = listOf(
            DashboardWidgetType.DAILY_PRODUCTIVITY_SCORE,
            DashboardWidgetType.MOTIVATION_GOAL,
            DashboardWidgetType.TASKS_SUMMARY,
            DashboardWidgetType.HABIT_PROGRESS,
            DashboardWidgetType.ACTIVE_CHALLENGES,
            DashboardWidgetType.WATER_INTAKE,
            DashboardWidgetType.AUTOMATIC_STEPS,
            DashboardWidgetType.FOCUS_TIME,
            DashboardWidgetType.UPCOMING_REMINDERS,
            DashboardWidgetType.GOAL_PROGRESS
        )
        assertEquals(10, DashboardWidgetType.entries.size)
        assertEquals(expected, DashboardWidgetType.DEFAULT_ORDER)
    }

    @Test
    fun widgetState_entityMappingAndReconciliationPreservesVisibilityPositionAndMetadata() {
        val defaults = WidgetState.defaultWidgetsForUser("uid_1")
        assertEquals(10, defaults.size)
        assertTrue(defaults.all { it.isVisible })
        assertEquals((0..9).toList(), defaults.map { it.position })

        // Customize spanSize on MOTIVATION_GOAL, then hide WATER_INTAKE and move GOAL_PROGRESS to top
        val customized = defaults.map {
            if (it.type == DashboardWidgetType.MOTIVATION_GOAL) {
                it.copy(spanSize = WidgetSpanSize.EXPANDED, customConfigJson = "{\"theme\":\"Focus\"}")
            } else {
                it
            }
        }
        val reconciled = WidgetState.reconcileWithEnabledOrder(
            userId = "uid_1",
            enabledOrder = listOf(
                DashboardWidgetType.GOAL_PROGRESS,
                DashboardWidgetType.MOTIVATION_GOAL,
                DashboardWidgetType.DAILY_PRODUCTIVITY_SCORE
            ),
            existing = customized
        )

        val visible = reconciled.filter { it.isVisible }.sortedBy { it.position }
        val hidden = reconciled.filterNot { it.isVisible }.sortedBy { it.position }
        assertEquals(3, visible.size)
        assertEquals(7, hidden.size)
        assertEquals(DashboardWidgetType.GOAL_PROGRESS, visible[0].type)
        assertEquals(0, visible[0].position)
        assertEquals(DashboardWidgetType.MOTIVATION_GOAL, visible[1].type)
        assertEquals(1, visible[1].position)
        assertEquals(WidgetSpanSize.EXPANDED, visible[1].spanSize)
        assertEquals("{\"theme\":\"Focus\"}", visible[1].customConfigJson)

        // Room entity + TypeConverter round-trip
        val entity = visible[1].toEntity()
        val roundTripped = entity.toWidgetState()
        assertEquals(visible[1], roundTripped)

        val converters = DashboardWidgetConverters()
        assertEquals(
            DashboardWidgetType.MOTIVATION_GOAL,
            converters.toWidgetType(converters.fromWidgetType(DashboardWidgetType.MOTIVATION_GOAL))
        )
        assertEquals(
            WidgetSpanSize.EXPANDED,
            converters.toWidgetSpanSize(converters.fromWidgetSpanSize(WidgetSpanSize.EXPANDED))
        )
    }

    @Test
    fun motivationQuotesProvider_rotatesDailyInEnglishWithAuthors() {
        assertTrue(MotivationQuotesProvider.quotes.size >= 30)
        MotivationQuotesProvider.quotes.forEach { q ->
            assertTrue(q.text.isNotBlank())
            assertTrue(q.author.isNotBlank())
            assertTrue(q.authorRole.isNotBlank())
            assertTrue(q.theme.isNotBlank())
        }

        val day1 = MotivationQuotesProvider.quoteForDate("2026-11-19")
        val day2 = MotivationQuotesProvider.quoteForDate("2026-11-20")
        val day3 = MotivationQuotesProvider.quoteForDate("2026-11-21")
        assertNotEquals(day1.id, day2.id)
        assertNotEquals(day2.id, day3.id)

        // Deterministic for the same day
        assertEquals(day1, MotivationQuotesProvider.quoteForDate("2026-11-19"))

        // Offset advances to a different quote immediately
        val nextOnSameDay = MotivationQuotesProvider.quoteForDate("2026-11-19", offset = 1)
        assertNotEquals(day1.id, nextOnSameDay.id)
    }

    @Test
    fun productivityScoreEngine_dynamicallyReweightsActiveModulesOnly() {
        val today = "2026-11-19"
        val prefs = UserPreferencesModel(
            weightTasks = 30,
            weightHabits = 30,
            weightChallenges = 20,
            weightWater = 10,
            weightSteps = 10,
            waterTargetMl = 2000,
            stepsTarget = 8000
        )

        val tasks = listOf(
            TaskEntity(id = "t1", userId = "u1", title = "A", dueDateIso = today, status = TaskStatus.COMPLETED.name),
            TaskEntity(id = "t2", userId = "u1", title = "B", dueDateIso = today, status = TaskStatus.PENDING.name)
        )
        val water = listOf(
            WaterLogEntity(id = "w1", userId = "u1", dateIso = today, amountMl = 2000)
        )

        val report = ProductivityScoreEngine.calculateForDate(
            dateIso = today,
            prefs = prefs,
            tasks = tasks,
            habits = emptyList(),
            habitLogs = emptyList(),
            challenges = emptyList(),
            challengeLogs = emptyList(),
            waterLogs = water,
            stepLogs = emptyList()
        )

        assertTrue(report.hasAnyTrackedData)
        assertEquals(63, report.scorePercent)
        assertTrue(report.categories.first { it.name == "Tasks" }.applicable)
        assertTrue(report.categories.first { it.name == "Water" }.applicable)
        assertFalse(report.categories.first { it.name == "Habits" }.applicable)
    }

    @Test
    fun productivityScoreEngine_countsActiveChallengeFailureStrictly() {
        val today = "2026-11-19"
        val prefs = UserPreferencesModel(
            weightTasks = 0,
            weightHabits = 0,
            weightChallenges = 100,
            weightWater = 0,
            weightSteps = 0
        )
        val challenge = ChallengeEntity(
            id = "c1",
            userId = "u1",
            title = "No Instagram",
            startDateIso = "2026-11-19",
            endDateIso = "2027-01-01"
        )
        val failedLog = ChallengeLogEntity(
            id = "c1_20261119",
            userId = "u1",
            challengeId = "c1",
            dateIso = today,
            status = ChallengeDayStatus.FAILED.name
        )

        val report = ProductivityScoreEngine.calculateForDate(
            dateIso = today,
            prefs = prefs,
            tasks = emptyList(),
            habits = emptyList(),
            habitLogs = emptyList(),
            challenges = listOf(challenge),
            challengeLogs = listOf(failedLog),
            waterLogs = emptyList(),
            stepLogs = emptyList()
        )

        assertTrue(report.hasAnyTrackedData)
        assertEquals(0, report.scorePercent)
    }

    @Test
    fun productivityScoreEngine_handlesFullCompletionAcrossAllFiveModules() {
        val today = "2026-11-19"
        val prefs = UserPreferencesModel(waterTargetMl = 2000, stepsTarget = 5000)

        val report = ProductivityScoreEngine.calculateForDate(
            dateIso = today,
            prefs = prefs,
            tasks = listOf(TaskEntity(id = "t1", userId = "u1", title = "Ship", dueDateIso = today, status = TaskStatus.COMPLETED.name)),
            habits = listOf(HabitEntity(id = "h1", userId = "u1", title = "Read", startDateIso = "2026-11-01")),
            habitLogs = listOf(HabitLogEntity(id = "h1_20261119", userId = "u1", habitId = "h1", dateIso = today, completed = true)),
            challenges = listOf(ChallengeEntity(id = "c1", userId = "u1", title = "No Instagram", startDateIso = "2026-11-01", endDateIso = "2027-01-01")),
            challengeLogs = listOf(ChallengeLogEntity(id = "c1_20261119", userId = "u1", challengeId = "c1", dateIso = today, status = ChallengeDayStatus.SUCCESS.name)),
            waterLogs = listOf(WaterLogEntity(id = "w1", userId = "u1", dateIso = today, amountMl = 2500)),
            stepLogs = listOf(StepLogEntity(id = "s1", userId = "u1", dateIso = today, steps = 6500))
        )

        assertTrue(report.hasAnyTrackedData)
        assertEquals(100, report.scorePercent)
        assertTrue(report.categories.all { it.applicable && it.rawRatio >= 0.999f })
    }

    @Test
    fun habitProgress_autoFillsFromWaterStepsAndFocusAndSupportsCustomUnits() {
        val today = "2026-11-19"
        val waterHabit = HabitEntity(
            id = "h_water",
            userId = "u1",
            title = "Hydrate",
            targetValue = 8,
            unit = "Glasses",
            autoSource = com.example.data.HabitAutoSource.WATER.name,
            startDateIso = "2026-11-01"
        )
        val stepsHabit = HabitEntity(
            id = "h_steps",
            userId = "u1",
            title = "Walk",
            targetValue = 8000,
            unit = "Steps",
            autoSource = com.example.data.HabitAutoSource.STEPS.name,
            startDateIso = "2026-11-01"
        )
        val readingHabit = HabitEntity(
            id = "h_read",
            userId = "u1",
            title = "Read",
            targetValue = 20,
            unit = "Pages",
            autoSource = com.example.data.HabitAutoSource.NONE.name,
            startDateIso = "2026-11-01"
        )

        val waterLogs = listOf(
            WaterLogEntity(id = "w1", userId = "u1", dateIso = today, amountMl = 1000),
            WaterLogEntity(id = "w2", userId = "u1", dateIso = today, amountMl = 1000)
        )
        val stepLogs = listOf(StepLogEntity(id = "s1", userId = "u1", dateIso = today, steps = 4000))
        val habitLogs = listOf(
            HabitLogEntity(id = "h_read_20261119", userId = "u1", habitId = "h_read", dateIso = today, completed = false, progressValue = 10)
        )

        val waterProg = ProductivityScoreEngine.resolveHabitDayProgress(waterHabit, today, habitLogs, waterLogs, stepLogs)
        assertTrue(waterProg.isAutoFilled)
        assertEquals(8, waterProg.currentValue)
        assertTrue(waterProg.completed)

        val stepsProg = ProductivityScoreEngine.resolveHabitDayProgress(stepsHabit, today, habitLogs, waterLogs, stepLogs)
        assertTrue(stepsProg.isAutoFilled)
        assertEquals(4000, stepsProg.currentValue)
        assertFalse(stepsProg.completed)
        assertEquals(0.5f, stepsProg.ratio, 0.01f)

        val readProg = ProductivityScoreEngine.resolveHabitDayProgress(readingHabit, today, habitLogs, waterLogs, stepLogs)
        assertFalse(readProg.isAutoFilled)
        assertEquals(10, readProg.currentValue)
        assertEquals(0.5f, readProg.ratio, 0.01f)
        assertEquals("10 / 20 Pages", readProg.formattedProgress)
    }
}
