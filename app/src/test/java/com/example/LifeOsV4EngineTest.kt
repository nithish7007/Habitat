package com.example

import com.example.data.FinanceTransactionEntity
import com.example.data.MealType
import com.example.data.NutritionEntryEntity
import com.example.data.SavingsGoalEntity
import com.example.data.StepCounterManager
import com.example.data.TaskEntity
import com.example.data.TaskStatus
import com.example.data.TransactionType
import com.example.data.UserProfileEntity
import com.example.data.WaterLogEntity
import com.example.domain.FinanceEngine
import com.example.domain.NutritionEngine
import com.example.domain.StreakAndXpEngine
import com.example.worker.NotificationPolicyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeOsV4EngineTest {

    @Test
    fun quietHoursWrapAroundMidnightWorksCorrectly() {
        // 22:00 to 07:00
        assertTrue(NotificationPolicyEngine.isWithinQuietHours(22, 22, 7, true))
        assertTrue(NotificationPolicyEngine.isWithinQuietHours(23, 22, 7, true))
        assertTrue(NotificationPolicyEngine.isWithinQuietHours(0, 22, 7, true))
        assertTrue(NotificationPolicyEngine.isWithinQuietHours(6, 22, 7, true))
        assertFalse(NotificationPolicyEngine.isWithinQuietHours(7, 22, 7, true))
        assertFalse(NotificationPolicyEngine.isWithinQuietHours(14, 22, 7, true))
        // Disabled quiet hours always false
        assertFalse(NotificationPolicyEngine.isWithinQuietHours(23, 22, 7, false))
    }

    @Test
    fun waterReminderStopsOnceDailyTargetIsReached() {
        assertTrue(NotificationPolicyEngine.shouldSendWaterReminder(todayWaterMl = 1200, dailyTargetMl = 2500))
        assertFalse(NotificationPolicyEngine.shouldSendWaterReminder(todayWaterMl = 2500, dailyTargetMl = 2500))
        assertFalse(NotificationPolicyEngine.shouldSendWaterReminder(todayWaterMl = 3000, dailyTargetMl = 2500))
    }

    @Test
    fun dailySummaryFormatsCoreMetricsAndInterviews() {
        val text = NotificationPolicyEngine.formatDailySummary(
            tasksDone = 4,
            tasksTotal = 5,
            waterMl = 2000,
            waterTargetMl = 2500,
            steps = 6400,
            stepsTarget = 8000,
            todayExpense = 350.0,
            upcomingInterviews = 2
        )
        assertTrue(text.contains("Tasks 4/5"))
        assertTrue(text.contains("Water 2000/2500 ml"))
        assertTrue(text.contains("Steps 6400/8000"))
        assertTrue(text.contains("Spent ₹350"))
        assertTrue(text.contains("2 interview(s) ahead"))
    }

    @Test
    fun stepCounterDistanceEstimationIsAccurateAndSafe() {
        assertEquals(0.0, StepCounterManager.estimateDistanceKm(0), 0.0001)
        assertEquals(0.0, StepCounterManager.estimateDistanceKm(-50), 0.0001)
        assertEquals(6.0, StepCounterManager.estimateDistanceKm(8000), 0.001)
    }

    @Test
    fun financeEngineComputesBalanceAndAlertsAccurately() {
        val profile = UserProfileEntity(
            userId = "u1",
            fullName = "Arun",
            initialBalance = 10000.0,
            monthlyBudget = 5000.0,
            dailySpendLimit = 1000.0
        )
        val txs = listOf(
            FinanceTransactionEntity(userId = "u1", type = TransactionType.INCOME.name, amount = 4000.0, category = "Freelance", dateIso = "2026-11-19"),
            FinanceTransactionEntity(userId = "u1", type = TransactionType.EXPENSE.name, amount = 1200.0, category = "Food", dateIso = "2026-11-19"),
            FinanceTransactionEntity(userId = "u1", type = TransactionType.EXPENSE.name, amount = 800.0, category = "Travel", dateIso = "2026-11-18")
        )
        val goals = listOf(
            SavingsGoalEntity(userId = "u1", title = "Laptop", targetAmount = 60000.0, savedAmount = 15000.0, targetDateIso = "2027-03-01")
        )
        val summary = FinanceEngine.summarize(profile, txs, goals, dateIso = "2026-11-19")
        // 10000 + 4000 - 2000 = 12000
        assertEquals(12000.0, summary.availableBalance, 0.01)
        assertEquals(1200.0, summary.todayExpense, 0.01)
        assertEquals(2000.0, summary.monthlyExpense, 0.01)
        assertEquals(3000.0, summary.budgetRemaining, 0.01)
        assertTrue(summary.alerts.any { it.contains("daily limit") })
    }

    @Test
    fun nutritionEngineAggregatesDailyProteinAndCalories() {
        val entries = listOf(
            NutritionEntryEntity(userId = "u1", dateIso = "2026-11-19", mealType = MealType.BREAKFAST.name, foodName = "Eggs", quantityLabel = "2", proteinG = 12.6, calories = 155),
            NutritionEntryEntity(userId = "u1", dateIso = "2026-11-19", mealType = MealType.LUNCH.name, foodName = "Paneer", quantityLabel = "100g", proteinG = 18.0, calories = 265),
            NutritionEntryEntity(userId = "u1", dateIso = "2026-11-18", mealType = MealType.DINNER.name, foodName = "Old Meal", quantityLabel = "1", proteinG = 30.0, calories = 500)
        )
        val summary = NutritionEngine.summarizeForDate(entries, "2026-11-19", proteinTargetG = 90, calorieTargetKcal = 2200)
        assertEquals(30.6, summary.totalProteinG, 0.01)
        assertEquals(420, summary.totalCalories)
        assertEquals(2, summary.entriesCount)
    }

    @Test
    fun streakAndXpEngineCountsConsecutiveActiveDays() {
        val tasks = listOf(
            TaskEntity(userId = "u1", title = "Day 1", status = TaskStatus.COMPLETED.name, dueDateIso = "2026-11-18", completedDateIso = "2026-11-18"),
            TaskEntity(userId = "u1", title = "Day 2", status = TaskStatus.COMPLETED.name, dueDateIso = "2026-11-19", completedDateIso = "2026-11-19")
        )
        val water = listOf(
            WaterLogEntity(userId = "u1", dateIso = "2026-11-17", amountMl = 600)
        )
        val report = StreakAndXpEngine.calculate(
            todayIso = "2026-11-19",
            todayScorePercent = 85,
            tasks = tasks,
            habitLogs = emptyList(),
            challengeLogs = emptyList(),
            focusSessions = emptyList(),
            waterLogs = water,
            nutritionEntries = emptyList(),
            learningTopics = emptyList()
        )
        assertTrue(report.qualifiedToday)
        assertEquals(3, report.currentStreak)
        assertTrue(report.totalXp > 0)
    }
}
