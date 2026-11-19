package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

data class BackupPreview(
    val tasksCount: Int,
    val habitsCount: Int,
    val challengesCount: Int,
    val notesCount: Int,
    val goalsCount: Int,
    val journalCount: Int,
    val focusSessionsCount: Int,
    val waterLogsCount: Int,
    val stepLogsCount: Int,
    val remindersCount: Int,
    val rawJson: String
)

class LifeOsRepository(
    private val db: FirebaseFirestore,
    private val dao: LifeOsDao,
    private val widgetDao: DashboardWidgetDao,
    val preferences: PreferencesRepository,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        dao = LifeOsDatabase.getInstance(context.applicationContext).dao(),
        widgetDao = LifeOsDatabase.getInstance(context.applicationContext).dashboardWidgetDao(),
        preferences = PreferencesRepository(context.applicationContext)
    )

    private fun requireUserId(expectedUid: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUid) { "Signed-in user changed; refusing cross-user access." }
        return uid
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    // Reactive Room Flows (instant offline-first source of truth)
    fun observeWidgetStates(uid: String): Flow<List<WidgetState>> =
        widgetDao.observeWidgetStates(uid).map { entities ->
            if (entities.isEmpty()) {
                WidgetState.defaultWidgetsForUser(uid)
            } else {
                val mapped = entities.map { it.toWidgetState() }
                // Ensure newly added widget types (e.g., MOTIVATION_GOAL) are also represented
                val existingTypes = mapped.map { it.type }.toSet()
                val missing = DashboardWidgetType.DEFAULT_ORDER
                    .filterNot { it in existingTypes }
                    .mapIndexed { offset, type ->
                        WidgetState(
                            id = WidgetState.instanceId(uid, type),
                            userId = uid,
                            type = type,
                            isVisible = true,
                            position = mapped.size + offset
                        )
                    }
                WidgetState.normalizePositions(mapped + missing)
            }
        }

    suspend fun ensureDefaultWidgetStates(uid: String) {
        val current = widgetDao.getWidgetStatesOnce(uid)
        if (current.isEmpty()) {
            val prefs = preferences.observePreferences(uid).first()
            val seeded = WidgetState.reconcileWithEnabledOrder(
                userId = uid,
                enabledOrder = prefs.enabledWidgets
            )
            widgetDao.replaceUserWidgetStates(uid, seeded.map { it.toEntity() })
        } else {
            val existingTypes = current.map { it.widgetType }.toSet()
            val missing = DashboardWidgetType.DEFAULT_ORDER.filterNot { it in existingTypes }
            if (missing.isNotEmpty()) {
                val now = System.currentTimeMillis()
                val currentModels = current.map { it.toWidgetState() }
                val additions = missing.mapIndexed { idx, type ->
                    WidgetState(
                        id = WidgetState.instanceId(uid, type),
                        userId = uid,
                        type = type,
                        isVisible = true,
                        position = if (type == DashboardWidgetType.MOTIVATION_GOAL) 1 else currentModels.size + idx,
                        createdAtMs = now,
                        updatedAtMs = now
                    )
                }
                val merged = WidgetState.normalizePositions(currentModels + additions, now)
                widgetDao.replaceUserWidgetStates(uid, merged.map { it.toEntity() })
            }
        }
    }

    suspend fun saveWidgetStates(uid: String, states: List<WidgetState>): Result<Unit> {
        val now = System.currentTimeMillis()
        val normalized = WidgetState.normalizePositions(
            states.map { it.copy(userId = uid, updatedAtMs = now) },
            updatedAtMs = now
        )
        widgetDao.replaceUserWidgetStates(uid, normalized.map { it.toEntity() })
        val currentPrefs = preferences.observePreferences(uid).first()
        val visibleOrdered = normalized.filter { it.isVisible }.sortedBy { it.position }.map { it.type }
        return saveUserPreferences(uid, currentPrefs.copy(enabledWidgets = visibleOrdered))
    }

    fun observeTasks(uid: String): Flow<List<TaskEntity>> = dao.observeTasks(uid)
    fun observeHabits(uid: String): Flow<List<HabitEntity>> = dao.observeHabits(uid)
    fun observeHabitLogs(uid: String): Flow<List<HabitLogEntity>> = dao.observeHabitLogs(uid)
    fun observeChallenges(uid: String): Flow<List<ChallengeEntity>> = dao.observeChallenges(uid)
    fun observeChallengeLogs(uid: String): Flow<List<ChallengeLogEntity>> = dao.observeChallengeLogs(uid)
    fun observeNotes(uid: String): Flow<List<NoteEntity>> = dao.observeNotes(uid)
    fun observeRoutines(uid: String): Flow<List<RoutineEntity>> = dao.observeRoutines(uid)
    fun observeRoutineLogs(uid: String): Flow<List<RoutineLogEntity>> = dao.observeRoutineLogs(uid)
    fun observeGoals(uid: String): Flow<List<GoalEntity>> = dao.observeGoals(uid)
    fun observeJournalEntries(uid: String): Flow<List<JournalEntryEntity>> = dao.observeJournalEntries(uid)
    fun observeFocusSessions(uid: String): Flow<List<FocusSessionEntity>> = dao.observeFocusSessions(uid)
    fun observeWaterLogs(uid: String): Flow<List<WaterLogEntity>> = dao.observeWaterLogs(uid)
    fun observeStepLogs(uid: String): Flow<List<StepLogEntity>> = dao.observeStepLogs(uid)
    fun observeReminders(uid: String): Flow<List<ReminderEntity>> = dao.observeReminders(uid)

    private suspend fun upsertFirestoreDoc(
        docPath: String,
        createFields: Map<String, Any?>,
        updateFields: Map<String, Any?>
    ): Result<Unit> {
        return try {
            val ref = db.document(docPath)
            val snap = ref.get().await()
            if (snap.exists()) {
                val cleanUpdate = updateFields.filterValues { it != null }.toMutableMap()
                cleanUpdate["updatedAt"] = FieldValue.serverTimestamp()
                ref.update(cleanUpdate).await()
            } else {
                val cleanCreate = createFields.filterValues { it != null }.toMutableMap()
                cleanCreate["createdAt"] = FieldValue.serverTimestamp()
                cleanCreate["updatedAt"] = FieldValue.serverTimestamp()
                ref.set(cleanCreate).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun deleteFirestoreDoc(docPath: String): Result<Unit> {
        return try {
            db.document(docPath).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncUserProfile(uid: String, displayName: String, email: String?): Result<Unit> {
        return try {
            requireUserId(uid)
            val safeName = displayName.trim().ifBlank { "Life OS User" }.take(120)
            upsertFirestoreDoc(
                docPath = "users/$uid",
                createFields = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to email?.take(200),
                    "timezone" to "Asia/Kolkata"
                ),
                updateFields = mapOf(
                    "displayName" to safeName,
                    "email" to email?.take(200),
                    "timezone" to "Asia/Kolkata"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun saveUserPreferences(uid: String, prefs: UserPreferencesModel): Result<Unit> {
        preferences.savePreferences(uid, prefs)
        return try {
            requireUserId(uid)
            val widgetNames = prefs.enabledWidgets.map { it.name }.take(20)
            val fields = mapOf<String, Any?>(
                "enabledWidgets" to widgetNames,
                "themeMode" to prefs.themeMode.name,
                "weightTasks" to prefs.weightTasks.coerceIn(0, 100),
                "weightHabits" to prefs.weightHabits.coerceIn(0, 100),
                "weightChallenges" to prefs.weightChallenges.coerceIn(0, 100),
                "weightWater" to prefs.weightWater.coerceIn(0, 100),
                "weightSteps" to prefs.weightSteps.coerceIn(0, 100),
                "waterTargetMl" to prefs.waterTargetMl.coerceIn(250, 20000),
                "stepsTarget" to prefs.stepsTarget.coerceIn(500, 100000),
                "focusTargetMinutes" to prefs.focusTargetMinutes.coerceIn(5, 960),
                "quietHoursStart" to prefs.quietHoursStart.coerceIn(0, 23),
                "quietHoursEnd" to prefs.quietHoursEnd.coerceIn(0, 23)
            )
            upsertFirestoreDoc(
                docPath = "users/$uid/userPreferences/dashboard",
                createFields = fields + ("userId" to uid),
                updateFields = fields
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/userPreferences/dashboard")
            Result.failure(e)
        }
    }

    // Tasks
    suspend fun saveTask(uid: String, task: TaskEntity): Result<Unit> {
        val clean = task.copy(
            userId = uid,
            title = task.title.trim().take(200),
            description = task.description.trim().take(2000),
            category = task.category.trim().ifBlank { "Personal" }.take(60),
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertTask(clean)
        val mutableFields = mapOf<String, Any?>(
            "title" to clean.title,
            "description" to clean.description.ifBlank { null },
            "priority" to clean.priority,
            "status" to clean.status,
            "dueDateIso" to clean.dueDateIso.take(20),
            "dueTimeText" to clean.dueTimeText.ifBlank { null },
            "category" to clean.category,
            "labelsCsv" to clean.labelsCsv.ifBlank { null },
            "recurrence" to clean.recurrence,
            "timeBlockStart" to clean.timeBlockStart.ifBlank { null },
            "timeBlockEnd" to clean.timeBlockEnd.ifBlank { null },
            "completedDateIso" to clean.completedDateIso.ifBlank { null }
        )
        return upsertFirestoreDoc(
            docPath = "users/$uid/tasks/${clean.id}",
            createFields = mutableFields + ("userId" to uid),
            updateFields = mutableFields
        )
    }

    suspend fun deleteTask(uid: String, taskId: String): Result<Unit> {
        dao.deleteTask(uid, taskId)
        return deleteFirestoreDoc("users/$uid/tasks/$taskId")
    }

    // Habits & Logs
    suspend fun saveHabit(uid: String, habit: HabitEntity): Result<Unit> {
        val safeAuto = HabitAutoSource.fromNameOrDefault(habit.autoSource).name
        val clean = habit.copy(
            userId = uid,
            title = habit.title.trim().take(150),
            weekdaysMask = habit.weekdaysMask.coerceIn(1, 127),
            targetValue = habit.targetValue.coerceIn(1, 200000),
            unit = habit.unit.trim().ifBlank { "Check-in" }.take(30),
            autoSource = safeAuto,
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertHabit(clean)
        val fields = mapOf<String, Any?>(
            "title" to clean.title,
            "scheduleType" to clean.scheduleType,
            "weekdaysMask" to clean.weekdaysMask,
            "startDateIso" to clean.startDateIso.take(20),
            "endDateIso" to clean.endDateIso.ifBlank { null },
            "reminderTime" to clean.reminderTime.ifBlank { null },
            "targetValue" to clean.targetValue,
            "unit" to clean.unit,
            "autoSource" to clean.autoSource
        )
        return upsertFirestoreDoc(
            docPath = "users/$uid/habits/${clean.id}",
            createFields = fields + ("userId" to uid),
            updateFields = fields
        )
    }

    suspend fun deleteHabit(uid: String, habitId: String): Result<Unit> {
        dao.deleteHabit(uid, habitId)
        return deleteFirestoreDoc("users/$uid/habits/$habitId")
    }

    suspend fun toggleHabitLog(
        uid: String,
        habitId: String,
        dateIso: String,
        completed: Boolean,
        targetValue: Int = 1
    ): Result<Unit> {
        val progress = if (completed) targetValue.coerceAtLeast(1) else 0
        return logHabitProgress(uid, habitId, dateIso, progress, targetValue)
    }

    suspend fun logHabitProgress(
        uid: String,
        habitId: String,
        dateIso: String,
        progressValue: Int,
        targetValue: Int
    ): Result<Unit> {
        val safeProgress = progressValue.coerceIn(0, 200000)
        val safeTarget = targetValue.coerceAtLeast(1)
        val completed = safeProgress >= safeTarget
        val logId = "${habitId}_${dateIso.replace("-", "")}"
        val entity = HabitLogEntity(
            id = logId,
            userId = uid,
            habitId = habitId,
            dateIso = dateIso,
            completed = completed,
            progressValue = safeProgress,
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertHabitLog(entity)
        return upsertFirestoreDoc(
            docPath = "users/$uid/habitLogs/$logId",
            createFields = mapOf(
                "userId" to uid,
                "habitId" to habitId,
                "dateIso" to dateIso,
                "completed" to completed,
                "progressValue" to safeProgress
            ),
            updateFields = mapOf(
                "completed" to completed,
                "progressValue" to safeProgress
            )
        )
    }

    // Challenges & Logs
    suspend fun saveChallenge(uid: String, challenge: ChallengeEntity): Result<Unit> {
        val clean = challenge.copy(
            userId = uid,
            title = challenge.title.trim().take(160),
            description = challenge.description.trim().take(1000),
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertChallenge(clean)
        val fields = mapOf<String, Any?>(
            "title" to clean.title,
            "description" to clean.description.ifBlank { null },
            "startDateIso" to clean.startDateIso.take(10),
            "endDateIso" to clean.endDateIso.take(10)
        )
        return upsertFirestoreDoc(
            docPath = "users/$uid/challenges/${clean.id}",
            createFields = fields + ("userId" to uid),
            updateFields = fields
        )
    }

    suspend fun deleteChallenge(uid: String, challengeId: String): Result<Unit> {
        dao.deleteChallenge(uid, challengeId)
        return deleteFirestoreDoc("users/$uid/challenges/$challengeId")
    }

    suspend fun logChallengeDay(
        uid: String,
        challengeId: String,
        dateIso: String,
        newStatus: ChallengeDayStatus,
        existingLog: ChallengeLogEntity?
    ): Result<Unit> {
        if (DateUtils.isFutureIso(dateIso)) {
            return Result.failure(IllegalArgumentException("Cannot mark future dates on a challenge."))
        }
        val logId = "${challengeId}_${dateIso.replace("-", "")}"
        val prevStatus = existingLog?.status ?: ChallengeDayStatus.NOT_RECORDED.name
        val auditLine = "${DateUtils.formatTime(System.currentTimeMillis())}: $prevStatus -> ${newStatus.name}"
        val combinedAudit = listOfNotNull(
            existingLog?.auditHistory?.takeIf { it.isNotBlank() },
            auditLine
        ).joinToString(" | ").take(4000)

        val entity = ChallengeLogEntity(
            id = logId,
            userId = uid,
            challengeId = challengeId,
            dateIso = dateIso,
            status = newStatus.name,
            auditHistory = combinedAudit,
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertChallengeLog(entity)
        return upsertFirestoreDoc(
            docPath = "users/$uid/challengeLogs/$logId",
            createFields = mapOf(
                "userId" to uid,
                "challengeId" to challengeId,
                "dateIso" to dateIso,
                "status" to newStatus.name,
                "auditHistory" to combinedAudit
            ),
            updateFields = mapOf(
                "status" to newStatus.name,
                "auditHistory" to combinedAudit
            )
        )
    }

    // Notes
    suspend fun saveNote(uid: String, note: NoteEntity): Result<Unit> {
        val clean = note.copy(
            userId = uid,
            title = note.title.trim().take(200),
            content = note.content.take(20000),
            folder = note.folder.trim().ifBlank { "Quick Notes" }.take(60),
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertNote(clean)
        val fields = mapOf<String, Any?>(
            "title" to clean.title,
            "content" to clean.content,
            "folder" to clean.folder,
            "labelsCsv" to clean.labelsCsv.ifBlank { null },
            "pinned" to clean.pinned,
            "favorite" to clean.favorite,
            "isCodeNote" to clean.isCodeNote,
            "archived" to clean.archived,
            "attachmentUri" to clean.attachmentUri.ifBlank { null }
        )
        return upsertFirestoreDoc(
            docPath = "users/$uid/notes/${clean.id}",
            createFields = fields + ("userId" to uid),
            updateFields = fields
        )
    }

    suspend fun deleteNote(uid: String, noteId: String): Result<Unit> {
        dao.deleteNote(uid, noteId)
        return deleteFirestoreDoc("users/$uid/notes/$noteId")
    }

    // Routines & Logs
    suspend fun saveRoutine(uid: String, routine: RoutineEntity): Result<Unit> {
        val clean = routine.copy(
            userId = uid,
            title = routine.title.trim().take(140),
            stepsCsv = routine.stepsCsv.take(4000),
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertRoutine(clean)
        val fields = mapOf<String, Any?>(
            "title" to clean.title,
            "routineType" to clean.routineType,
            "stepsCsv" to clean.stepsCsv,
            "recurrence" to clean.recurrence
        )
        return upsertFirestoreDoc(
            docPath = "users/$uid/routines/${clean.id}",
            createFields = fields + ("userId" to uid),
            updateFields = fields
        )
    }

    suspend fun deleteRoutine(uid: String, routineId: String): Result<Unit> {
        dao.deleteRoutine(uid, routineId)
        return deleteFirestoreDoc("users/$uid/routines/$routineId")
    }

    suspend fun saveRoutineLog(
        uid: String,
        routineId: String,
        dateIso: String,
        completedStepsCsv: String,
        completedAll: Boolean
    ): Result<Unit> {
        val logId = "${routineId}_${dateIso.replace("-", "")}"
        val entity = RoutineLogEntity(
            id = logId,
            userId = uid,
            routineId = routineId,
            dateIso = dateIso,
            completedStepsCsv = completedStepsCsv,
            completedAll = completedAll,
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertRoutineLog(entity)
        return upsertFirestoreDoc(
            docPath = "users/$uid/routineLogs/$logId",
            createFields = mapOf(
                "userId" to uid,
                "routineId" to routineId,
                "dateIso" to dateIso,
                "completedStepsCsv" to completedStepsCsv,
                "completedAll" to completedAll
            ),
            updateFields = mapOf(
                "completedStepsCsv" to completedStepsCsv,
                "completedAll" to completedAll
            )
        )
    }

    // Goals
    suspend fun saveGoal(uid: String, goal: GoalEntity): Result<Unit> {
        val clean = goal.copy(
            userId = uid,
            title = goal.title.trim().take(180),
            description = goal.description.trim().take(2000),
            progressPercent = goal.progressPercent.coerceIn(0, 100),
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertGoal(clean)
        val fields = mapOf<String, Any?>(
            "title" to clean.title,
            "description" to clean.description.ifBlank { null },
            "horizon" to clean.horizon,
            "priority" to clean.priority,
            "status" to clean.status,
            "targetDateIso" to clean.targetDateIso,
            "progressPercent" to clean.progressPercent,
            "milestonesCsv" to clean.milestonesCsv.ifBlank { null },
            "completedMilestonesCsv" to clean.completedMilestonesCsv.ifBlank { null }
        )
        return upsertFirestoreDoc(
            docPath = "users/$uid/goals/${clean.id}",
            createFields = fields + ("userId" to uid),
            updateFields = fields
        )
    }

    suspend fun deleteGoal(uid: String, goalId: String): Result<Unit> {
        dao.deleteGoal(uid, goalId)
        return deleteFirestoreDoc("users/$uid/goals/$goalId")
    }

    // Journal
    suspend fun saveJournalEntry(uid: String, entry: JournalEntryEntity): Result<Unit> {
        val entryId = entry.id.ifBlank { "journal_${entry.dateIso.replace("-", "")}" }
        val clean = entry.copy(
            id = entryId,
            userId = uid,
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertJournalEntry(clean)
        val fields = mapOf<String, Any?>(
            "highlights" to clean.highlights.ifBlank { null },
            "learned" to clean.learned.ifBlank { null },
            "wentWell" to clean.wentWell.ifBlank { null },
            "wentWrong" to clean.wentWrong.ifBlank { null },
            "tomorrowPlan" to clean.tomorrowPlan.ifBlank { null },
            "gratitude" to clean.gratitude.ifBlank { null },
            "mood" to clean.mood.ifBlank { null },
            "privateEntry" to clean.privateEntry
        )
        return upsertFirestoreDoc(
            docPath = "users/$uid/journalEntries/$entryId",
            createFields = fields + mapOf("userId" to uid, "dateIso" to clean.dateIso),
            updateFields = fields
        )
    }

    // Focus Sessions
    suspend fun saveFocusSession(uid: String, session: FocusSessionEntity): Result<Unit> {
        val clean = session.copy(
            userId = uid,
            category = session.category.trim().ifBlank { "General" }.take(60),
            actualSeconds = session.actualSeconds.coerceIn(0, 43200),
            distractionsCount = session.distractionsCount.coerceIn(0, 500),
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertFocusSession(clean)
        return upsertFirestoreDoc(
            docPath = "users/$uid/focusSessions/${clean.id}",
            createFields = mapOf(
                "userId" to uid,
                "dateIso" to clean.dateIso,
                "mode" to clean.mode,
                "category" to clean.category,
                "plannedMinutes" to clean.plannedMinutes.coerceIn(0, 600),
                "actualSeconds" to clean.actualSeconds,
                "distractionsCount" to clean.distractionsCount,
                "notes" to clean.notes.ifBlank { null }
            ),
            updateFields = mapOf(
                "notes" to clean.notes.ifBlank { null },
                "distractionsCount" to clean.distractionsCount,
                "actualSeconds" to clean.actualSeconds
            )
        )
    }

    // Water Logs
    suspend fun logWater(uid: String, dateIso: String, amountMl: Int): Result<Unit> {
        val safeMl = amountMl.coerceIn(10, 5000)
        val log = WaterLogEntity(
            userId = uid,
            dateIso = dateIso,
            amountMl = safeMl
        )
        dao.upsertWaterLog(log)
        return upsertFirestoreDoc(
            docPath = "users/$uid/waterLogs/${log.id}",
            createFields = mapOf(
                "userId" to uid,
                "dateIso" to dateIso,
                "amountMl" to safeMl
            ),
            updateFields = mapOf("amountMl" to safeMl)
        )
    }

    suspend fun deleteWaterLog(uid: String, logId: String): Result<Unit> {
        dao.deleteWaterLog(uid, logId)
        return deleteFirestoreDoc("users/$uid/waterLogs/$logId")
    }

    // Step Logs (one deduplicated record per dateIso)
    suspend fun saveStepLog(uid: String, dateIso: String, steps: Int, source: String = "MANUAL"): Result<Unit> {
        val logId = "steps_${dateIso.replace("-", "")}"
        val safeSteps = steps.coerceIn(0, 200000)
        val safeSource = if (source == "HEALTH_CONNECT") "HEALTH_CONNECT" else "MANUAL"
        val log = StepLogEntity(
            id = logId,
            userId = uid,
            dateIso = dateIso,
            steps = safeSteps,
            source = safeSource,
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertStepLog(log)
        return upsertFirestoreDoc(
            docPath = "users/$uid/stepLogs/$logId",
            createFields = mapOf(
                "userId" to uid,
                "dateIso" to dateIso,
                "steps" to safeSteps,
                "source" to safeSource
            ),
            updateFields = mapOf(
                "steps" to safeSteps,
                "source" to safeSource
            )
        )
    }

    // Reminders
    suspend fun saveReminder(uid: String, reminder: ReminderEntity): Result<Unit> {
        val clean = reminder.copy(
            userId = uid,
            title = reminder.title.trim().take(160),
            updatedAtMs = System.currentTimeMillis()
        )
        dao.upsertReminder(clean)
        val fields = mapOf<String, Any?>(
            "title" to clean.title,
            "moduleType" to clean.moduleType,
            "scheduledEpochMs" to clean.scheduledEpochMs,
            "recurrence" to clean.recurrence,
            "enabled" to clean.enabled
        )
        return upsertFirestoreDoc(
            docPath = "users/$uid/reminders/${clean.id}",
            createFields = fields + ("userId" to uid),
            updateFields = fields
        )
    }

    suspend fun deleteReminder(uid: String, reminderId: String): Result<Unit> {
        dao.deleteReminder(uid, reminderId)
        return deleteFirestoreDoc("users/$uid/reminders/$reminderId")
    }

    // Bidirectional Realtime Cloud Sync -> Room
    fun startCloudSync(uid: String, scope: CoroutineScope, onSyncError: (String) -> Unit): List<Job> {
        if (uid.isBlank() || auth.currentUser?.uid != uid) return emptyList()
        val jobs = mutableListOf<Job>()

        fun <T> launchSubcollectionSync(
            subcol: String,
            mapper: (DocumentSnapshot) -> T?,
            onItems: suspend (List<T>) -> Unit
        ) {
            val path = "users/$uid/$subcol"
            jobs += scope.launch {
                userDoc(uid).collection(subcol)
                    .snapshots()
                    .catch { err ->
                        val fsErr = generateSequence(err) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (fsErr == null && err is CancellationException) throw err
                        if (auth.currentUser?.uid == uid) {
                            handleFirestoreError(fsErr ?: err, OperationType.LIST, path)
                            onSyncError(fsErr?.localizedMessage ?: err.localizedMessage ?: "Cloud sync issue")
                        }
                    }
                    .collect { snap ->
                        val items = snap.documents.mapNotNull { doc -> runCatching { mapper(doc) }.getOrNull() }
                        if (items.isNotEmpty()) onItems(items)
                    }
            }
        }

        // Sync Dashboard Preferences
        jobs += scope.launch {
            val prefPath = "users/$uid/userPreferences/dashboard"
            userDoc(uid).collection("userPreferences").document("dashboard")
                .snapshots()
                .catch { err ->
                    val fsErr = generateSequence(err) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && err is CancellationException) throw err
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: err, OperationType.GET, prefPath)
                    }
                }
                .collect { doc ->
                    if (doc.exists()) {
                        val data = doc.data ?: return@collect
                        val rawList = (data["enabledWidgets"] as? List<*>)?.mapNotNull { it as? String }
                        val widgets = rawList?.mapNotNull { DashboardWidgetType.fromNameOrNull(it) }
                            ?: DashboardWidgetType.DEFAULT_ORDER
                        val themeStr = data["themeMode"] as? String ?: ThemeMode.SYSTEM.name
                        val theme = ThemeMode.entries.firstOrNull { it.name == themeStr } ?: ThemeMode.SYSTEM
                        preferences.savePreferences(
                            uid,
                            UserPreferencesModel(
                                enabledWidgets = widgets,
                                themeMode = theme,
                                weightTasks = (data["weightTasks"] as? Number)?.toInt() ?: 30,
                                weightHabits = (data["weightHabits"] as? Number)?.toInt() ?: 30,
                                weightChallenges = (data["weightChallenges"] as? Number)?.toInt() ?: 20,
                                weightWater = (data["weightWater"] as? Number)?.toInt() ?: 10,
                                weightSteps = (data["weightSteps"] as? Number)?.toInt() ?: 10,
                                waterTargetMl = (data["waterTargetMl"] as? Number)?.toInt() ?: 2500,
                                stepsTarget = (data["stepsTarget"] as? Number)?.toInt() ?: 8000,
                                focusTargetMinutes = (data["focusTargetMinutes"] as? Number)?.toInt() ?: 90,
                                quietHoursStart = (data["quietHoursStart"] as? Number)?.toInt() ?: 22,
                                quietHoursEnd = (data["quietHoursEnd"] as? Number)?.toInt() ?: 7
                            )
                        )
                        val existingEntities = widgetDao.getWidgetStatesOnce(uid).map { it.toWidgetState() }
                        val reconciled = WidgetState.reconcileWithEnabledOrder(
                            userId = uid,
                            enabledOrder = widgets,
                            existing = existingEntities
                        )
                        widgetDao.replaceUserWidgetStates(uid, reconciled.map { it.toEntity() })
                    }
                }
        }

        launchSubcollectionSync("tasks", { d ->
            val data = d.data ?: return@launchSubcollectionSync null
            TaskEntity(
                id = d.id,
                userId = uid,
                title = data["title"] as? String ?: "",
                description = data["description"] as? String ?: "",
                priority = data["priority"] as? String ?: TaskPriority.MEDIUM.name,
                status = data["status"] as? String ?: TaskStatus.PENDING.name,
                dueDateIso = data["dueDateIso"] as? String ?: DateUtils.todayIso(),
                dueTimeText = data["dueTimeText"] as? String ?: "",
                category = data["category"] as? String ?: "Personal",
                labelsCsv = data["labelsCsv"] as? String ?: "",
                recurrence = data["recurrence"] as? String ?: RecurrenceRule.NONE.name,
                timeBlockStart = data["timeBlockStart"] as? String ?: "",
                timeBlockEnd = data["timeBlockEnd"] as? String ?: "",
                completedDateIso = data["completedDateIso"] as? String ?: "",
                createdAtMs = d.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: System.currentTimeMillis(),
                updatedAtMs = d.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: System.currentTimeMillis()
            )
        }, { dao.upsertTasks(it) })

        launchSubcollectionSync("habits", { d ->
            val data = d.data ?: return@launchSubcollectionSync null
            HabitEntity(
                id = d.id,
                userId = uid,
                title = data["title"] as? String ?: "",
                scheduleType = data["scheduleType"] as? String ?: HabitScheduleType.DAILY.name,
                weekdaysMask = (data["weekdaysMask"] as? Number)?.toInt() ?: 127,
                startDateIso = data["startDateIso"] as? String ?: DateUtils.todayIso(),
                endDateIso = data["endDateIso"] as? String ?: "",
                reminderTime = data["reminderTime"] as? String ?: "",
                targetValue = (data["targetValue"] as? Number)?.toInt() ?: 1,
                unit = data["unit"] as? String ?: "Check-in",
                autoSource = data["autoSource"] as? String ?: HabitAutoSource.NONE.name
            )
        }, { dao.upsertHabits(it) })

        launchSubcollectionSync("habitLogs", { d ->
            val data = d.data ?: return@launchSubcollectionSync null
            val completed = data["completed"] as? Boolean ?: false
            HabitLogEntity(
                id = d.id,
                userId = uid,
                habitId = data["habitId"] as? String ?: "",
                dateIso = data["dateIso"] as? String ?: DateUtils.todayIso(),
                completed = completed,
                progressValue = (data["progressValue"] as? Number)?.toInt() ?: if (completed) 1 else 0
            )
        }, { dao.upsertHabitLogs(it) })

        launchSubcollectionSync("challenges", { d ->
            val data = d.data ?: return@launchSubcollectionSync null
            ChallengeEntity(
                id = d.id,
                userId = uid,
                title = data["title"] as? String ?: "",
                description = data["description"] as? String ?: "",
                startDateIso = data["startDateIso"] as? String ?: DateUtils.todayIso(),
                endDateIso = data["endDateIso"] as? String ?: DateUtils.todayIso()
            )
        }, { dao.upsertChallenges(it) })

        launchSubcollectionSync("challengeLogs", { d ->
            val data = d.data ?: return@launchSubcollectionSync null
            ChallengeLogEntity(
                id = d.id,
                userId = uid,
                challengeId = data["challengeId"] as? String ?: "",
                dateIso = data["dateIso"] as? String ?: DateUtils.todayIso(),
                status = data["status"] as? String ?: ChallengeDayStatus.NOT_RECORDED.name,
                auditHistory = data["auditHistory"] as? String ?: ""
            )
        }, { dao.upsertChallengeLogs(it) })

        launchSubcollectionSync("notes", { d ->
            val data = d.data ?: return@launchSubcollectionSync null
            NoteEntity(
                id = d.id,
                userId = uid,
                title = data["title"] as? String ?: "",
                content = data["content"] as? String ?: "",
                folder = data["folder"] as? String ?: "Quick Notes",
                labelsCsv = data["labelsCsv"] as? String ?: "",
                pinned = data["pinned"] as? Boolean ?: false,
                favorite = data["favorite"] as? Boolean ?: false,
                isCodeNote = data["isCodeNote"] as? Boolean ?: false,
                archived = data["archived"] as? Boolean ?: false,
                attachmentUri = data["attachmentUri"] as? String ?: ""
            )
        }, { dao.upsertNotes(it) })

        launchSubcollectionSync("routines", { d ->
            val data = d.data ?: return@launchSubcollectionSync null
            RoutineEntity(
                id = d.id,
                userId = uid,
                title = data["title"] as? String ?: "",
                routineType = data["routineType"] as? String ?: RoutineType.MORNING.name,
                stepsCsv = data["stepsCsv"] as? String ?: "",
                recurrence = data["recurrence"] as? String ?: "DAILY"
            )
        }, { dao.upsertRoutines(it) })

        launchSubcollectionSync("routineLogs", { d ->
            val data = d.data ?: return@launchSubcollectionSync null
            RoutineLogEntity(
                id = d.id,
                userId = uid,
                routineId = data["routineId"] as? String ?: "",
                dateIso = data["dateIso"] as? String ?: DateUtils.todayIso(),
                completedStepsCsv = data["completedStepsCsv"] as? String ?: "",
                completedAll = data["completedAll"] as? Boolean ?: false
            )
        }, { dao.upsertRoutineLogs(it) })

        launchSubcollectionSync("goals", { d ->
            val data = d.data ?: return@launchSubcollectionSync null
            GoalEntity(
                id = d.id,
                userId = uid,
                title = data["title"] as? String ?: "",
                description = data["description"] as? String ?: "",
                horizon = data["horizon"] as? String ?: "30_DAYS",
                priority = data["priority"] as? String ?: TaskPriority.MEDIUM.name,
                status = data["status"] as? String ?: GoalStatus.ACTIVE.name,
                targetDateIso = data["targetDateIso"] as? String ?: DateUtils.todayIso(),
                progressPercent = (data["progressPercent"] as? Number)?.toInt() ?: 0,
                milestonesCsv = data["milestonesCsv"] as? String ?: "",
                completedMilestonesCsv = data["completedMilestonesCsv"] as? String ?: ""
            )
        }, { dao.upsertGoals(it) })

        launchSubcollectionSync("journalEntries", { d ->
            val data = d.data ?: return@launchSubcollectionSync null
            JournalEntryEntity(
                id = d.id,
                userId = uid,
                dateIso = data["dateIso"] as? String ?: DateUtils.todayIso(),
                highlights = data["highlights"] as? String ?: "",
                learned = data["learned"] as? String ?: "",
                wentWell = data["wentWell"] as? String ?: "",
                wentWrong = data["wentWrong"] as? String ?: "",
                tomorrowPlan = data["tomorrowPlan"] as? String ?: "",
                gratitude = data["gratitude"] as? String ?: "",
                mood = data["mood"] as? String ?: "",
                privateEntry = data["privateEntry"] as? Boolean ?: false
            )
        }, { dao.upsertJournalEntries(it) })

        launchSubcollectionSync("focusSessions", { d ->
            val data = d.data ?: return@launchSubcollectionSync null
            FocusSessionEntity(
                id = d.id,
                userId = uid,
                dateIso = data["dateIso"] as? String ?: DateUtils.todayIso(),
                mode = data["mode"] as? String ?: FocusMode.POMODORO.name,
                category = data["category"] as? String ?: "General",
                plannedMinutes = (data["plannedMinutes"] as? Number)?.toInt() ?: 25,
                actualSeconds = (data["actualSeconds"] as? Number)?.toInt() ?: 0,
                distractionsCount = (data["distractionsCount"] as? Number)?.toInt() ?: 0,
                notes = data["notes"] as? String ?: ""
            )
        }, { dao.upsertFocusSessions(it) })

        launchSubcollectionSync("waterLogs", { d ->
            val data = d.data ?: return@launchSubcollectionSync null
            WaterLogEntity(
                id = d.id,
                userId = uid,
                dateIso = data["dateIso"] as? String ?: DateUtils.todayIso(),
                amountMl = (data["amountMl"] as? Number)?.toInt() ?: 250
            )
        }, { dao.upsertWaterLogs(it) })

        launchSubcollectionSync("stepLogs", { d ->
            val data = d.data ?: return@launchSubcollectionSync null
            StepLogEntity(
                id = d.id,
                userId = uid,
                dateIso = data["dateIso"] as? String ?: DateUtils.todayIso(),
                steps = (data["steps"] as? Number)?.toInt() ?: 0,
                source = data["source"] as? String ?: "MANUAL"
            )
        }, { dao.upsertStepLogs(it) })

        launchSubcollectionSync("reminders", { d ->
            val data = d.data ?: return@launchSubcollectionSync null
            ReminderEntity(
                id = d.id,
                userId = uid,
                title = data["title"] as? String ?: "",
                moduleType = data["moduleType"] as? String ?: "TASK",
                scheduledEpochMs = (data["scheduledEpochMs"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                recurrence = data["recurrence"] as? String ?: "NONE",
                enabled = data["enabled"] as? Boolean ?: true
            )
        }, { dao.upsertReminders(it) })

        return jobs
    }

    // Export JSON Backup
    suspend fun exportBackupJson(uid: String): String {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("app", "LIFE_OS")
        root.put("exportedAtIso", DateUtils.todayIso())

        val tasks = dao.observeTasks(uid).first()
        val habits = dao.observeHabits(uid).first()
        val challenges = dao.observeChallenges(uid).first()
        val notes = dao.observeNotes(uid).first()
        val goals = dao.observeGoals(uid).first()
        val journal = dao.observeJournalEntries(uid).first()
        val focus = dao.observeFocusSessions(uid).first()
        val water = dao.observeWaterLogs(uid).first()
        val steps = dao.observeStepLogs(uid).first()
        val reminders = dao.observeReminders(uid).first()
        val prefs = preferences.observePreferences(uid).first()
        val widgetStates = widgetDao.getWidgetStatesOnce(uid).map { it.toWidgetState() }

        root.put("enabledWidgets", JSONArray(prefs.enabledWidgets.map { it.name }))
        root.put("widgetStates", JSONArray(widgetStates.map { ws ->
            JSONObject().apply {
                put("id", ws.id)
                put("type", ws.type.name)
                put("isVisible", ws.isVisible)
                put("position", ws.position)
                put("isPinned", ws.isPinned)
                put("spanSize", ws.spanSize.name)
                put("customConfigJson", ws.customConfigJson)
            }
        }))
        root.put("tasks", JSONArray(tasks.map {
            JSONObject().apply {
                put("id", it.id); put("title", it.title); put("description", it.description)
                put("priority", it.priority); put("status", it.status); put("dueDateIso", it.dueDateIso)
                put("category", it.category); put("recurrence", it.recurrence)
            }
        }))
        root.put("habits", JSONArray(habits.map {
            JSONObject().apply {
                put("id", it.id); put("title", it.title); put("scheduleType", it.scheduleType)
                put("weekdaysMask", it.weekdaysMask); put("startDateIso", it.startDateIso)
            }
        }))
        root.put("challenges", JSONArray(challenges.map {
            JSONObject().apply {
                put("id", it.id); put("title", it.title); put("description", it.description)
                put("startDateIso", it.startDateIso); put("endDateIso", it.endDateIso)
            }
        }))
        root.put("notes", JSONArray(notes.map {
            JSONObject().apply {
                put("id", it.id); put("title", it.title); put("content", it.content)
                put("labelsCsv", it.labelsCsv); put("pinned", it.pinned); put("archived", it.archived)
            }
        }))
        root.put("goals", JSONArray(goals.map {
            JSONObject().apply {
                put("id", it.id); put("title", it.title); put("horizon", it.horizon)
                put("priority", it.priority); put("status", it.status)
                put("targetDateIso", it.targetDateIso); put("progressPercent", it.progressPercent)
            }
        }))
        root.put("journalEntries", JSONArray(journal.map {
            JSONObject().apply {
                put("id", it.id); put("dateIso", it.dateIso); put("highlights", it.highlights)
                put("learned", it.learned); put("gratitude", it.gratitude); put("mood", it.mood)
            }
        }))
        root.put("focusSessions", JSONArray(focus.map {
            JSONObject().apply {
                put("id", it.id); put("dateIso", it.dateIso); put("mode", it.mode)
                put("category", it.category); put("plannedMinutes", it.plannedMinutes)
                put("actualSeconds", it.actualSeconds); put("distractionsCount", it.distractionsCount)
            }
        }))
        root.put("waterLogs", JSONArray(water.map {
            JSONObject().apply { put("id", it.id); put("dateIso", it.dateIso); put("amountMl", it.amountMl) }
        }))
        root.put("stepLogs", JSONArray(steps.map {
            JSONObject().apply { put("id", it.id); put("dateIso", it.dateIso); put("steps", it.steps); put("source", it.source) }
        }))
        root.put("reminders", JSONArray(reminders.map {
            JSONObject().apply {
                put("id", it.id); put("title", it.title); put("moduleType", it.moduleType)
                put("scheduledEpochMs", it.scheduledEpochMs); put("enabled", it.enabled)
            }
        }))
        return root.toString(2)
    }

    suspend fun exportTasksCsv(uid: String): String {
        val tasks = dao.observeTasks(uid).first()
        val sb = StringBuilder("id,title,priority,status,dueDate,category,recurrence\n")
        tasks.forEach { t ->
            val escTitle = t.title.replace("\"", "\"\"")
            sb.append("${t.id},\"$escTitle\",${t.priority},${t.status},${t.dueDateIso},${t.category},${t.recurrence}\n")
        }
        return sb.toString()
    }

    fun parseBackupPreview(rawJson: String): Result<BackupPreview> = runCatching {
        val root = JSONObject(rawJson)
        require(root.optString("app") == "LIFE_OS") { "Not a valid LIFE OS backup file." }
        BackupPreview(
            tasksCount = root.optJSONArray("tasks")?.length() ?: 0,
            habitsCount = root.optJSONArray("habits")?.length() ?: 0,
            challengesCount = root.optJSONArray("challenges")?.length() ?: 0,
            notesCount = root.optJSONArray("notes")?.length() ?: 0,
            goalsCount = root.optJSONArray("goals")?.length() ?: 0,
            journalCount = root.optJSONArray("journalEntries")?.length() ?: 0,
            focusSessionsCount = root.optJSONArray("focusSessions")?.length() ?: 0,
            waterLogsCount = root.optJSONArray("waterLogs")?.length() ?: 0,
            stepLogsCount = root.optJSONArray("stepLogs")?.length() ?: 0,
            remindersCount = root.optJSONArray("reminders")?.length() ?: 0,
            rawJson = rawJson
        )
    }

    suspend fun restoreFromBackup(uid: String, rawJson: String, replaceExisting: Boolean): Result<Unit> = runCatching {
        val root = JSONObject(rawJson)
        require(root.optString("app") == "LIFE_OS") { "Invalid LIFE OS backup file." }
        if (replaceExisting) {
            clearLocalUserData(uid)
        }
        root.optJSONArray("tasks")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                saveTask(uid, TaskEntity(
                    id = o.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                    userId = uid,
                    title = o.optString("title"),
                    description = o.optString("description"),
                    priority = o.optString("priority", TaskPriority.MEDIUM.name),
                    status = o.optString("status", TaskStatus.PENDING.name),
                    dueDateIso = o.optString("dueDateIso", DateUtils.todayIso()),
                    category = o.optString("category", "Personal"),
                    recurrence = o.optString("recurrence", RecurrenceRule.NONE.name)
                ))
            }
        }
        root.optJSONArray("habits")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                saveHabit(uid, HabitEntity(
                    id = o.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                    userId = uid,
                    title = o.optString("title"),
                    scheduleType = o.optString("scheduleType", HabitScheduleType.DAILY.name),
                    weekdaysMask = o.optInt("weekdaysMask", 127),
                    startDateIso = o.optString("startDateIso", DateUtils.todayIso())
                ))
            }
        }
        root.optJSONArray("notes")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                saveNote(uid, NoteEntity(
                    id = o.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                    userId = uid,
                    title = o.optString("title"),
                    content = o.optString("content"),
                    labelsCsv = o.optString("labelsCsv"),
                    pinned = o.optBoolean("pinned"),
                    archived = o.optBoolean("archived")
                ))
            }
        }
        root.optJSONArray("goals")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                saveGoal(uid, GoalEntity(
                    id = o.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                    userId = uid,
                    title = o.optString("title"),
                    horizon = o.optString("horizon", "30_DAYS"),
                    priority = o.optString("priority", TaskPriority.HIGH.name),
                    status = o.optString("status", GoalStatus.ACTIVE.name),
                    targetDateIso = o.optString("targetDateIso", DateUtils.todayIso()),
                    progressPercent = o.optInt("progressPercent", 0)
                ))
            }
        }
    }

    suspend fun clearLocalUserData(uid: String) {
        widgetDao.clearWidgetStates(uid)
        dao.clearTasks(uid)
        dao.clearHabits(uid)
        dao.clearHabitLogs(uid)
        dao.clearChallenges(uid)
        dao.clearChallengeLogs(uid)
        dao.clearNotes(uid)
        dao.clearRoutines(uid)
        dao.clearRoutineLogs(uid)
        dao.clearGoals(uid)
        dao.clearJournalEntries(uid)
        dao.clearFocusSessions(uid)
        dao.clearWaterLogs(uid)
        dao.clearStepLogs(uid)
        dao.clearReminders(uid)
    }

    suspend fun deleteAllUserData(uid: String): Result<Unit> = try {
        requireUserId(uid)
        clearLocalUserData(uid)
        val subcols = listOf(
            "userPreferences", "tasks", "habits", "habitLogs", "challenges", "challengeLogs",
            "notes", "routines", "routineLogs", "goals", "journalEntries",
            "focusSessions", "waterLogs", "stepLogs", "reminders"
        )
        for (col in subcols) {
            val docs = userDoc(uid).collection(col).get().await().documents
            docs.forEach { it.reference.delete().await() }
        }
        userDoc(uid).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e("LifeOsRepo", "Failed to clear cloud data", e)
        handleFirestoreError(e, OperationType.DELETE, "users/$uid")
        Result.failure(e)
    }
}
