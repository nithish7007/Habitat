package com.example.domain

import com.example.data.DateUtils
import com.example.data.TaskStatus
import com.example.ui.LifeOsUiState
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isStreaming: Boolean = false,
    val suggestedTaskTitle: String? = null
)

/**
 * Powered by Firebase AI Logic (Gemini API) with App Check attestation.
 * Uses real user records from Room/Firestore as structured prompt context — never fabricates stats.
 */
object LifeOsAiAssistant {

    // Lazily initialized so FirebaseApp and App Check are ready before first AI request.
    private val assistantModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are the LIFE OS Personal Assistant inside an Android personal life management app.
                    Be concise, warm, accurate, and actionable.
                    ALWAYS base numeric answers strictly on the LIVE USER DATA SNAPSHOT provided in the prompt.
                    NEVER invent tasks, expenses, balances, streaks, classes, or interviews that are not in the snapshot.
                    If a record is empty or zero, clearly say so and suggest a helpful next step.
                    If the user explicitly asks to create or add a new task (for example: "Add a task to study Spring Boot"),
                    include a single machine-readable tag at the very end of your reply on its own line:
                    [[PROPOSE_TASK:<short task title>]]
                    Never execute financial or destructive changes automatically — remind the user to confirm in the app.
                    """.trimIndent()
                )
            }
        )
    }

    private val quoteModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    Write ONE short, original, inspiring motivational quote in plain English (1 to 2 sentences max, under 28 words).
                    Do NOT attribute it to any historical or living person — this is an original AI-generated quote.
                    Output ONLY the quote text without quotation marks or commentary.
                    """.trimIndent()
                )
            }
        )
    }

    fun buildLiveContextSnapshot(state: LifeOsUiState): String {
        val today = state.selectedDateIso
        val tomorrow = DateUtils.offsetDateIso(today, 1)
        val todayTasks = state.tasks.filter { it.dueDateIso == today }
        val pendingToday = todayTasks.filter { it.status != TaskStatus.COMPLETED.name }
        val completedToday = todayTasks.filter { it.status == TaskStatus.COMPLETED.name }
        val overdueTasks = state.tasks.filter { it.dueDateIso < today && it.status != TaskStatus.COMPLETED.name }
        val todayWaterMl = state.waterLogs.filter { it.dateIso == today }.sumOf { it.amountMl }
        val focusMinToday = state.focusSessions.filter { it.dateIso == today }.sumOf { it.actualSeconds } / 60
        val finance = state.financeSummary
        val nutrition = state.todayNutrition
        val streak = state.streakReport

        val todayDayOfWeek = isoDayOfWeekMon1Sun7(today)
        val tomorrowDayOfWeek = isoDayOfWeekMon1Sun7(tomorrow)
        val todayClasses = state.classSchedules.filter { it.dayOfWeek == todayDayOfWeek }
        val tomorrowClasses = state.classSchedules.filter { it.dayOfWeek == tomorrowDayOfWeek }
        val upcomingInterviews = state.jobApplications.filter {
            it.interviewDateIso.isNotBlank() && it.interviewDateIso >= today
        }.sortedBy { it.interviewDateIso }

        val completedTopics = state.learningTopics.count { it.completed }
        val totalTopics = state.learningTopics.size

        return buildString {
            appendLine("=== LIVE USER DATA SNAPSHOT ($today) ===")
            appendLine("User: ${state.profile.fullName.ifBlank { state.displayName }}")
            appendLine("Productivity Score Today: ${state.todayScore.scorePercent?.let { "$it%" } ?: "No data logged yet"}")
            appendLine("Weekly Scores (7 days): ${state.weeklyScores.joinToString { "${it.first}:${it.second}%" }}")
            appendLine("Streak: current=${streak.currentStreak} days, longest=${streak.longestStreak} days, XP=${streak.totalXp}, Level=${streak.level}")
            appendLine("Tasks Today (${todayTasks.size} total, ${completedToday.size} done, ${pendingToday.size} pending):")
            pendingToday.take(8).forEach { appendLine("  - [PENDING][${it.priority}] ${it.title}") }
            completedToday.take(5).forEach { appendLine("  - [DONE] ${it.title}") }
            if (overdueTasks.isNotEmpty()) {
                appendLine("Overdue Tasks (${overdueTasks.size}): ${overdueTasks.take(5).joinToString { it.title }}")
            }
            appendLine("Water Today: $todayWaterMl ml / ${state.preferences.waterTargetMl} ml target")
            appendLine("Nutrition Today: Protein ${"%.1f".format(nutrition.totalProteinG)}g / ${nutrition.proteinTargetG}g, Calories ${nutrition.totalCalories} / ${nutrition.calorieTargetKcal} kcal (${nutrition.entriesCount} meals logged)")
            appendLine("Focus Time Today: $focusMinToday min / ${state.preferences.focusTargetMinutes} min target")
            appendLine("Money: Available Balance=${FinanceEngine.formatInr(finance.availableBalance)} (Initial=${FinanceEngine.formatInr(finance.initialBalance)}, Income=${FinanceEngine.formatInr(finance.totalIncome)}, Expenses=${FinanceEngine.formatInr(finance.totalExpenses)})")
            appendLine("Spending: Today=${FinanceEngine.formatInr(finance.todayExpense)}, This Month=${FinanceEngine.formatInr(finance.monthlyExpense)}, Budget Remaining=${FinanceEngine.formatInr(finance.budgetRemaining)}")
            appendLine("Savings Goals (${state.savingsGoals.size}): Total Saved=${FinanceEngine.formatInr(finance.totalSavedInGoals)} / Target=${FinanceEngine.formatInr(finance.totalSavingsTarget)}")
            state.savingsGoals.take(5).forEach {
                appendLine("  - ${it.title}: ${FinanceEngine.formatInr(it.savedAmount)} / ${FinanceEngine.formatInr(it.targetAmount)}")
            }
            appendLine("Tech Learning: $completedTopics / $totalTopics topics completed")
            appendLine("Today's Classes (${todayClasses.size}): ${todayClasses.joinToString { "${it.className} (${it.startTime}-${it.endTime})" }.ifBlank { "None scheduled" }}")
            appendLine("Tomorrow's Classes (${tomorrowClasses.size}): ${tomorrowClasses.joinToString { "${it.className} (${it.startTime}-${it.endTime})" }.ifBlank { "None scheduled" }}")
            appendLine("Upcoming Interviews (${upcomingInterviews.size}): ${upcomingInterviews.take(5).joinToString { "${it.company} - ${it.role} on ${it.interviewDateIso} ${it.interviewTime}" }.ifBlank { "None scheduled" }}")
        }
    }

    fun streamAssistantReply(
        userPrompt: String,
        history: List<AiChatMessage>,
        state: LifeOsUiState
    ): Flow<String> = flow {
        val snapshot = buildLiveContextSnapshot(state)
        val recentTurns = history.takeLast(6).joinToString("\n") { msg ->
            if (msg.isUser) "User: ${msg.text}" else "Assistant: ${msg.text}"
        }
        val combinedPrompt = buildString {
            appendLine(snapshot)
            if (recentTurns.isNotBlank()) {
                appendLine("=== RECENT CONVERSATION ===")
                appendLine(recentTurns)
            }
            appendLine("=== USER MESSAGE ===")
            appendLine(userPrompt.trim())
        }

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            assistantModel.generateContentStream(combinedPrompt).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    accumulated.append(part)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isEmpty()) {
            throw IllegalStateException("Empty response received from Gemini.")
        }
    }

    suspend fun generateOriginalMotivationalQuote(theme: String = "Discipline"): Result<String> {
        return try {
            val response = withTimeout(45_000L) {
                quoteModel.generateContent("Write one original motivational quote focused on: $theme.")
            }
            val clean = response.text?.trim()?.trim('"', '\'', '“', '”').orEmpty()
            if (clean.isBlank()) {
                Result.failure(IllegalStateException("Empty AI quote response"))
            } else {
                Result.success(clean)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun extractProposedTask(replyText: String): Pair<String, String?> {
        val regex = Regex("\\[\\[PROPOSE_TASK:(.+?)]]")
        val match = regex.find(replyText)
        val taskTitle = match?.groupValues?.getOrNull(1)?.trim()?.take(180)
        val cleanText = replyText.replace(regex, "").trim()
        return cleanText to taskTitle?.ifBlank { null }
    }

    fun isoDayOfWeekMon1Sun7(dateIso: String): Int {
        val parts = dateIso.split("-")
        if (parts.size != 3) return 1
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, parts[0].toIntOrNull() ?: 2026)
            set(java.util.Calendar.MONTH, ((parts[1].toIntOrNull() ?: 1) - 1).coerceIn(0, 11))
            set(java.util.Calendar.DAY_OF_MONTH, (parts[2].toIntOrNull() ?: 1).coerceIn(1, 31))
        }
        return when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> 1
            java.util.Calendar.TUESDAY -> 2
            java.util.Calendar.WEDNESDAY -> 3
            java.util.Calendar.THURSDAY -> 4
            java.util.Calendar.FRIDAY -> 5
            java.util.Calendar.SATURDAY -> 6
            else -> 7
        }
    }
}
