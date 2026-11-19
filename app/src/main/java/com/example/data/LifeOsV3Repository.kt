package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.R
import com.example.domain.TechLearningCatalog
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class LifeOsV3Repository(
    private val context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
    ),
    private val v3Dao: LifeOsV3Dao = LifeOsDatabase.getInstance(context.applicationContext).v3Dao(),
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeProfile(uid: String, fallbackName: String): Flow<UserProfileEntity> =
        v3Dao.observeUserProfile(uid).map { entity ->
            entity ?: UserProfileEntity(userId = uid, fullName = fallbackName.ifBlank { "Builder" })
        }

    fun observeTransactions(uid: String): Flow<List<FinanceTransactionEntity>> = v3Dao.observeTransactions(uid)
    fun observeSavingsGoals(uid: String): Flow<List<SavingsGoalEntity>> = v3Dao.observeSavingsGoals(uid)
    fun observeNutritionEntries(uid: String): Flow<List<NutritionEntryEntity>> = v3Dao.observeNutritionEntries(uid)
    fun observeLearningTopics(uid: String): Flow<List<LearningTopicEntity>> = v3Dao.observeLearningTopics(uid)
    fun observeClassSchedules(uid: String): Flow<List<ClassScheduleEntity>> = v3Dao.observeClassSchedules(uid)
    fun observeJobApplications(uid: String): Flow<List<JobApplicationEntity>> = v3Dao.observeJobApplications(uid)
    fun observeSavedMedia(uid: String): Flow<List<SavedMediaEntity>> = v3Dao.observeSavedMedia(uid)
    fun observeMemories(uid: String): Flow<List<MemoryEntity>> = v3Dao.observeMemories(uid)
    fun observeNotificationPrefs(uid: String): Flow<NotificationPreferencesEntity> =
        v3Dao.observeNotificationPrefs(uid).map { it ?: NotificationPreferencesEntity(userId = uid) }
    fun observeNotificationLogs(uid: String): Flow<List<NotificationLogEntity>> = v3Dao.observeNotificationLogs(uid)

    suspend fun ensureSeededForUser(uid: String, displayName: String) {
        if (v3Dao.getUserProfileOnce(uid) == null) {
            val avatarFile = avatarFileFor(uid)
            v3Dao.upsertUserProfile(
                UserProfileEntity(
                    userId = uid,
                    fullName = displayName.ifBlank { "Builder" },
                    avatarLocalPath = if (avatarFile.exists()) avatarFile.absolutePath else ""
                )
            )
        }
        // Seed the 20-category tech learning curriculum once if empty (preserves any existing user progress)
        val existingTopics = v3Dao.observeLearningTopics(uid)
        // Quick lightweight check via one-shot query isn't on DAO, so check via Flow first() safely
        var hasTopics = false
        try {
            existingTopics.collect { list ->
                hasTopics = list.isNotEmpty()
                throw CancellationException("done_check")
            }
        } catch (_: CancellationException) {
            // Expected short-circuit
        }
        if (!hasTopics) {
            v3Dao.upsertLearningTopics(TechLearningCatalog.seedForUser(uid))
        }
    }

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

    // --- PROFILE & AVATAR ---
    suspend fun saveProfile(uid: String, profile: UserProfileEntity, email: String?): Result<Unit> {
        val clean = profile.copy(
            userId = uid,
            fullName = profile.fullName.trim().ifBlank { "Builder" }.take(120),
            username = profile.username.trim().take(50),
            bio = profile.bio.trim().take(240),
            initialBalance = profile.initialBalance.coerceIn(-10_000_000.0, 100_000_000.0),
            monthlyBudget = profile.monthlyBudget.coerceIn(0.0, 100_000_000.0),
            dailySpendLimit = profile.dailySpendLimit.coerceIn(0.0, 10_000_000.0),
            proteinTargetG = profile.proteinTargetG.coerceIn(10, 500),
            calorieTargetKcal = profile.calorieTargetKcal.coerceIn(800, 10000),
            streakFreezesAvailable = profile.streakFreezesAvailable.coerceIn(0, 10),
            updatedAtMs = System.currentTimeMillis()
        )
        v3Dao.upsertUserProfile(clean)
        val fields = mapOf<String, Any?>(
            "displayName" to clean.fullName,
            "email" to email?.take(200),
            "timezone" to "Asia/Kolkata",
            "username" to clean.username.ifBlank { null },
            "bio" to clean.bio.ifBlank { null },
            "initialBalance" to clean.initialBalance,
            "monthlyBudget" to clean.monthlyBudget,
            "dailySpendLimit" to clean.dailySpendLimit,
            "proteinTargetG" to clean.proteinTargetG,
            "calorieTargetKcal" to clean.calorieTargetKcal,
            "streakFreezesAvailable" to clean.streakFreezesAvailable
        )
        return upsertFirestoreDoc(
            docPath = "users/$uid",
            createFields = fields + ("userId" to uid),
            updateFields = fields
        )
    }

    fun avatarFileFor(uid: String): File {
        val dir = File(context.applicationContext.filesDir, "avatars").apply { mkdirs() }
        return File(dir, "avatar_${uid.take(40)}.jpg")
    }

    suspend fun saveAvatarFromUri(uid: String, sourceUri: Uri, zoomFactor: Float = 1f): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.applicationContext.contentResolver
                val rawBitmap = resolver.openInputStream(sourceUri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                } ?: return@withContext Result.failure(IllegalArgumentException("Unable to read selected image."))

                // Center square crop with optional zoom factor
                val side = minOf(rawBitmap.width, rawBitmap.height)
                val zoomedSide = (side / zoomFactor.coerceIn(1f, 2.5f)).toInt().coerceAtLeast(64)
                val x = ((rawBitmap.width - zoomedSide) / 2).coerceAtLeast(0)
                val y = ((rawBitmap.height - zoomedSide) / 2).coerceAtLeast(0)
                val cropped = Bitmap.createBitmap(rawBitmap, x, y, zoomedSide, zoomedSide)
                val scaled = Bitmap.createScaledBitmap(cropped, 512, 512, true)

                val outFile = avatarFileFor(uid)
                FileOutputStream(outFile).use { fos ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 88, fos)
                }
                if (cropped != rawBitmap) cropped.recycle()
                if (scaled != cropped) scaled.recycle()
                rawBitmap.recycle()

                val current = v3Dao.getUserProfileOnce(uid) ?: UserProfileEntity(userId = uid)
                v3Dao.upsertUserProfile(
                    current.copy(
                        avatarLocalPath = outFile.absolutePath,
                        updatedAtMs = System.currentTimeMillis()
                    )
                )
                Result.success(outFile.absolutePath)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun removeAvatar(uid: String) = withContext(Dispatchers.IO) {
        val outFile = avatarFileFor(uid)
        if (outFile.exists()) outFile.delete()
        val current = v3Dao.getUserProfileOnce(uid) ?: UserProfileEntity(userId = uid)
        v3Dao.upsertUserProfile(current.copy(avatarLocalPath = "", updatedAtMs = System.currentTimeMillis()))
    }

    // --- FINANCE TRANSACTIONS ---
    suspend fun saveTransaction(uid: String, tx: FinanceTransactionEntity): Result<Unit> {
        val clean = tx.copy(
            userId = uid,
            type = if (tx.type == TransactionType.INCOME.name) TransactionType.INCOME.name else TransactionType.EXPENSE.name,
            amount = tx.amount.coerceIn(0.01, 100_000_000.0),
            category = tx.category.trim().ifBlank { "General" }.take(60),
            note = tx.note.trim().take(240),
            dateIso = tx.dateIso.take(10),
            locationTag = tx.locationTag.trim().take(120),
            updatedAtMs = System.currentTimeMillis()
        )
        v3Dao.upsertTransaction(clean)
        val fields = mapOf<String, Any?>(
            "type" to clean.type,
            "amount" to clean.amount,
            "category" to clean.category,
            "note" to clean.note.ifBlank { null },
            "dateIso" to clean.dateIso,
            "locationTag" to clean.locationTag.ifBlank { null }
        )
        return upsertFirestoreDoc(
            docPath = "users/$uid/transactions/${clean.id}",
            createFields = fields + ("userId" to uid),
            updateFields = fields
        )
    }

    suspend fun deleteTransaction(uid: String, id: String): Result<Unit> {
        v3Dao.deleteTransaction(uid, id)
        return deleteFirestoreDoc("users/$uid/transactions/$id")
    }

    // --- SAVINGS GOALS ---
    suspend fun saveSavingsGoal(uid: String, goal: SavingsGoalEntity): Result<Unit> {
        val clean = goal.copy(
            userId = uid,
            title = goal.title.trim().take(120),
            targetAmount = goal.targetAmount.coerceIn(1.0, 100_000_000.0),
            savedAmount = goal.savedAmount.coerceIn(0.0, 100_000_000.0),
            targetDateIso = goal.targetDateIso.take(10),
            updatedAtMs = System.currentTimeMillis()
        )
        v3Dao.upsertSavingsGoal(clean)
        val fields = mapOf<String, Any?>(
            "title" to clean.title,
            "targetAmount" to clean.targetAmount,
            "savedAmount" to clean.savedAmount,
            "targetDateIso" to clean.targetDateIso
        )
        return upsertFirestoreDoc(
            docPath = "users/$uid/savingsGoals/${clean.id}",
            createFields = fields + ("userId" to uid),
            updateFields = fields
        )
    }

    suspend fun deleteSavingsGoal(uid: String, id: String): Result<Unit> {
        v3Dao.deleteSavingsGoal(uid, id)
        return deleteFirestoreDoc("users/$uid/savingsGoals/$id")
    }

    // --- NUTRITION ---
    suspend fun saveNutritionEntry(uid: String, entry: NutritionEntryEntity): Result<Unit> {
        val clean = entry.copy(
            userId = uid,
            dateIso = entry.dateIso.take(10),
            mealType = MealType.fromNameOrDefault(entry.mealType).name,
            foodName = entry.foodName.trim().take(140),
            quantityLabel = entry.quantityLabel.trim().ifBlank { "1 serving" }.take(60),
            proteinG = entry.proteinG.coerceIn(0.0, 500.0),
            calories = entry.calories.coerceIn(0, 10000),
            carbsG = entry.carbsG.coerceIn(0.0, 1000.0),
            fatG = entry.fatG.coerceIn(0.0, 500.0),
            updatedAtMs = System.currentTimeMillis()
        )
        v3Dao.upsertNutritionEntry(clean)
        val fields = mapOf<String, Any?>(
            "dateIso" to clean.dateIso,
            "mealType" to clean.mealType,
            "foodName" to clean.foodName,
            "quantityLabel" to clean.quantityLabel,
            "proteinG" to clean.proteinG,
            "calories" to clean.calories,
            "carbsG" to clean.carbsG,
            "fatG" to clean.fatG,
            "isEstimated" to clean.isEstimated
        )
        return upsertFirestoreDoc(
            docPath = "users/$uid/nutritionEntries/${clean.id}",
            createFields = fields + ("userId" to uid),
            updateFields = fields
        )
    }

    suspend fun deleteNutritionEntry(uid: String, id: String): Result<Unit> {
        v3Dao.deleteNutritionEntry(uid, id)
        return deleteFirestoreDoc("users/$uid/nutritionEntries/$id")
    }

    // --- LEARNING TOPICS ---
    suspend fun saveLearningTopic(uid: String, topic: LearningTopicEntity): Result<Unit> {
        val clean = topic.copy(
            userId = uid,
            category = topic.category.trim().take(80),
            level = LearningLevel.fromNameOrDefault(topic.level).name,
            title = topic.title.trim().take(160),
            studyMinutes = topic.studyMinutes.coerceIn(0, 100000),
            notes = topic.notes.trim().take(4000),
            updatedAtMs = System.currentTimeMillis()
        )
        v3Dao.upsertLearningTopic(clean)
        val fields = mapOf<String, Any?>(
            "category" to clean.category,
            "level" to clean.level,
            "title" to clean.title,
            "completed" to clean.completed,
            "saved" to clean.saved,
            "studyMinutes" to clean.studyMinutes,
            "notes" to clean.notes.ifBlank { null }
        )
        return upsertFirestoreDoc(
            docPath = "users/$uid/learningTopics/${clean.id}",
            createFields = fields + ("userId" to uid),
            updateFields = fields
        )
    }

    // --- CLASS SCHEDULES ---
    suspend fun saveClassSchedule(uid: String, schedule: ClassScheduleEntity): Result<Unit> {
        val clean = schedule.copy(
            userId = uid,
            className = schedule.className.trim().take(140),
            subject = schedule.subject.trim().take(100),
            instructor = schedule.instructor.trim().take(100),
            dayOfWeek = schedule.dayOfWeek.coerceIn(1, 7),
            startTime = schedule.startTime.trim().take(16),
            endTime = schedule.endTime.trim().take(16),
            locationOrLink = schedule.locationOrLink.trim().take(300),
            updatedAtMs = System.currentTimeMillis()
        )
        v3Dao.upsertClassSchedule(clean)
        val fields = mapOf<String, Any?>(
            "className" to clean.className,
            "subject" to clean.subject.ifBlank { null },
            "instructor" to clean.instructor.ifBlank { null },
            "dayOfWeek" to clean.dayOfWeek,
            "startTime" to clean.startTime,
            "endTime" to clean.endTime,
            "locationOrLink" to clean.locationOrLink.ifBlank { null },
            "reminderEnabled" to clean.reminderEnabled
        )
        return upsertFirestoreDoc(
            docPath = "users/$uid/classSchedules/${clean.id}",
            createFields = fields + ("userId" to uid),
            updateFields = fields
        )
    }

    suspend fun deleteClassSchedule(uid: String, id: String): Result<Unit> {
        v3Dao.deleteClassSchedule(uid, id)
        return deleteFirestoreDoc("users/$uid/classSchedules/$id")
    }

    // --- JOB APPLICATIONS & INTERVIEWS ---
    suspend fun saveJobApplication(uid: String, app: JobApplicationEntity): Result<Unit> {
        val clean = app.copy(
            userId = uid,
            company = app.company.trim().take(120),
            role = app.role.trim().take(120),
            jobType = app.jobType.trim().ifBlank { "Full-time" }.take(40),
            appliedDateIso = app.appliedDateIso.take(10),
            applicationLink = app.applicationLink.trim().take(400),
            status = JobApplicationStatus.fromNameOrDefault(app.status).name,
            interviewDateIso = app.interviewDateIso.trim().take(10),
            interviewTime = app.interviewTime.trim().take(20),
            interviewType = app.interviewType.trim().take(60),
            meetingLink = app.meetingLink.trim().take(400),
            location = app.location.trim().take(160),
            contactInfo = app.contactInfo.trim().take(160),
            prepNotes = app.prepNotes.trim().take(3000),
            prepChecklistCsv = app.prepChecklistCsv.take(2000),
            completedChecklistCsv = app.completedChecklistCsv.take(2000),
            updatedAtMs = System.currentTimeMillis()
        )
        v3Dao.upsertJobApplication(clean)
        val fields = mapOf<String, Any?>(
            "company" to clean.company,
            "role" to clean.role,
            "jobType" to clean.jobType,
            "appliedDateIso" to clean.appliedDateIso,
            "applicationLink" to clean.applicationLink.ifBlank { null },
            "status" to clean.status,
            "interviewDateIso" to clean.interviewDateIso.ifBlank { null },
            "interviewTime" to clean.interviewTime.ifBlank { null },
            "interviewType" to clean.interviewType.ifBlank { null },
            "meetingLink" to clean.meetingLink.ifBlank { null },
            "location" to clean.location.ifBlank { null },
            "contactInfo" to clean.contactInfo.ifBlank { null },
            "prepNotes" to clean.prepNotes.ifBlank { null },
            "prepChecklistCsv" to clean.prepChecklistCsv.ifBlank { null },
            "completedChecklistCsv" to clean.completedChecklistCsv.ifBlank { null }
        )
        return upsertFirestoreDoc(
            docPath = "users/$uid/jobApplications/${clean.id}",
            createFields = fields + ("userId" to uid),
            updateFields = fields
        )
    }

    suspend fun deleteJobApplication(uid: String, id: String): Result<Unit> {
        v3Dao.deleteJobApplication(uid, id)
        return deleteFirestoreDoc("users/$uid/jobApplications/$id")
    }

    // --- SAVED MEDIA (Bookmarks, Watch Later, Favorite Quotes) ---
    suspend fun saveMediaItem(uid: String, item: SavedMediaEntity): Result<Unit> {
        val safeKind = when (item.kind.uppercase()) {
            "NEWS", "VIDEO", "QUOTE" -> item.kind.uppercase()
            else -> "NEWS"
        }
        val clean = item.copy(
            userId = uid,
            kind = safeKind,
            title = item.title.trim().take(240),
            subtitleOrAuthor = item.subtitleOrAuthor.trim().take(160),
            url = item.url.trim().take(500),
            category = item.category.trim().take(60)
        )
        v3Dao.upsertSavedMedia(clean)
        val fields = mapOf<String, Any?>(
            "kind" to clean.kind,
            "title" to clean.title,
            "subtitleOrAuthor" to clean.subtitleOrAuthor.ifBlank { null },
            "url" to clean.url.ifBlank { null },
            "category" to clean.category.ifBlank { null },
            "watchedOrRead" to clean.watchedOrRead
        )
        return upsertFirestoreDoc(
            docPath = "users/$uid/savedMedia/${clean.id}",
            createFields = fields + ("userId" to uid),
            updateFields = fields
        )
    }

    suspend fun deleteMediaItem(uid: String, id: String): Result<Unit> {
        v3Dao.deleteSavedMedia(uid, id)
        return deleteFirestoreDoc("users/$uid/savedMedia/$id")
    }

    // --- PRIVATE MEMORIES (Strictly Separate From Notes) ---
    suspend fun saveMemoryPhotoFromUri(uid: String, memoryId: String, sourceUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val resolver = context.applicationContext.contentResolver
                val rawBitmap = resolver.openInputStream(sourceUri)?.use { BitmapFactory.decodeStream(it) }
                    ?: return@withContext Result.failure(IllegalArgumentException("Unable to open selected photo."))
                val maxDim = 1280
                val scale = minOf(1f, maxDim.toFloat() / maxOf(rawBitmap.width, rawBitmap.height).coerceAtLeast(1))
                val outBitmap = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        rawBitmap,
                        (rawBitmap.width * scale).toInt().coerceAtLeast(1),
                        (rawBitmap.height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    rawBitmap
                }
                val dir = File(context.applicationContext.filesDir, "memories_${uid.take(24)}").apply { mkdirs() }
                val outFile = File(dir, "mem_${memoryId.take(36)}.jpg")
                FileOutputStream(outFile).use { fos ->
                    outBitmap.compress(Bitmap.CompressFormat.JPEG, 86, fos)
                }
                if (outBitmap != rawBitmap) outBitmap.recycle()
                rawBitmap.recycle()
                Result.success(outFile.absolutePath)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun saveMemory(uid: String, memory: MemoryEntity): Result<Unit> {
        val clean = memory.copy(
            userId = uid,
            title = memory.title.trim().take(160),
            description = memory.description.trim().take(4000),
            memoryDateIso = memory.memoryDateIso.take(10),
            memoryTimeText = memory.memoryTimeText.trim().take(20),
            category = memory.category.trim().ifBlank { "Personal" }.take(60),
            tagsCsv = memory.tagsCsv.trim().take(300),
            updatedAtMs = System.currentTimeMillis()
        )
        v3Dao.upsertMemory(clean)
        val fields = mapOf<String, Any?>(
            "title" to clean.title,
            "description" to clean.description.ifBlank { null },
            "memoryDateIso" to clean.memoryDateIso,
            "memoryTimeText" to clean.memoryTimeText.ifBlank { null },
            "category" to clean.category,
            "tagsCsv" to clean.tagsCsv.ifBlank { null },
            "favorite" to clean.favorite
        )
        return upsertFirestoreDoc(
            docPath = "users/$uid/memories/${clean.id}",
            createFields = fields + ("userId" to uid),
            updateFields = fields
        )
    }

    suspend fun deleteMemory(uid: String, id: String, photoLocalPath: String = ""): Result<Unit> {
        if (photoLocalPath.isNotBlank()) {
            runCatching { File(photoLocalPath).takeIf { it.exists() }?.delete() }
        }
        v3Dao.deleteMemory(uid, id)
        return deleteFirestoreDoc("users/$uid/memories/$id")
    }

    // --- SMART NOTIFICATION PREFERENCES & HISTORY ---
    suspend fun getNotificationPrefsOnce(uid: String): NotificationPreferencesEntity {
        return v3Dao.getNotificationPrefsOnce(uid) ?: NotificationPreferencesEntity(userId = uid)
    }

    suspend fun saveNotificationPrefs(uid: String, prefs: NotificationPreferencesEntity): Result<Unit> {
        val clean = prefs.copy(
            userId = uid,
            waterIntervalMinutes = prefs.waterIntervalMinutes.coerceIn(30, 240),
            newsIntervalMinutes = prefs.newsIntervalMinutes.coerceIn(60, 360),
            videoIntervalMinutes = prefs.videoIntervalMinutes.coerceIn(60, 360),
            dailySummaryHour = prefs.dailySummaryHour.coerceIn(0, 23),
            dailySummaryMinute = prefs.dailySummaryMinute.coerceIn(0, 59),
            quietStartHour = prefs.quietStartHour.coerceIn(0, 23),
            quietEndHour = prefs.quietEndHour.coerceIn(0, 23),
            updatedAtMs = System.currentTimeMillis()
        )
        v3Dao.upsertNotificationPrefs(clean)
        val fields = mapOf<String, Any?>(
            "pauseAll" to clean.pauseAll,
            "waterEnabled" to clean.waterEnabled,
            "stepsEnabled" to clean.stepsEnabled,
            "goalsEnabled" to clean.goalsEnabled,
            "tasksEnabled" to clean.tasksEnabled,
            "habitsEnabled" to clean.habitsEnabled,
            "techNewsEnabled" to clean.techNewsEnabled,
            "youtubeVideosEnabled" to clean.youtubeVideosEnabled,
            "learningEnabled" to clean.learningEnabled,
            "classesEnabled" to clean.classesEnabled,
            "interviewsEnabled" to clean.interviewsEnabled,
            "financeEnabled" to clean.financeEnabled,
            "motivationEnabled" to clean.motivationEnabled,
            "dailySummaryEnabled" to clean.dailySummaryEnabled,
            "waterIntervalMinutes" to clean.waterIntervalMinutes,
            "dailySummaryHour" to clean.dailySummaryHour,
            "dailySummaryMinute" to clean.dailySummaryMinute,
            "quietHoursEnabled" to clean.quietHoursEnabled,
            "quietStartHour" to clean.quietStartHour,
            "quietEndHour" to clean.quietEndHour,
            "soundEnabled" to clean.soundEnabled,
            "vibrationEnabled" to clean.vibrationEnabled
        )
        return upsertFirestoreDoc(
            docPath = "users/$uid/notificationPrefs/default",
            createFields = fields + ("userId" to uid),
            updateFields = fields
        )
    }

    suspend fun clearNotificationLogs(uid: String) {
        v3Dao.clearNotificationLogs(uid)
    }

    suspend fun deleteAllV3Data(uid: String) {
        v3Dao.clearAllV3ForUser(uid)
        val file = avatarFileFor(uid)
        if (file.exists()) file.delete()
        val collections = listOf(
            "transactions", "savingsGoals", "nutritionEntries",
            "learningTopics", "classSchedules", "jobApplications",
            "savedMedia", "memories", "notificationPrefs"
        )
        for (col in collections) {
            runCatching {
                val snap = userDoc(uid).collection(col).get().await()
                snap.documents.forEach { it.reference.delete().await() }
            }
        }
    }

    // --- REALTIME FIRESTORE SYNC FOR V3 COLLECTIONS ---
    fun startV3CloudSync(uid: String, scope: CoroutineScope, onSyncError: (String) -> Unit): List<Job> {
        if (uid.isBlank() || auth.currentUser?.uid != uid) return emptyList()
        val jobs = mutableListOf<Job>()

        // Profile doc listener
        jobs += scope.launch {
            userDoc(uid).snapshots()
                .catch { err ->
                    val fsErr = generateSequence(err) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && err is CancellationException) throw err
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: err, OperationType.GET, "users/$uid")
                    }
                }
                .collect { doc ->
                    val data = doc.data ?: return@collect
                    val existing = v3Dao.getUserProfileOnce(uid)
                    v3Dao.upsertUserProfile(
                        UserProfileEntity(
                            userId = uid,
                            fullName = (data["displayName"] as? String) ?: existing?.fullName ?: "Builder",
                            username = (data["username"] as? String) ?: existing?.username.orEmpty(),
                            bio = (data["bio"] as? String) ?: existing?.bio.orEmpty(),
                            avatarLocalPath = existing?.avatarLocalPath
                                ?: avatarFileFor(uid).takeIf { it.exists() }?.absolutePath.orEmpty(),
                            initialBalance = (data["initialBalance"] as? Number)?.toDouble() ?: existing?.initialBalance ?: 10000.0,
                            monthlyBudget = (data["monthlyBudget"] as? Number)?.toDouble() ?: existing?.monthlyBudget ?: 15000.0,
                            dailySpendLimit = (data["dailySpendLimit"] as? Number)?.toDouble() ?: existing?.dailySpendLimit ?: 1000.0,
                            proteinTargetG = (data["proteinTargetG"] as? Number)?.toInt() ?: existing?.proteinTargetG ?: 90,
                            calorieTargetKcal = (data["calorieTargetKcal"] as? Number)?.toInt() ?: existing?.calorieTargetKcal ?: 2200,
                            streakFreezesAvailable = (data["streakFreezesAvailable"] as? Number)?.toInt() ?: existing?.streakFreezesAvailable ?: 1,
                            waterReminderEnabled = existing?.waterReminderEnabled ?: false,
                            waterReminderIntervalMin = existing?.waterReminderIntervalMin ?: 120
                        )
                    )
                }
        }

        fun <T> syncSubcol(subcol: String, mapper: (DocumentSnapshot) -> T?, onItems: suspend (List<T>) -> Unit) {
            val path = "users/$uid/$subcol"
            jobs += scope.launch {
                userDoc(uid).collection(subcol).snapshots()
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
                        val items = snap.documents.mapNotNull { d -> runCatching { mapper(d) }.getOrNull() }
                        if (items.isNotEmpty()) onItems(items)
                    }
            }
        }

        syncSubcol("transactions", { d ->
            val data = d.data ?: return@syncSubcol null
            FinanceTransactionEntity(
                id = d.id,
                userId = uid,
                type = data["type"] as? String ?: TransactionType.EXPENSE.name,
                amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                category = data["category"] as? String ?: "Food",
                note = data["note"] as? String ?: "",
                dateIso = data["dateIso"] as? String ?: DateUtils.todayIso(),
                locationTag = data["locationTag"] as? String ?: ""
            )
        }, { v3Dao.upsertTransactions(it) })

        syncSubcol("savingsGoals", { d ->
            val data = d.data ?: return@syncSubcol null
            SavingsGoalEntity(
                id = d.id,
                userId = uid,
                title = data["title"] as? String ?: "",
                targetAmount = (data["targetAmount"] as? Number)?.toDouble() ?: 1000.0,
                savedAmount = (data["savedAmount"] as? Number)?.toDouble() ?: 0.0,
                targetDateIso = data["targetDateIso"] as? String ?: DateUtils.todayIso()
            )
        }, { v3Dao.upsertSavingsGoals(it) })

        syncSubcol("nutritionEntries", { d ->
            val data = d.data ?: return@syncSubcol null
            NutritionEntryEntity(
                id = d.id,
                userId = uid,
                dateIso = data["dateIso"] as? String ?: DateUtils.todayIso(),
                mealType = data["mealType"] as? String ?: MealType.BREAKFAST.name,
                foodName = data["foodName"] as? String ?: "",
                quantityLabel = data["quantityLabel"] as? String ?: "1 serving",
                proteinG = (data["proteinG"] as? Number)?.toDouble() ?: 0.0,
                calories = (data["calories"] as? Number)?.toInt() ?: 0,
                carbsG = (data["carbsG"] as? Number)?.toDouble() ?: 0.0,
                fatG = (data["fatG"] as? Number)?.toDouble() ?: 0.0,
                isEstimated = data["isEstimated"] as? Boolean ?: true
            )
        }, { v3Dao.upsertNutritionEntries(it) })

        syncSubcol("learningTopics", { d ->
            val data = d.data ?: return@syncSubcol null
            LearningTopicEntity(
                id = d.id,
                userId = uid,
                category = data["category"] as? String ?: "Java",
                level = data["level"] as? String ?: LearningLevel.BEGINNER.name,
                title = data["title"] as? String ?: "",
                completed = data["completed"] as? Boolean ?: false,
                saved = data["saved"] as? Boolean ?: false,
                studyMinutes = (data["studyMinutes"] as? Number)?.toInt() ?: 0,
                notes = data["notes"] as? String ?: ""
            )
        }, { v3Dao.upsertLearningTopics(it) })

        syncSubcol("classSchedules", { d ->
            val data = d.data ?: return@syncSubcol null
            ClassScheduleEntity(
                id = d.id,
                userId = uid,
                className = data["className"] as? String ?: "",
                subject = data["subject"] as? String ?: "",
                instructor = data["instructor"] as? String ?: "",
                dayOfWeek = (data["dayOfWeek"] as? Number)?.toInt() ?: 1,
                startTime = data["startTime"] as? String ?: "09:00",
                endTime = data["endTime"] as? String ?: "10:00",
                locationOrLink = data["locationOrLink"] as? String ?: "",
                reminderEnabled = data["reminderEnabled"] as? Boolean ?: true
            )
        }, { v3Dao.upsertClassSchedules(it) })

        syncSubcol("jobApplications", { d ->
            val data = d.data ?: return@syncSubcol null
            JobApplicationEntity(
                id = d.id,
                userId = uid,
                company = data["company"] as? String ?: "",
                role = data["role"] as? String ?: "",
                jobType = data["jobType"] as? String ?: "Full-time",
                appliedDateIso = data["appliedDateIso"] as? String ?: DateUtils.todayIso(),
                applicationLink = data["applicationLink"] as? String ?: "",
                status = data["status"] as? String ?: JobApplicationStatus.APPLIED.name,
                interviewDateIso = data["interviewDateIso"] as? String ?: "",
                interviewTime = data["interviewTime"] as? String ?: "",
                interviewType = data["interviewType"] as? String ?: "Technical",
                meetingLink = data["meetingLink"] as? String ?: "",
                location = data["location"] as? String ?: "",
                contactInfo = data["contactInfo"] as? String ?: "",
                prepNotes = data["prepNotes"] as? String ?: "",
                prepChecklistCsv = data["prepChecklistCsv"] as? String ?: "",
                completedChecklistCsv = data["completedChecklistCsv"] as? String ?: ""
            )
        }, { v3Dao.upsertJobApplications(it) })

        syncSubcol("savedMedia", { d ->
            val data = d.data ?: return@syncSubcol null
            SavedMediaEntity(
                id = d.id,
                userId = uid,
                kind = data["kind"] as? String ?: "NEWS",
                title = data["title"] as? String ?: "",
                subtitleOrAuthor = data["subtitleOrAuthor"] as? String ?: "",
                url = data["url"] as? String ?: "",
                category = data["category"] as? String ?: "",
                watchedOrRead = data["watchedOrRead"] as? Boolean ?: false
            )
        }, { v3Dao.upsertSavedMediaList(it) })

        syncSubcol("memories", { d ->
            val data = d.data ?: return@syncSubcol null
            val photoFile = File(
                File(context.applicationContext.filesDir, "memories_${uid.take(24)}"),
                "mem_${d.id.take(36)}.jpg"
            )
            MemoryEntity(
                id = d.id,
                userId = uid,
                title = data["title"] as? String ?: "",
                description = data["description"] as? String ?: "",
                memoryDateIso = data["memoryDateIso"] as? String ?: DateUtils.todayIso(),
                memoryTimeText = data["memoryTimeText"] as? String ?: "",
                category = data["category"] as? String ?: "Personal",
                tagsCsv = data["tagsCsv"] as? String ?: "",
                photoLocalPath = if (photoFile.exists()) photoFile.absolutePath else "",
                favorite = data["favorite"] as? Boolean ?: false
            )
        }, { v3Dao.upsertMemories(it) })

        return jobs
    }
}
