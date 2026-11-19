package com.example.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.worker.LifeOsNotificationCenter
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Coordinates automatic step tracking across:
 * 1. Android hardware sensors (TYPE_STEP_COUNTER + TYPE_STEP_DETECTOR via StepCounterManager)
 * 2. Jetpack Health Connect aggregated step & distance records (HealthConnectStepManager)
 * 3. Offline-first Room & Cloud Firestore persistence (LifeOsRepository)
 * 4. Background periodic step synchronization & milestone notifications (WorkManager)
 */
class StepTrackingRepository(
    context: Context,
    private val lifeOsRepository: LifeOsRepository,
    private val onSensorDailySteps: (dateIso: String, steps: Int) -> Unit
) {
    private val appContext = context.applicationContext
    val sensorManager = StepCounterManager(appContext, onSensorDailySteps)
    val healthConnectManager = HealthConnectStepManager(appContext)

    val liveSensorStatus: StateFlow<LiveStepSensorStatus> = sensorManager.status

    fun observeStepLogs(uid: String): Flow<List<StepLogEntity>> = lifeOsRepository.observeStepLogs(uid)

    fun startLiveTracking() {
        sensorManager.refreshAndStartIfPermitted()
    }

    fun stopLiveTracking() {
        sensorManager.stop()
    }

    suspend fun syncHealthConnectAndPersist(uid: String): HealthConnectSnapshot {
        val snapshot = healthConnectManager.readSnapshot(daysToRead = 30)
        sensorManager.updateHealthConnectOverlay(
            available = snapshot.availability == HealthConnectAvailability.AVAILABLE,
            granted = snapshot.permissionsGranted,
            hcTodaySteps = snapshot.todaySteps,
            hcDistanceKm = snapshot.todayDistanceKm
        )

        if (snapshot.permissionsGranted && uid.isNotBlank()) {
            val existingByDate = lifeOsRepository.observeStepLogs(uid).first().associateBy { it.dateIso }
            snapshot.dailyStepsByIso.forEach { (dateIso, hcSteps) ->
                val currentStored = existingByDate[dateIso]?.steps ?: 0
                if (hcSteps > currentStored) {
                    lifeOsRepository.saveStepLog(uid, dateIso, hcSteps, "HEALTH_CONNECT")
                }
            }
        }
        return snapshot
    }

    suspend fun persistSensorSteps(uid: String, dateIso: String, sensorSteps: Int, dailyGoal: Int) {
        if (uid.isBlank() || sensorSteps <= 0) return
        val existing = lifeOsRepository.observeStepLogs(uid).first()
            .firstOrNull { it.dateIso == dateIso }?.steps ?: 0
        if (sensorSteps > existing) {
            lifeOsRepository.saveStepLog(uid, dateIso, sensorSteps, "STEP_SENSOR")
            checkAndNotifyStepMilestones(appContext, uid, dateIso, sensorSteps, dailyGoal)
        }
    }

    fun scheduleBackgroundStepSync(uid: String) {
        if (uid.isBlank()) return
        val input = Data.Builder().putString(LifeOsNotificationCenter.KEY_UID, uid).build()
        val req = PeriodicWorkRequestBuilder<StepBackgroundSyncWorker>(15, TimeUnit.MINUTES)
            .setInputData(input)
            .build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            WORK_STEP_SYNC,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    companion object {
        private const val WORK_STEP_SYNC = "lifeos_work_automatic_step_sync"

        /**
         * Sends optional automatic step progress & completion notifications
         * strictly derived from real calculated step counts with daily dedupe keys.
         */
        suspend fun checkAndNotifyStepMilestones(
            context: Context,
            uid: String,
            dateIso: String,
            actualSteps: Int,
            dailyGoal: Int
        ) {
            if (actualSteps <= 0 || dailyGoal <= 0) return
            val metrics = StepCalculationEngine.computeMetrics(actualSteps, dailyGoal)
            if (metrics.goalReached) {
                LifeOsNotificationCenter.postSmartNotification(
                    context = context,
                    uid = uid,
                    category = NotificationCategory.STEPS,
                    title = "Daily Step Goal Achieved!",
                    message = "You reached ${String.format("%,d", actualSteps)} / ${String.format("%,d", dailyGoal)} steps today.",
                    deepLinkRoute = "steps",
                    dedupeKey = "steps_goal_reached_$dateIso"
                )
            } else if (metrics.progressPercent >= 50f) {
                LifeOsNotificationCenter.postSmartNotification(
                    context = context,
                    uid = uid,
                    category = NotificationCategory.STEPS,
                    title = "Halfway to Your Step Goal (${metrics.progressPercent}%)",
                    message = "${String.format("%,d", actualSteps)} steps logged automatically — ${String.format("%,d", metrics.remainingSteps)} steps to go.",
                    deepLinkRoute = "steps",
                    dedupeKey = "steps_halfway_$dateIso"
                )
            }
        }
    }
}

/**
 * Periodic WorkManager worker that syncs automatic Health Connect / persisted sensor step counts
 * into Room & Firestore and evaluates real step progress notifications.
 */
class StepBackgroundSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val uid = inputData.getString(LifeOsNotificationCenter.KEY_UID)
            ?: Firebase.auth.currentUser?.uid
            ?: return Result.success()

        val repo = LifeOsRepository(applicationContext)
        val hcManager = HealthConnectStepManager(applicationContext)
        val today = DateUtils.todayIso()
        val userPrefs = repo.preferences.observePreferences(uid).first()
        val dailyGoal = userPrefs.stepsTarget.coerceAtLeast(1000)

        // 1. Read persisted hardware sensor total for today (survives restarts)
        val sensorMgr = StepCounterManager(applicationContext) { _, _ -> }
        val sensorToday = sensorMgr.readPersistedTodaySteps(today)

        // 2. Read aggregated Health Connect records if authorized
        val hcSnapshot = hcManager.readSnapshot(daysToRead = 7)
        val existingByDate = repo.observeStepLogs(uid).first().associateBy { it.dateIso }

        if (hcSnapshot.permissionsGranted) {
            hcSnapshot.dailyStepsByIso.forEach { (iso, count) ->
                val stored = existingByDate[iso]?.steps ?: 0
                if (count > stored) {
                    repo.saveStepLog(uid, iso, count, "HEALTH_CONNECT")
                }
            }
        }

        val mergedToday = StepCalculationEngine.mergeAutomaticSources(
            sensorTodaySteps = sensorToday,
            healthConnectTodaySteps = hcSnapshot.todaySteps,
            persistedRoomSteps = existingByDate[today]?.steps ?: 0
        )

        if (mergedToday > (existingByDate[today]?.steps ?: 0)) {
            val source = if (hcSnapshot.todaySteps >= sensorToday && hcSnapshot.permissionsGranted) {
                "HEALTH_CONNECT"
            } else {
                "STEP_SENSOR"
            }
            repo.saveStepLog(uid, today, mergedToday, source)
        }

        if (mergedToday > 0) {
            StepTrackingRepository.checkAndNotifyStepMilestones(
                context = applicationContext,
                uid = uid,
                dateIso = today,
                actualSteps = mergedToday,
                dailyGoal = dailyGoal
            )
        }
        return Result.success()
    }
}
