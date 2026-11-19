package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.data.BackupPreview
import com.example.data.ChallengeDayStatus
import com.example.data.ChallengeEntity
import com.example.data.ChallengeLogEntity
import com.example.data.DashboardWidgetType
import com.example.data.DateUtils
import com.example.data.FocusMode
import com.example.data.FocusSessionEntity
import com.example.data.GoalEntity
import com.example.data.GoalStatus
import com.example.data.HabitEntity
import com.example.data.HabitLogEntity
import com.example.data.JournalEntryEntity
import com.example.data.LifeOsRepository
import com.example.data.NoteEntity
import com.example.data.PersistedTimerState
import com.example.data.ReminderEntity
import com.example.data.RoutineEntity
import com.example.data.RoutineLogEntity
import com.example.data.StepLogEntity
import com.example.data.TaskEntity
import com.example.data.TaskStatus
import com.example.data.ThemeMode
import com.example.data.UserPreferencesModel
import com.example.data.WaterLogEntity
import com.example.data.WidgetSpanSize
import com.example.data.WidgetState
import com.example.domain.AchievementItem
import com.example.domain.InsightSuggestion
import com.example.domain.ProductivityScoreEngine
import com.example.domain.ProductivityScoreReport
import com.example.worker.ReminderWorker
import android.net.Uri
import com.example.data.ClassScheduleEntity
import com.example.data.FinanceTransactionEntity
import com.example.data.JobApplicationEntity
import com.example.data.LearningTopicEntity
import com.example.data.LifeOsV3Repository
import com.example.data.LiveStepSensorStatus
import com.example.data.MemoryEntity
import com.example.data.NotificationCategory
import com.example.data.NotificationLogEntity
import com.example.data.NotificationPreferencesEntity
import com.example.data.NutritionEntryEntity
import com.example.data.SavedMediaEntity
import com.example.data.SavingsGoalEntity
import com.example.data.StepTrackingRepository
import com.example.data.UserProfileEntity
import com.example.domain.DailyNutritionSummary
import com.example.domain.FinanceEngine
import com.example.domain.FinanceSummary
import com.example.domain.NutritionEngine
import com.example.domain.StreakAndXpEngine
import com.example.domain.StreakReport
import com.example.worker.LifeOsNotificationCenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class LifeOsUiState(
    val userId: String = "",
    val displayName: String = "Builder",
    val email: String = "",
    val selectedDateIso: String = DateUtils.todayIso(),
    val preferences: UserPreferencesModel = UserPreferencesModel(),
    val widgetStates: List<WidgetState> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val habits: List<HabitEntity> = emptyList(),
    val habitLogs: List<HabitLogEntity> = emptyList(),
    val challenges: List<ChallengeEntity> = emptyList(),
    val challengeLogs: List<ChallengeLogEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val routines: List<RoutineEntity> = emptyList(),
    val routineLogs: List<RoutineLogEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val journalEntries: List<JournalEntryEntity> = emptyList(),
    val focusSessions: List<FocusSessionEntity> = emptyList(),
    val waterLogs: List<WaterLogEntity> = emptyList(),
    val stepLogs: List<StepLogEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val todayScore: ProductivityScoreReport = ProductivityScoreReport(DateUtils.todayIso(), null, emptyList(), false),
    val yesterdayScore: ProductivityScoreReport = ProductivityScoreReport(DateUtils.yesterdayIso(), null, emptyList(), false),
    val weeklyScores: List<Pair<String, Int>> = emptyList(),
    val achievements: List<AchievementItem> = emptyList(),
    val insights: List<InsightSuggestion> = emptyList(),
    val timerState: PersistedTimerState = PersistedTimerState(),
    val liveElapsedSeconds: Int = 0,
    val syncError: String? = null,
    // LIFE OS v3.0 Modules
    val profile: UserProfileEntity = UserProfileEntity(userId = ""),
    val transactions: List<FinanceTransactionEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val nutritionEntries: List<NutritionEntryEntity> = emptyList(),
    val learningTopics: List<LearningTopicEntity> = emptyList(),
    val classSchedules: List<ClassScheduleEntity> = emptyList(),
    val jobApplications: List<JobApplicationEntity> = emptyList(),
    val savedMedia: List<SavedMediaEntity> = emptyList(),
    val financeSummary: FinanceSummary = FinanceEngine.summarize(UserProfileEntity(userId = ""), emptyList(), emptyList()),
    val todayNutrition: DailyNutritionSummary = NutritionEngine.summarizeForDate(emptyList(), DateUtils.todayIso(), 90, 2200),
    val streakReport: StreakReport = StreakAndXpEngine.calculate(
        DateUtils.todayIso(), null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
    ),
    // LIFE OS v4.0 Advanced Modules: Private Memories, Smart Notification Center & Live Step Sensor
    val memories: List<MemoryEntity> = emptyList(),
    val notificationPrefs: NotificationPreferencesEntity = NotificationPreferencesEntity(userId = ""),
    val notificationLogs: List<NotificationLogEntity> = emptyList(),
    val stepSensorStatus: LiveStepSensorStatus = LiveStepSensorStatus(),
    val stepTracking: StepTrackingUiState = StepTrackingUiState()
) {
    val visibleWidgetStates: List<WidgetState>
        get() = widgetStates.filter { it.isVisible }.sortedBy { it.position }

    val hiddenWidgetStates: List<WidgetState>
        get() = widgetStates.filterNot { it.isVisible }.sortedBy { it.position }
}

class LifeOsViewModel(
    private val repository: LifeOsRepository,
    private val v3Repository: LifeOsV3Repository,
    private val appContext: Context,
    val currentUserId: String,
    initialDisplayName: String,
    initialEmail: String
) : ViewModel() {

    private val selectedDate = MutableStateFlow(DateUtils.todayIso())
    private val syncError = MutableStateFlow<String?>(null)
    private val liveTick = MutableStateFlow(0L)
    private val userDisplayName = MutableStateFlow(initialDisplayName.ifBlank { "Builder" })
    private val userEmail = MutableStateFlow(initialEmail)
    private var lastSyncedSensorSteps = -1

    private val stepTrackingRepository: StepTrackingRepository = StepTrackingRepository(
        context = appContext,
        lifeOsRepository = repository
    ) { dateIso, steps ->
        if (steps > lastSyncedSensorSteps) {
            lastSyncedSensorSteps = steps
            viewModelScope.launch {
                val goal = uiState.value.preferences.stepsTarget.coerceAtLeast(1000)
                stepTrackingRepository.persistSensorSteps(currentUserId, dateIso, steps, goal)
            }
        }
    }

    val healthConnectRequiredPermissions: Set<String>
        get() = stepTrackingRepository.healthConnectManager.requiredPermissions

    private var syncJobs: List<Job> = emptyList()

    private val coreDataFlow = combine(
        repository.observeTasks(currentUserId),
        repository.observeHabits(currentUserId),
        repository.observeHabitLogs(currentUserId),
        repository.observeChallenges(currentUserId),
        repository.observeChallengeLogs(currentUserId)
    ) { tasks, habits, habitLogs, challenges, challengeLogs ->
        CoreBundle(tasks, habits, habitLogs, challenges, challengeLogs)
    }

    private val extraDataFlow = combine(
        repository.observeNotes(currentUserId),
        repository.observeRoutines(currentUserId),
        repository.observeRoutineLogs(currentUserId),
        repository.observeGoals(currentUserId),
        repository.observeJournalEntries(currentUserId)
    ) { notes, routines, routineLogs, goals, journal ->
        ExtraBundle(notes, routines, routineLogs, goals, journal)
    }

    private val wellnessDataFlow = combine(
        repository.observeFocusSessions(currentUserId),
        repository.observeWaterLogs(currentUserId),
        repository.observeStepLogs(currentUserId),
        repository.observeReminders(currentUserId),
        repository.preferences.observePreferences(currentUserId)
    ) { focus, water, steps, reminders, prefs ->
        WellnessBundle(focus, water, steps, reminders, prefs)
    }

    private val v3FinanceAndHealthFlow = combine(
        v3Repository.observeProfile(currentUserId, initialDisplayName),
        v3Repository.observeTransactions(currentUserId),
        v3Repository.observeSavingsGoals(currentUserId),
        v3Repository.observeNutritionEntries(currentUserId)
    ) { profile, txs, savings, nutrition ->
        V3HealthFinanceBundle(profile, txs, savings, nutrition)
    }

    private val v3LearnAndCareerFlow = combine(
        v3Repository.observeLearningTopics(currentUserId),
        v3Repository.observeClassSchedules(currentUserId),
        v3Repository.observeJobApplications(currentUserId),
        v3Repository.observeSavedMedia(currentUserId)
    ) { topics, classes, jobs, media ->
        V3LearnCareerBundle(topics, classes, jobs, media)
    }

    private val v4ExtrasFlow = combine(
        v3Repository.observeMemories(currentUserId),
        v3Repository.observeNotificationPrefs(currentUserId),
        v3Repository.observeNotificationLogs(currentUserId),
        stepTrackingRepository.liveSensorStatus
    ) { memories, notifPrefs, notifLogs, sensorStatus ->
        V4ExtrasBundle(memories, notifPrefs, notifLogs, sensorStatus)
    }

    private val v3CombinedFlow = combine(v3FinanceAndHealthFlow, v3LearnAndCareerFlow, v4ExtrasFlow) { hf, lc, v4 ->
        V3Bundle(
            hf.profile, hf.transactions, hf.savingsGoals, hf.nutritionEntries,
            lc.learningTopics, lc.classSchedules, lc.jobApplications, lc.savedMedia,
            v4.memories, v4.notificationPrefs, v4.notificationLogs, v4.stepSensorStatus
        )
    }

    private val metaAndWidgetsFlow = combine(
        selectedDate,
        repository.preferences.observeTimerState(currentUserId),
        liveTick,
        syncError,
        repository.observeWidgetStates(currentUserId)
    ) { dateIso, timer, tick, err, widgets ->
        MetaBundle(dateIso, timer, tick, err, widgets)
    }

    val uiState: StateFlow<LifeOsUiState> = combine(
        coreDataFlow,
        extraDataFlow,
        wellnessDataFlow,
        metaAndWidgetsFlow,
        v3CombinedFlow
    ) { core, extra, wellness, meta, v3 ->
        val dateIso = meta.dateIso
        val widgets = meta.widgets
        val yesterdayIso = DateUtils.yesterdayIso(dateIso)
        val visibleTypes = widgets.filter { it.isVisible }.sortedBy { it.position }.map { it.type }
        val effectivePrefs = wellness.prefs.copy(enabledWidgets = visibleTypes)

        val todayScore = ProductivityScoreEngine.calculateForDate(
            dateIso = dateIso,
            prefs = wellness.prefs,
            tasks = core.tasks,
            habits = core.habits,
            habitLogs = core.habitLogs,
            challenges = core.challenges,
            challengeLogs = core.challengeLogs,
            waterLogs = wellness.waterLogs,
            stepLogs = wellness.stepLogs,
            focusSessions = wellness.focusSessions
        )
        val yesterdayScore = ProductivityScoreEngine.calculateForDate(
            dateIso = yesterdayIso,
            prefs = wellness.prefs,
            tasks = core.tasks,
            habits = core.habits,
            habitLogs = core.habitLogs,
            challenges = core.challenges,
            challengeLogs = core.challengeLogs,
            waterLogs = wellness.waterLogs,
            stepLogs = wellness.stepLogs,
            focusSessions = wellness.focusSessions
        )
        val weekly = DateUtils.lastNDaysIso(7, dateIso).map { iso ->
            val r = ProductivityScoreEngine.calculateForDate(
                dateIso = iso,
                prefs = wellness.prefs,
                tasks = core.tasks,
                habits = core.habits,
                habitLogs = core.habitLogs,
                challenges = core.challenges,
                challengeLogs = core.challengeLogs,
                waterLogs = wellness.waterLogs,
                stepLogs = wellness.stepLogs,
                focusSessions = wellness.focusSessions
            )
            DateUtils.formatShortDay(iso) to (r.scorePercent ?: 0)
        }
        val achievements = ProductivityScoreEngine.evaluateAchievements(
            tasks = core.tasks,
            habits = core.habits,
            habitLogs = core.habitLogs,
            challenges = core.challenges,
            challengeLogs = core.challengeLogs,
            focusSessions = wellness.focusSessions,
            waterLogs = wellness.waterLogs,
            prefs = wellness.prefs
        )
        val insights = ProductivityScoreEngine.generateInsights(
            todayReport = todayScore,
            yesterdayReport = yesterdayScore,
            tasks = core.tasks,
            focusSessions = wellness.focusSessions,
            goals = extra.goals,
            todayIso = dateIso
        )

        val computedElapsed = if (meta.timer.running && meta.timer.startedAtEpochMs > 0L) {
            val deltaSec = ((System.currentTimeMillis() - meta.timer.startedAtEpochMs) / 1000L).toInt().coerceAtLeast(0)
            meta.timer.accumulatedElapsedSec + deltaSec
        } else {
            meta.timer.accumulatedElapsedSec
        }

        val financeSummary = FinanceEngine.summarize(
            profile = v3.profile,
            transactions = v3.transactions,
            savingsGoals = v3.savingsGoals,
            dateIso = dateIso
        )
        val todayNutrition = NutritionEngine.summarizeForDate(
            entries = v3.nutritionEntries,
            dateIso = dateIso,
            proteinTargetG = v3.profile.proteinTargetG,
            calorieTargetKcal = v3.profile.calorieTargetKcal
        )
        val streakReport = StreakAndXpEngine.calculate(
            todayIso = dateIso,
            todayScorePercent = todayScore.scorePercent,
            tasks = core.tasks,
            habitLogs = core.habitLogs,
            challengeLogs = core.challengeLogs,
            focusSessions = wellness.focusSessions,
            waterLogs = wellness.waterLogs,
            nutritionEntries = v3.nutritionEntries,
            learningTopics = v3.learningTopics
        )

        LifeOsUiState(
            userId = currentUserId,
            displayName = v3.profile.fullName.ifBlank { userDisplayName.value },
            email = userEmail.value,
            selectedDateIso = dateIso,
            preferences = effectivePrefs,
            widgetStates = widgets,
            tasks = core.tasks,
            habits = core.habits,
            habitLogs = core.habitLogs,
            challenges = core.challenges,
            challengeLogs = core.challengeLogs,
            notes = extra.notes,
            routines = extra.routines,
            routineLogs = extra.routineLogs,
            goals = extra.goals,
            journalEntries = extra.journal,
            focusSessions = wellness.focusSessions,
            waterLogs = wellness.waterLogs,
            stepLogs = wellness.stepLogs,
            reminders = wellness.reminders,
            todayScore = todayScore,
            yesterdayScore = yesterdayScore,
            weeklyScores = weekly,
            achievements = achievements,
            insights = insights,
            timerState = meta.timer,
            liveElapsedSeconds = computedElapsed,
            syncError = meta.syncError,
            profile = v3.profile,
            transactions = v3.transactions,
            savingsGoals = v3.savingsGoals,
            nutritionEntries = v3.nutritionEntries,
            learningTopics = v3.learningTopics,
            classSchedules = v3.classSchedules,
            jobApplications = v3.jobApplications,
            savedMedia = v3.savedMedia,
            financeSummary = financeSummary,
            todayNutrition = todayNutrition,
            streakReport = streakReport,
            memories = v3.memories,
            notificationPrefs = v3.notificationPrefs,
            notificationLogs = v3.notificationLogs,
            stepSensorStatus = v3.stepSensorStatus,
            stepTracking = StepTrackingStateMapper.build(
                selectedDateIso = dateIso,
                dailyGoal = effectivePrefs.stepsTarget,
                stepLogs = wellness.stepLogs,
                sensorStatus = v3.stepSensorStatus
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = LifeOsUiState(
            userId = currentUserId,
            displayName = initialDisplayName,
            email = initialEmail,
            widgetStates = WidgetState.defaultWidgetsForUser(currentUserId),
            profile = UserProfileEntity(userId = currentUserId, fullName = initialDisplayName.ifBlank { "Builder" }),
            notificationPrefs = NotificationPreferencesEntity(userId = currentUserId)
        )
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureDefaultWidgetStates(currentUserId)
            v3Repository.ensureSeededForUser(currentUserId, userDisplayName.value)
            repository.syncUserProfile(currentUserId, userDisplayName.value, userEmail.value.ifBlank { null })
            val notifPrefs = v3Repository.getNotificationPrefsOnce(currentUserId)
            LifeOsNotificationCenter.syncAutomationSchedules(appContext, currentUserId, notifPrefs)
            stepTrackingRepository.startLiveTracking()
            stepTrackingRepository.scheduleBackgroundStepSync(currentUserId)
            syncHealthConnectSteps()
        }
        syncJobs = repository.startCloudSync(currentUserId, viewModelScope) { err ->
            syncError.value = err
        } + v3Repository.startV3CloudSync(currentUserId, viewModelScope) { err ->
            syncError.value = err
        }
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                liveTick.value = System.currentTimeMillis()
            }
        }
    }

    fun stopSyncForSignOut() {
        syncJobs.forEach { it.cancel() }
        syncJobs = emptyList()
        stepTrackingRepository.stopLiveTracking()
    }

    override fun onCleared() {
        super.onCleared()
        stopSyncForSignOut()
    }

    fun dismissSyncError() {
        syncError.value = null
    }

    fun selectDate(iso: String) {
        selectedDate.value = iso
    }

    // --- CUSTOMIZABLE DASHBOARD WIDGETS (Room WidgetStateEntity + Cloud Firestore) ---
    private fun currentWidgetList(): List<WidgetState> =
        uiState.value.widgetStates.ifEmpty { WidgetState.defaultWidgetsForUser(currentUserId) }

    fun addWidget(widget: DashboardWidgetType) {
        viewModelScope.launch {
            val all = currentWidgetList()
            val visible = all.filter { it.isVisible }.sortedBy { it.position }.map { it.type }
            if (widget !in visible) {
                val updated = WidgetState.reconcileWithEnabledOrder(
                    userId = currentUserId,
                    enabledOrder = visible + widget,
                    existing = all
                )
                repository.saveWidgetStates(currentUserId, updated)
            }
        }
    }

    fun removeWidget(widget: DashboardWidgetType) {
        viewModelScope.launch {
            val all = currentWidgetList()
            val visible = all.filter { it.isVisible }.sortedBy { it.position }.map { it.type }
            val updated = WidgetState.reconcileWithEnabledOrder(
                userId = currentUserId,
                enabledOrder = visible.filterNot { it == widget },
                existing = all
            )
            repository.saveWidgetStates(currentUserId, updated)
        }
    }

    fun setWidgetVisibility(widget: DashboardWidgetType, visible: Boolean) {
        if (visible) addWidget(widget) else removeWidget(widget)
    }

    fun updateWidgetSpanSize(widget: DashboardWidgetType, spanSize: WidgetSpanSize) {
        viewModelScope.launch {
            val all = currentWidgetList().map { ws ->
                if (ws.type == widget) ws.copy(spanSize = spanSize) else ws
            }
            repository.saveWidgetStates(currentUserId, all)
        }
    }

    fun restoreWidgetAt(widget: DashboardWidgetType, index: Int) {
        viewModelScope.launch {
            val all = currentWidgetList()
            val visible = all.filter { it.isVisible }.sortedBy { it.position }.map { it.type }.toMutableList()
            visible.remove(widget)
            val safeIndex = index.coerceIn(0, visible.size)
            visible.add(safeIndex, widget)
            val updated = WidgetState.reconcileWithEnabledOrder(
                userId = currentUserId,
                enabledOrder = visible,
                existing = all
            )
            repository.saveWidgetStates(currentUserId, updated)
        }
    }

    fun moveWidget(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val all = currentWidgetList()
            val visible = all.filter { it.isVisible }.sortedBy { it.position }.map { it.type }.toMutableList()
            if (fromIndex !in visible.indices || toIndex !in visible.indices || fromIndex == toIndex) return@launch
            val item = visible.removeAt(fromIndex)
            visible.add(toIndex, item)
            val updated = WidgetState.reconcileWithEnabledOrder(
                userId = currentUserId,
                enabledOrder = visible,
                existing = all
            )
            repository.saveWidgetStates(currentUserId, updated)
        }
    }

    fun reorderWidgets(newOrder: List<DashboardWidgetType>) {
        viewModelScope.launch {
            val all = currentWidgetList()
            val updated = WidgetState.reconcileWithEnabledOrder(
                userId = currentUserId,
                enabledOrder = newOrder.distinct(),
                existing = all
            )
            repository.saveWidgetStates(currentUserId, updated)
        }
    }

    fun resetWidgetsToDefault() {
        viewModelScope.launch {
            val defaults = WidgetState.defaultWidgetsForUser(currentUserId)
            repository.saveWidgetStates(currentUserId, defaults)
        }
    }

    fun updatePreferences(transform: (UserPreferencesModel) -> UserPreferencesModel) {
        viewModelScope.launch {
            val updated = transform(uiState.value.preferences)
            repository.saveUserPreferences(currentUserId, updated)
        }
    }

    fun updateThemeMode(mode: ThemeMode) = updatePreferences { it.copy(themeMode = mode) }

    // --- TASKS ---
    fun saveTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.saveTask(currentUserId, task)
        }
    }

    fun toggleTaskCompleted(task: TaskEntity) {
        viewModelScope.launch {
            val nowCompleted = task.status != TaskStatus.COMPLETED.name
            val updated = task.copy(
                status = if (nowCompleted) TaskStatus.COMPLETED.name else TaskStatus.PENDING.name,
                completedDateIso = if (nowCompleted) DateUtils.todayIso() else ""
            )
            repository.saveTask(currentUserId, updated)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch { repository.deleteTask(currentUserId, taskId) }
    }

    // --- HABITS ---
    fun saveHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.saveHabit(currentUserId, habit)
            if (habit.reminderTime.matches(Regex("^\\d{2}:\\d{2}$"))) {
                val parts = habit.reminderTime.split(":")
                val hr = parts[0].toIntOrNull()?.coerceIn(0, 23) ?: 8
                val min = parts[1].toIntOrNull()?.coerceIn(0, 59) ?: 0
                val cal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, hr)
                    set(java.util.Calendar.MINUTE, min)
                    set(java.util.Calendar.SECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(java.util.Calendar.DAY_OF_YEAR, 1)
                    }
                }
                val prefs = uiState.value.preferences
                ReminderWorker.scheduleReminder(
                    context = appContext,
                    reminderId = "habit_${habit.id}",
                    title = "Habit Reminder: ${habit.title}",
                    moduleType = "HABIT",
                    triggerEpochMs = cal.timeInMillis,
                    quietStart = prefs.quietHoursStart,
                    quietEnd = prefs.quietHoursEnd
                )
            }
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            ReminderWorker.cancelReminder(appContext, "habit_$habitId")
            repository.deleteHabit(currentUserId, habitId)
        }
    }

    fun toggleHabitForDate(habitId: String, dateIso: String, currentlyDone: Boolean) {
        viewModelScope.launch {
            val habit = uiState.value.habits.firstOrNull { it.id == habitId }
            val target = habit?.targetValue?.coerceAtLeast(1) ?: 1
            repository.toggleHabitLog(currentUserId, habitId, dateIso, !currentlyDone, target)
        }
    }

    fun incrementHabitProgress(habit: HabitEntity, dateIso: String, delta: Int) {
        viewModelScope.launch {
            val currentLog = uiState.value.habitLogs.firstOrNull {
                it.habitId == habit.id && it.dateIso == dateIso
            }
            val currentVal = when {
                currentLog == null -> 0
                currentLog.progressValue > 0 -> currentLog.progressValue
                currentLog.completed -> habit.targetValue
                else -> 0
            }
            val nextVal = (currentVal + delta).coerceIn(0, 200000)
            repository.logHabitProgress(
                uid = currentUserId,
                habitId = habit.id,
                dateIso = dateIso,
                progressValue = nextVal,
                targetValue = habit.targetValue
            )
        }
    }

    // --- CHALLENGES ---
    fun saveChallenge(challenge: ChallengeEntity) {
        viewModelScope.launch { repository.saveChallenge(currentUserId, challenge) }
    }

    fun deleteChallenge(challengeId: String) {
        viewModelScope.launch { repository.deleteChallenge(currentUserId, challengeId) }
    }

    fun setChallengeDayStatus(challengeId: String, dateIso: String, status: ChallengeDayStatus) {
        viewModelScope.launch {
            val existing = uiState.value.challengeLogs.firstOrNull {
                it.challengeId == challengeId && it.dateIso == dateIso
            }
            val result = repository.logChallengeDay(currentUserId, challengeId, dateIso, status, existing)
            result.onFailure { err -> syncError.value = err.message }
        }
    }

    // --- WATER & STEPS ---
    fun addWater(amountMl: Int, dateIso: String = uiState.value.selectedDateIso) {
        viewModelScope.launch { repository.logWater(currentUserId, dateIso, amountMl) }
    }

    fun deleteWaterLog(logId: String) {
        viewModelScope.launch { repository.deleteWaterLog(currentUserId, logId) }
    }

    // --- FOCUS STUDIO (Persistent across process recreation) ---
    fun configureFocusTimer(mode: FocusMode, category: String, plannedMinutes: Int, breakMinutes: Int) {
        viewModelScope.launch {
            val current = uiState.value.timerState
            repository.preferences.saveTimerState(
                currentUserId,
                current.copy(
                    mode = mode,
                    category = category,
                    plannedMinutes = plannedMinutes.coerceIn(1, 300),
                    breakMinutes = breakMinutes.coerceIn(1, 60),
                    isBreakPhase = false,
                    running = false,
                    startedAtEpochMs = 0L,
                    accumulatedElapsedSec = 0,
                    distractionsCount = 0
                )
            )
        }
    }

    fun startOrPauseTimer() {
        viewModelScope.launch {
            val current = uiState.value.timerState
            val now = System.currentTimeMillis()
            if (current.running) {
                val delta = ((now - current.startedAtEpochMs) / 1000L).toInt().coerceAtLeast(0)
                repository.preferences.saveTimerState(
                    currentUserId,
                    current.copy(
                        running = false,
                        startedAtEpochMs = 0L,
                        accumulatedElapsedSec = current.accumulatedElapsedSec + delta
                    )
                )
                ReminderWorker.cancelReminder(appContext, "focus_timer")
            } else {
                repository.preferences.saveTimerState(
                    currentUserId,
                    current.copy(running = true, startedAtEpochMs = now)
                )
                if (current.mode != FocusMode.STOPWATCH) {
                    val targetSec = (if (current.isBreakPhase) current.breakMinutes else current.plannedMinutes) * 60
                    val remSec = (targetSec - current.accumulatedElapsedSec).coerceAtLeast(5)
                    val prefs = uiState.value.preferences
                    ReminderWorker.scheduleReminder(
                        context = appContext,
                        reminderId = "focus_timer",
                        title = "${current.category} focus block complete!",
                        moduleType = "FOCUS",
                        triggerEpochMs = now + remSec * 1000L,
                        quietStart = prefs.quietHoursStart,
                        quietEnd = prefs.quietHoursEnd
                    )
                }
            }
        }
    }

    fun logFocusDistraction() {
        viewModelScope.launch {
            val current = uiState.value.timerState
            repository.preferences.saveTimerState(
                currentUserId,
                current.copy(distractionsCount = current.distractionsCount + 1)
            )
        }
    }

    fun updateFocusNotes(notes: String) {
        viewModelScope.launch {
            val current = uiState.value.timerState
            repository.preferences.saveTimerState(currentUserId, current.copy(sessionNotes = notes))
        }
    }

    fun finishAndSaveFocusSession() {
        viewModelScope.launch {
            val state = uiState.value
            val t = state.timerState
            val elapsed = state.liveElapsedSeconds
            if (elapsed >= 10) {
                repository.saveFocusSession(
                    currentUserId,
                    FocusSessionEntity(
                        userId = currentUserId,
                        dateIso = DateUtils.todayIso(),
                        mode = t.mode.name,
                        category = t.category,
                        plannedMinutes = t.plannedMinutes,
                        actualSeconds = elapsed,
                        distractionsCount = t.distractionsCount,
                        notes = t.sessionNotes
                    )
                )
            }
            ReminderWorker.cancelReminder(appContext, "focus_timer")
            repository.preferences.saveTimerState(
                currentUserId,
                t.copy(
                    running = false,
                    startedAtEpochMs = 0L,
                    accumulatedElapsedSec = 0,
                    distractionsCount = 0,
                    sessionNotes = ""
                )
            )
        }
    }

    fun resetFocusTimer() {
        viewModelScope.launch {
            val t = uiState.value.timerState
            ReminderWorker.cancelReminder(appContext, "focus_timer")
            repository.preferences.saveTimerState(
                currentUserId,
                t.copy(running = false, startedAtEpochMs = 0L, accumulatedElapsedSec = 0, distractionsCount = 0)
            )
        }
    }

    // --- REMINDERS ---
    fun saveReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.saveReminder(currentUserId, reminder)
            val prefs = uiState.value.preferences
            if (reminder.enabled) {
                ReminderWorker.scheduleReminder(
                    context = appContext,
                    reminderId = reminder.id,
                    title = reminder.title,
                    moduleType = reminder.moduleType,
                    triggerEpochMs = reminder.scheduledEpochMs,
                    quietStart = prefs.quietHoursStart,
                    quietEnd = prefs.quietHoursEnd
                )
            } else {
                ReminderWorker.cancelReminder(appContext, reminder.id)
            }
        }
    }

    fun snoozeReminder(reminder: ReminderEntity, minutes: Int = 15) {
        val next = reminder.copy(scheduledEpochMs = System.currentTimeMillis() + minutes * 60_000L, enabled = true)
        saveReminder(next)
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            ReminderWorker.cancelReminder(appContext, reminderId)
            repository.deleteReminder(currentUserId, reminderId)
        }
    }

    // --- NOTES, ROUTINES, GOALS, JOURNAL ---
    fun saveNote(note: NoteEntity) { viewModelScope.launch { repository.saveNote(currentUserId, note) } }
    fun deleteNote(noteId: String) { viewModelScope.launch { repository.deleteNote(currentUserId, noteId) } }

    fun saveRoutine(routine: RoutineEntity) { viewModelScope.launch { repository.saveRoutine(currentUserId, routine) } }
    fun deleteRoutine(routineId: String) { viewModelScope.launch { repository.deleteRoutine(currentUserId, routineId) } }
    fun toggleRoutineStep(routine: RoutineEntity, step: String, dateIso: String = uiState.value.selectedDateIso) {
        viewModelScope.launch {
            val steps = routine.stepsCsv.split("|").map { it.trim() }.filter { it.isNotBlank() }
            val existing = uiState.value.routineLogs.firstOrNull { it.routineId == routine.id && it.dateIso == dateIso }
            val doneSet = existing?.completedStepsCsv?.split("|")?.map { it.trim() }?.filter { it.isNotBlank() }?.toMutableSet() ?: mutableSetOf()
            if (step in doneSet) doneSet.remove(step) else doneSet.add(step)
            val completedAll = steps.isNotEmpty() && steps.all { it in doneSet }
            repository.saveRoutineLog(currentUserId, routine.id, dateIso, doneSet.joinToString("|"), completedAll)
        }
    }

    fun saveGoal(goal: GoalEntity) { viewModelScope.launch { repository.saveGoal(currentUserId, goal) } }
    fun deleteGoal(goalId: String) { viewModelScope.launch { repository.deleteGoal(currentUserId, goalId) } }

    fun toggleGoalMilestone(goal: GoalEntity, milestone: String) {
        viewModelScope.launch {
            val all = goal.milestonesCsv.split("|").map { it.trim() }.filter { it.isNotBlank() }
            val done = goal.completedMilestonesCsv.split("|").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
            if (milestone in done) done.remove(milestone) else done.add(milestone)
            val pct = if (all.isNotEmpty()) ((done.size.toFloat() / all.size) * 100).toInt().coerceIn(0, 100) else goal.progressPercent
            val status = if (pct == 100) GoalStatus.COMPLETED.name else goal.status
            repository.saveGoal(
                currentUserId,
                goal.copy(completedMilestonesCsv = done.joinToString("|"), progressPercent = pct, status = status)
            )
        }
    }

    fun saveJournalEntry(entry: JournalEntryEntity) {
        viewModelScope.launch { repository.saveJournalEntry(currentUserId, entry) }
    }

    // --- BACKUP & EXPORT ---
    suspend fun generateJsonBackup(): String = repository.exportBackupJson(currentUserId)
    suspend fun generateTasksCsv(): String = repository.exportTasksCsv(currentUserId)
    fun parseBackupPreview(rawJson: String): Result<BackupPreview> = repository.parseBackupPreview(rawJson)
    suspend fun restoreBackup(rawJson: String, replaceExisting: Boolean): Result<Unit> =
        repository.restoreFromBackup(currentUserId, rawJson, replaceExisting)
    suspend fun deleteAllUserData(): Result<Unit> {
        v3Repository.deleteAllV3Data(currentUserId)
        return repository.deleteAllUserData(currentUserId)
    }

    // --- LIFE OS v3.0 ACTIONS: PROFILE & AVATAR ---
    fun updateProfile(transform: (UserProfileEntity) -> UserProfileEntity) {
        viewModelScope.launch {
            val updated = transform(uiState.value.profile)
            userDisplayName.value = updated.fullName.ifBlank { "Builder" }
            v3Repository.saveProfile(currentUserId, updated, userEmail.value.ifBlank { null })
        }
    }

    suspend fun saveProfileAvatar(uri: Uri, zoomFactor: Float = 1f): Result<String> =
        v3Repository.saveAvatarFromUri(currentUserId, uri, zoomFactor)

    fun removeProfileAvatar() {
        viewModelScope.launch { v3Repository.removeAvatar(currentUserId) }
    }

    fun toggleWaterReminder(enabled: Boolean, intervalMin: Int = 120) {
        updateProfile { it.copy(waterReminderEnabled = enabled, waterReminderIntervalMin = intervalMin) }
        val prefs = uiState.value.preferences
        if (enabled) {
            ReminderWorker.scheduleReminder(
                context = appContext,
                reminderId = "water_hydration_reminder",
                title = "Hydration check — drink a glass of water!",
                moduleType = "WATER",
                triggerEpochMs = System.currentTimeMillis() + intervalMin * 60_000L,
                quietStart = prefs.quietHoursStart,
                quietEnd = prefs.quietHoursEnd
            )
        } else {
            ReminderWorker.cancelReminder(appContext, "water_hydration_reminder")
        }
    }

    // --- FINANCE & SAVINGS ---
    fun saveTransaction(tx: FinanceTransactionEntity) {
        viewModelScope.launch { v3Repository.saveTransaction(currentUserId, tx) }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch { v3Repository.deleteTransaction(currentUserId, id) }
    }

    fun saveSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch { v3Repository.saveSavingsGoal(currentUserId, goal) }
    }

    fun addContributionToSavingsGoal(goal: SavingsGoalEntity, amount: Double) {
        if (amount <= 0.0) return
        saveSavingsGoal(goal.copy(savedAmount = (goal.savedAmount + amount).coerceAtLeast(0.0)))
    }

    fun deleteSavingsGoal(id: String) {
        viewModelScope.launch { v3Repository.deleteSavingsGoal(currentUserId, id) }
    }

    fun generateFinanceCsv(): String = FinanceEngine.exportCsv(uiState.value.transactions)

    // --- NUTRITION ---
    fun saveNutritionEntry(entry: NutritionEntryEntity) {
        viewModelScope.launch { v3Repository.saveNutritionEntry(currentUserId, entry) }
    }

    fun deleteNutritionEntry(id: String) {
        viewModelScope.launch { v3Repository.deleteNutritionEntry(currentUserId, id) }
    }

    // --- TECH LEARNING HUB ---
    fun toggleLearningTopicCompleted(topic: LearningTopicEntity) {
        viewModelScope.launch {
            v3Repository.saveLearningTopic(currentUserId, topic.copy(completed = !topic.completed))
        }
    }

    fun toggleLearningTopicSaved(topic: LearningTopicEntity) {
        viewModelScope.launch {
            v3Repository.saveLearningTopic(currentUserId, topic.copy(saved = !topic.saved))
        }
    }

    fun updateLearningTopicStudy(topic: LearningTopicEntity, addMinutes: Int, notes: String) {
        viewModelScope.launch {
            v3Repository.saveLearningTopic(
                currentUserId,
                topic.copy(studyMinutes = (topic.studyMinutes + addMinutes).coerceAtLeast(0), notes = notes)
            )
        }
    }

    // --- CLASS TIMETABLE ---
    fun saveClassSchedule(schedule: ClassScheduleEntity) {
        viewModelScope.launch {
            v3Repository.saveClassSchedule(currentUserId, schedule)
            if (schedule.reminderEnabled) {
                val prefs = uiState.value.preferences
                ReminderWorker.scheduleReminder(
                    context = appContext,
                    reminderId = "class_${schedule.id}",
                    title = "Class starting soon: ${schedule.className} (${schedule.startTime})",
                    moduleType = "CLASS",
                    triggerEpochMs = System.currentTimeMillis() + 60 * 60_000L,
                    quietStart = prefs.quietHoursStart,
                    quietEnd = prefs.quietHoursEnd
                )
            } else {
                ReminderWorker.cancelReminder(appContext, "class_${schedule.id}")
            }
        }
    }

    fun deleteClassSchedule(id: String) {
        viewModelScope.launch {
            ReminderWorker.cancelReminder(appContext, "class_$id")
            v3Repository.deleteClassSchedule(currentUserId, id)
        }
    }

    // --- JOB APPLICATIONS & INTERVIEWS ---
    fun saveJobApplication(app: JobApplicationEntity) {
        viewModelScope.launch { v3Repository.saveJobApplication(currentUserId, app) }
    }

    fun toggleJobChecklistItem(app: JobApplicationEntity, item: String) {
        val done = app.completedChecklistCsv.split("|").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
        if (item in done) done.remove(item) else done.add(item)
        saveJobApplication(app.copy(completedChecklistCsv = done.joinToString("|")))
    }

    fun deleteJobApplication(id: String) {
        viewModelScope.launch { v3Repository.deleteJobApplication(currentUserId, id) }
    }

    // --- SAVED MEDIA (Bookmarks, Watch Later, Favorite Quotes) ---
    fun saveMediaBookmark(item: SavedMediaEntity) {
        viewModelScope.launch { v3Repository.saveMediaItem(currentUserId, item) }
    }

    fun deleteMediaBookmark(id: String) {
        viewModelScope.launch { v3Repository.deleteMediaItem(currentUserId, id) }
    }

    // --- PRIVATE MEMORIES (Strictly Separate From Notes) ---
    fun saveMemory(memory: MemoryEntity) {
        viewModelScope.launch { v3Repository.saveMemory(currentUserId, memory) }
    }

    suspend fun saveMemoryPhoto(memoryId: String, uri: Uri): Result<String> =
        v3Repository.saveMemoryPhotoFromUri(currentUserId, memoryId, uri)

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch { v3Repository.deleteMemory(currentUserId, memory.id, memory.photoLocalPath) }
    }

    // --- AUTOMATIC STEP COUNTER (Hardware Sensor + Health Connect — Zero Manual Input) ---
    fun refreshStepSensor() {
        stepTrackingRepository.startLiveTracking()
        syncHealthConnectSteps()
    }

    fun syncHealthConnectSteps() {
        viewModelScope.launch {
            runCatching { stepTrackingRepository.syncHealthConnectAndPersist(currentUserId) }
        }
    }

    // --- SMART NOTIFICATION CENTER ---
    fun updateNotificationPrefs(transform: (NotificationPreferencesEntity) -> NotificationPreferencesEntity) {
        viewModelScope.launch {
            val current = uiState.value.notificationPrefs.copy(userId = currentUserId)
            val updated = transform(current)
            v3Repository.saveNotificationPrefs(currentUserId, updated)
            LifeOsNotificationCenter.syncAutomationSchedules(appContext, currentUserId, updated)
        }
    }

    fun toggleNotificationCategory(category: NotificationCategory, enabled: Boolean) {
        updateNotificationPrefs { p ->
            when (category) {
                NotificationCategory.WATER -> p.copy(waterEnabled = enabled)
                NotificationCategory.STEPS -> p.copy(stepsEnabled = enabled)
                NotificationCategory.GOALS -> p.copy(goalsEnabled = enabled)
                NotificationCategory.TASKS -> p.copy(tasksEnabled = enabled)
                NotificationCategory.HABITS -> p.copy(habitsEnabled = enabled)
                NotificationCategory.TECH_NEWS -> p.copy(techNewsEnabled = enabled)
                NotificationCategory.YOUTUBE_VIDEOS -> p.copy(youtubeVideosEnabled = enabled)
                NotificationCategory.LEARNING -> p.copy(learningEnabled = enabled)
                NotificationCategory.CLASSES -> p.copy(classesEnabled = enabled)
                NotificationCategory.INTERVIEWS -> p.copy(interviewsEnabled = enabled)
                NotificationCategory.FINANCE -> p.copy(financeEnabled = enabled)
                NotificationCategory.MOTIVATION -> p.copy(motivationEnabled = enabled)
                NotificationCategory.DAILY_SUMMARY -> p.copy(dailySummaryEnabled = enabled)
            }
        }
    }

    fun sendNotificationPreview(category: NotificationCategory, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val (title, body) = when (category) {
                NotificationCategory.WATER -> "Hydration Reminder (Preview)" to "Time for a glass of water! Tap +200 ml or +250 ml below once you drink."
                NotificationCategory.STEPS -> "Step Goal Nudge (Preview)" to "You're close to today's step goal. A short walk keeps your streak strong."
                NotificationCategory.GOALS -> "Daily Goal Check-in (Preview)" to "Review your active milestones and log today's progress."
                NotificationCategory.TASKS -> "Task Reminder (Preview)" to "You have priority tasks waiting in your daily plan."
                NotificationCategory.HABITS -> "Habit Streak Protection (Preview)" to "Complete your remaining habits today to extend your streak."
                NotificationCategory.TECH_NEWS -> "IT & Tech News (Preview)" to "Fresh software engineering and AI headlines are ready in Learn."
                NotificationCategory.YOUTUBE_VIDEOS -> "Educational Video (Preview)" to "New programming tutorial available — tap to inspect in LIFE OS."
                NotificationCategory.LEARNING -> "Learning Check-in (Preview)" to "Spend 20 focused minutes on your active tech roadmap."
                NotificationCategory.CLASSES -> "Upcoming Class (Preview)" to "Check your timetable for today's upcoming class session."
                NotificationCategory.INTERVIEWS -> "Interview Prep (Preview)" to "Review your company research and prep checklist before your interview."
                NotificationCategory.FINANCE -> "Budget & Savings (Preview)" to "Log today's expenses and stay aligned with your monthly savings goal."
                NotificationCategory.MOTIVATION -> "Daily Motivation (Preview)" to "\"Consistency beats intensity when you show up every single day.\""
                NotificationCategory.DAILY_SUMMARY -> "LIFE OS Daily Summary (Preview)" to "Tasks, water, steps, learning, and spending snapshot ready."
            }
            val ok = LifeOsNotificationCenter.postSmartNotification(
                context = appContext,
                uid = currentUserId,
                category = category,
                title = title,
                message = body,
                deepLinkRoute = category.defaultDeepLink,
                isPreview = true
            )
            onResult(ok)
        }
    }

    fun clearNotificationLogs() {
        viewModelScope.launch { v3Repository.clearNotificationLogs(currentUserId) }
    }

    private data class CoreBundle(
        val tasks: List<TaskEntity>,
        val habits: List<HabitEntity>,
        val habitLogs: List<HabitLogEntity>,
        val challenges: List<ChallengeEntity>,
        val challengeLogs: List<ChallengeLogEntity>
    )

    private data class ExtraBundle(
        val notes: List<NoteEntity>,
        val routines: List<RoutineEntity>,
        val routineLogs: List<RoutineLogEntity>,
        val goals: List<GoalEntity>,
        val journal: List<JournalEntryEntity>
    )

    private data class WellnessBundle(
        val focusSessions: List<FocusSessionEntity>,
        val waterLogs: List<WaterLogEntity>,
        val stepLogs: List<StepLogEntity>,
        val reminders: List<ReminderEntity>,
        val prefs: UserPreferencesModel
    )

    private data class MetaBundle(
        val dateIso: String,
        val timer: PersistedTimerState,
        val tick: Long,
        val syncError: String?,
        val widgets: List<WidgetState>
    )

    private data class V3HealthFinanceBundle(
        val profile: UserProfileEntity,
        val transactions: List<FinanceTransactionEntity>,
        val savingsGoals: List<SavingsGoalEntity>,
        val nutritionEntries: List<NutritionEntryEntity>
    )

    private data class V3LearnCareerBundle(
        val learningTopics: List<LearningTopicEntity>,
        val classSchedules: List<ClassScheduleEntity>,
        val jobApplications: List<JobApplicationEntity>,
        val savedMedia: List<SavedMediaEntity>
    )

    private data class V4ExtrasBundle(
        val memories: List<MemoryEntity>,
        val notificationPrefs: NotificationPreferencesEntity,
        val notificationLogs: List<NotificationLogEntity>,
        val stepSensorStatus: LiveStepSensorStatus
    )

    private data class V3Bundle(
        val profile: UserProfileEntity,
        val transactions: List<FinanceTransactionEntity>,
        val savingsGoals: List<SavingsGoalEntity>,
        val nutritionEntries: List<NutritionEntryEntity>,
        val learningTopics: List<LearningTopicEntity>,
        val classSchedules: List<ClassScheduleEntity>,
        val jobApplications: List<JobApplicationEntity>,
        val savedMedia: List<SavedMediaEntity>,
        val memories: List<MemoryEntity>,
        val notificationPrefs: NotificationPreferencesEntity,
        val notificationLogs: List<NotificationLogEntity>,
        val stepSensorStatus: LiveStepSensorStatus
    )

    companion object {
        fun factory(
            context: Context,
            userId: String,
            displayName: String,
            email: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val appCtx = context.applicationContext
                val repo = LifeOsRepository(appCtx)
                val v3Repo = LifeOsV3Repository(appCtx)
                return LifeOsViewModel(repo, v3Repo, appCtx, userId, displayName, email) as T
            }
        }
    }
}
