package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DateUtils
import com.example.data.LiveStepSensorStatus
import com.example.data.StepCalculationEngine
import com.example.data.StepLogEntity
import com.example.data.StepTrackingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DailyStepRecord(
    val dateIso: String,
    val dayLabel: String,
    val steps: Int,
    val source: String
)

data class StepTrackingUiState(
    val selectedDateIso: String = DateUtils.todayIso(),
    val todaySteps: Int = 0,
    val dailyGoal: Int = 10_000,
    val progressFraction: Float = 0f,
    val progressPercent: Float = 0f,
    val remainingSteps: Int = 10_000,
    val goalReached: Boolean = false,
    val distanceKm: Double? = null,
    val distanceIsMeasured: Boolean = false,
    val sensorAvailable: Boolean = false,
    val activityPermissionGranted: Boolean = false,
    val sensorTrackingActive: Boolean = false,
    val healthConnectAvailable: Boolean = false,
    val healthConnectGranted: Boolean = false,
    val isTrackingUnavailable: Boolean = false,
    val isWalkingNow: Boolean = false,
    val activeSourceLabel: String = "Checking sensors...",
    val walkingActivityLabel: String = "Idle",
    val statusMessage: String = "Checking device step sensors...",
    val weeklyHistory: List<DailyStepRecord> = emptyList(),
    val monthlyHistory: List<DailyStepRecord> = emptyList(),
    val weeklyAverage: Int = 0,
    val monthlyTotal: Int = 0
)

/**
 * Pure mapper building StepTrackingUiState from real automatic sensor, Health Connect,
 * and persisted Room StepLogEntity records — zero fabricated or manual counts.
 */
object StepTrackingStateMapper {

    fun build(
        selectedDateIso: String,
        dailyGoal: Int,
        stepLogs: List<StepLogEntity>,
        sensorStatus: LiveStepSensorStatus
    ): StepTrackingUiState {
        val todayIso = DateUtils.todayIso()
        val storedForSelected = stepLogs.firstOrNull { it.dateIso == selectedDateIso }?.steps ?: 0
        val effectiveSteps = if (selectedDateIso == todayIso) {
            StepCalculationEngine.mergeAutomaticSources(
                sensorTodaySteps = sensorStatus.todaySensorSteps,
                healthConnectTodaySteps = sensorStatus.healthConnectTodaySteps,
                persistedRoomSteps = storedForSelected
            )
        } else {
            storedForSelected
        }

        val metrics = StepCalculationEngine.computeMetrics(effectiveSteps, dailyGoal)
        val byDate = stepLogs.associateBy { it.dateIso }

        val weekly = DateUtils.lastNDaysIso(7, selectedDateIso).map { iso ->
            val stored = byDate[iso]
            val steps = if (iso == todayIso) effectiveSteps else (stored?.steps ?: 0)
            DailyStepRecord(
                dateIso = iso,
                dayLabel = DateUtils.formatShortDay(iso),
                steps = steps,
                source = stored?.source ?: if (sensorStatus.healthConnectGranted) "HEALTH_CONNECT" else "STEP_SENSOR"
            )
        }

        val monthly = DateUtils.lastNDaysIso(30, selectedDateIso).map { iso ->
            val stored = byDate[iso]
            val steps = if (iso == todayIso) effectiveSteps else (stored?.steps ?: 0)
            DailyStepRecord(
                dateIso = iso,
                dayLabel = DateUtils.formatShortDay(iso),
                steps = steps,
                source = stored?.source ?: "STEP_SENSOR"
            )
        }

        val walkingLabel = when {
            sensorStatus.isWalkingNow -> "Walking now"
            sensorStatus.lastStepEpochMs > 0L -> {
                val minsAgo = ((System.currentTimeMillis() - sensorStatus.lastStepEpochMs) / 60_000L).coerceAtLeast(1)
                if (minsAgo <= 60) "Active ${minsAgo}m ago" else "Resting"
            }
            effectiveSteps > 0 -> "Active today"
            sensorStatus.isAutomaticTrackingUnavailable -> "Sensor unavailable"
            !sensorStatus.permissionGranted && !sensorStatus.healthConnectGranted -> "Permission required"
            else -> "Ready to detect walking"
        }

        val distanceKm = if (selectedDateIso == todayIso && sensorStatus.healthConnectDistanceKm != null && sensorStatus.healthConnectDistanceKm > 0.0) {
            sensorStatus.healthConnectDistanceKm
        } else if (effectiveSteps > 0) {
            StepCalculationEngine.estimateDistanceKm(effectiveSteps)
        } else {
            null
        }

        return StepTrackingUiState(
            selectedDateIso = selectedDateIso,
            todaySteps = metrics.todaySteps,
            dailyGoal = metrics.dailyGoal,
            progressFraction = metrics.progressFraction,
            progressPercent = metrics.progressPercent,
            remainingSteps = metrics.remainingSteps,
            goalReached = metrics.goalReached,
            distanceKm = distanceKm,
            distanceIsMeasured = selectedDateIso == todayIso && sensorStatus.distanceIsMeasured,
            sensorAvailable = sensorStatus.sensorAvailable,
            activityPermissionGranted = sensorStatus.permissionGranted,
            sensorTrackingActive = sensorStatus.trackingActive,
            healthConnectAvailable = sensorStatus.healthConnectAvailable,
            healthConnectGranted = sensorStatus.healthConnectGranted,
            isTrackingUnavailable = sensorStatus.isAutomaticTrackingUnavailable,
            isWalkingNow = sensorStatus.isWalkingNow,
            activeSourceLabel = sensorStatus.activeSourceLabel,
            walkingActivityLabel = walkingLabel,
            statusMessage = sensorStatus.statusMessage,
            weeklyHistory = weekly,
            monthlyHistory = monthly,
            weeklyAverage = if (weekly.isEmpty()) 0 else weekly.sumOf { it.steps } / weekly.size,
            monthlyTotal = monthly.sumOf { it.steps }
        )
    }
}

/**
 * Dedicated StepTrackingViewModel coordinating automatic step sensor lifecycle,
 * Health Connect sync, and reactive StepTrackingUiState.
 */
class StepTrackingViewModel(
    private val repository: StepTrackingRepository,
    private val userId: String
) : ViewModel() {

    private val _isSyncingHealthConnect = MutableStateFlow(false)
    val isSyncingHealthConnect: StateFlow<Boolean> = _isSyncingHealthConnect.asStateFlow()

    val sensorStatus: StateFlow<LiveStepSensorStatus> = repository.liveSensorStatus

    fun activateAutomaticTracking() {
        repository.startLiveTracking()
        repository.scheduleBackgroundStepSync(userId)
        syncHealthConnect()
    }

    fun syncHealthConnect() {
        viewModelScope.launch {
            _isSyncingHealthConnect.value = true
            runCatching { repository.syncHealthConnectAndPersist(userId) }
            _isSyncingHealthConnect.value = false
        }
    }

    override fun onCleared() {
        repository.stopLiveTracking()
        super.onCleared()
    }
}
