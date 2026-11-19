package com.example.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.TimeZone

/**
 * Persisted sensor baseline state for accurate local-day step calculation
 * across app restarts, device reboots, date rollovers, and timezone changes.
 */
data class StepSensorState(
    val dateIso: String = "",
    val carriedSteps: Int = 0,
    val baselineCumulative: Int = -1,
    val lastCumulative: Int = -1,
    val todaySteps: Int = 0,
    val timezoneId: String = TimeZone.getDefault().id
)

data class StepProgressMetrics(
    val todaySteps: Int,
    val dailyGoal: Int,
    val progressFraction: Float,
    val progressPercent: Float,
    val remainingSteps: Int,
    val goalReached: Boolean
)

/**
 * Pure Kotlin, deterministic, unit-testable engine for automatic daily step calculation.
 * Never double-counts cumulative sensor emissions or overlapping Health Connect records.
 */
object StepCalculationEngine {

    fun processCumulativeReading(
        previous: StepSensorState,
        cumulative: Int,
        todayIso: String,
        timezoneId: String = TimeZone.getDefault().id
    ): StepSensorState {
        if (cumulative < 0) return previous

        // 1. Local calendar date changed -> reset daily total cleanly to 0 and anchor new baseline
        if (previous.dateIso != todayIso) {
            return StepSensorState(
                dateIso = todayIso,
                carriedSteps = 0,
                baselineCumulative = cumulative,
                lastCumulative = cumulative,
                todaySteps = 0,
                timezoneId = timezoneId
            )
        }

        // 2. First cumulative reading on this date (no baseline yet)
        if (previous.baselineCumulative < 0) {
            return previous.copy(
                baselineCumulative = cumulative,
                lastCumulative = cumulative,
                timezoneId = timezoneId
            )
        }

        // 3. Device rebooted mid-day (Android hardware counter reset to 0 after reboot)
        if (previous.lastCumulative >= 0 && cumulative < previous.lastCumulative) {
            val preservedBeforeReboot = previous.todaySteps.coerceAtLeast(0)
            val newToday = preservedBeforeReboot + cumulative
            return StepSensorState(
                dateIso = todayIso,
                carriedSteps = preservedBeforeReboot,
                baselineCumulative = 0,
                lastCumulative = cumulative,
                todaySteps = newToday,
                timezoneId = timezoneId
            )
        }

        // 4. Normal cumulative progression (repeated identical readings yield identical delta -> zero double-counting)
        val deltaSinceBaseline = (cumulative - previous.baselineCumulative).coerceAtLeast(0)
        val computed = (previous.carriedSteps + deltaSinceBaseline).coerceAtLeast(previous.todaySteps)
        return previous.copy(
            lastCumulative = cumulative,
            todaySteps = computed,
            timezoneId = timezoneId
        )
    }

    /**
     * Used ONLY on devices that expose TYPE_STEP_DETECTOR without TYPE_STEP_COUNTER.
     */
    fun processStepDetectorPulse(
        previous: StepSensorState,
        todayIso: String,
        timezoneId: String = TimeZone.getDefault().id
    ): StepSensorState {
        if (previous.dateIso != todayIso) {
            return StepSensorState(
                dateIso = todayIso,
                carriedSteps = 0,
                baselineCumulative = -1,
                lastCumulative = -1,
                todaySteps = 1,
                timezoneId = timezoneId
            )
        }
        return previous.copy(
            todaySteps = previous.todaySteps + 1,
            timezoneId = timezoneId
        )
    }

    /**
     * Deduplicates automatic step sources (Hardware Sensor, Health Connect, persisted daily Room total).
     * Never sums overlapping readings from the same device walk.
     */
    fun mergeAutomaticSources(
        sensorTodaySteps: Int,
        healthConnectTodaySteps: Int,
        persistedRoomSteps: Int
    ): Int {
        return maxOf(
            sensorTodaySteps.coerceAtLeast(0),
            healthConnectTodaySteps.coerceAtLeast(0),
            persistedRoomSteps.coerceAtLeast(0)
        )
    }

    fun computeMetrics(todaySteps: Int, dailyGoal: Int): StepProgressMetrics {
        val safeSteps = todaySteps.coerceAtLeast(0)
        val safeGoal = dailyGoal.coerceAtLeast(500)
        val rawPercent = (safeSteps.toFloat() / safeGoal.toFloat()) * 100f
        val roundedPercent = ((rawPercent.coerceAtMost(100f) * 10f).toInt() / 10f)
        return StepProgressMetrics(
            todaySteps = safeSteps,
            dailyGoal = safeGoal,
            progressFraction = (safeSteps.toFloat() / safeGoal.toFloat()).coerceIn(0f, 1f),
            progressPercent = roundedPercent,
            remainingSteps = (safeGoal - safeSteps).coerceAtLeast(0),
            goalReached = safeSteps >= safeGoal
        )
    }

    /**
     * Conservative stride estimate (~0.75 m per step) used only as fallback
     * when Health Connect measured distance is unavailable.
     */
    fun estimateDistanceKm(steps: Int): Double {
        if (steps <= 0) return 0.0
        return steps * 0.00075
    }
}

data class LiveStepSensorStatus(
    val sensorAvailable: Boolean = false,
    val stepCounterAvailable: Boolean = false,
    val stepDetectorAvailable: Boolean = false,
    val permissionGranted: Boolean = false,
    val trackingActive: Boolean = false,
    val todaySensorSteps: Int = 0,
    val estimatedDistanceKm: Double = 0.0,
    val isWalkingNow: Boolean = false,
    val lastStepEpochMs: Long = 0L,
    val healthConnectAvailable: Boolean = false,
    val healthConnectGranted: Boolean = false,
    val healthConnectTodaySteps: Int = 0,
    val healthConnectDistanceKm: Double? = null,
    val activeSourceLabel: String = "Checking sensors...",
    val statusMessage: String = "Checking device step sensors..."
) {
    val isAutomaticTrackingUnavailable: Boolean
        get() = !sensorAvailable && !healthConnectAvailable

    val effectiveTodaySteps: Int
        get() = maxOf(todaySensorSteps, healthConnectTodaySteps)

    val effectiveDistanceKm: Double?
        get() = when {
            healthConnectDistanceKm != null && healthConnectDistanceKm > 0.0 -> healthConnectDistanceKm
            effectiveTodaySteps > 0 -> StepCalculationEngine.estimateDistanceKm(effectiveTodaySteps)
            else -> null
        }

    val distanceIsMeasured: Boolean
        get() = healthConnectDistanceKm != null && healthConnectDistanceKm > 0.0
}

/**
 * Real Android automatic hardware step counter & detector manager.
 * - Uses TYPE_STEP_COUNTER (primary cumulative hardware sensor) + TYPE_STEP_DETECTOR (live walking activity & fallback).
 * - Requires runtime ACTIVITY_RECOGNITION permission on Android 10+ (API 29+).
 * - Persists daily baseline & carried count in SharedPreferences so daily totals survive app restarts & reboots.
 * - Never fabricates steps and never prompts for manual step entry.
 */
class StepCounterManager(
    context: Context,
    private val onDailyStepsUpdated: (dateIso: String, steps: Int) -> Unit
) : SensorEventListener {

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepCounterSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _status = MutableStateFlow(computeInitialStatus())
    val status: StateFlow<LiveStepSensorStatus> = _status.asStateFlow()

    private var counterRegistered = false
    private var detectorRegistered = false
    private var lastStepTimestampMs = 0L

    fun hasActivityRecognitionPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasHardwareStepSensor(): Boolean = stepCounterSensor != null || stepDetectorSensor != null

    fun readPersistedTodaySteps(todayIso: String = DateUtils.todayIso()): Int {
        val state = loadSensorState()
        return if (state.dateIso == todayIso) state.todaySteps else 0
    }

    private fun loadSensorState(): StepSensorState {
        return StepSensorState(
            dateIso = prefs.getString(KEY_DATE, "") ?: "",
            carriedSteps = prefs.getInt(KEY_CARRIED, 0),
            baselineCumulative = prefs.getInt(KEY_BASELINE, -1),
            lastCumulative = prefs.getInt(KEY_LAST_CUMULATIVE, -1),
            todaySteps = prefs.getInt(KEY_TODAY_STEPS, 0),
            timezoneId = prefs.getString(KEY_TIMEZONE, TimeZone.getDefault().id) ?: TimeZone.getDefault().id
        )
    }

    private fun persistSensorState(state: StepSensorState) {
        prefs.edit()
            .putString(KEY_DATE, state.dateIso)
            .putInt(KEY_CARRIED, state.carriedSteps)
            .putInt(KEY_BASELINE, state.baselineCumulative)
            .putInt(KEY_LAST_CUMULATIVE, state.lastCumulative)
            .putInt(KEY_TODAY_STEPS, state.todaySteps)
            .putString(KEY_TIMEZONE, state.timezoneId)
            .apply()
    }

    private fun computeInitialStatus(): LiveStepSensorStatus {
        val counterAvail = stepCounterSensor != null
        val detectorAvail = stepDetectorSensor != null
        val anySensor = counterAvail || detectorAvail
        val granted = hasActivityRecognitionPermission()
        val today = DateUtils.todayIso()
        val persistedToday = readPersistedTodaySteps(today)
        val active = counterRegistered || detectorRegistered

        val sourceLabel = when {
            active && counterAvail -> "Hardware Step Sensor"
            active && detectorAvail -> "Hardware Step Detector"
            anySensor && !granted -> "Permission Needed"
            else -> "Sensor Unavailable"
        }
        val msg = when {
            !anySensor -> "This device does not have a built-in step counter sensor. Connect Health Connect if available."
            !granted -> "Grant Physical Activity permission so LIFE OS can automatically count your steps in real time."
            active -> "Counting steps automatically via device hardware sensor."
            else -> "Ready to track steps automatically via device sensor."
        }
        return LiveStepSensorStatus(
            sensorAvailable = anySensor,
            stepCounterAvailable = counterAvail,
            stepDetectorAvailable = detectorAvail,
            permissionGranted = granted,
            trackingActive = active,
            todaySensorSteps = persistedToday,
            estimatedDistanceKm = StepCalculationEngine.estimateDistanceKm(persistedToday),
            isWalkingNow = (System.currentTimeMillis() - lastStepTimestampMs) in 1..15_000L,
            lastStepEpochMs = lastStepTimestampMs,
            activeSourceLabel = sourceLabel,
            statusMessage = msg
        )
    }

    fun updateHealthConnectOverlay(
        available: Boolean,
        granted: Boolean,
        hcTodaySteps: Int,
        hcDistanceKm: Double?
    ) {
        val cur = _status.value
        val sourceLabel = when {
            cur.trackingActive && granted -> "Sensor + Health Connect"
            cur.trackingActive -> if (cur.stepCounterAvailable) "Hardware Step Sensor" else "Hardware Step Detector"
            granted -> "Health Connect"
            cur.sensorAvailable && !cur.permissionGranted -> "Permission Needed"
            available && !granted -> "Health Connect Ready"
            else -> "Unavailable on Device"
        }
        val msg = when {
            cur.trackingActive && granted -> "Hardware step sensor active and synced with Health Connect."
            cur.trackingActive -> "Automatic hardware step sensor active."
            granted -> "Reading automatic daily steps from Health Connect."
            cur.sensorAvailable && !cur.permissionGranted -> "Physical Activity permission is required to read your phone's automatic step sensor."
            available && !granted -> "Connect Health Connect to import automatic steps from your phone or wearables."
            else -> "Automatic step tracking is unavailable on this device (no built-in step sensor and Health Connect not installed)."
        }
        _status.value = cur.copy(
            healthConnectAvailable = available,
            healthConnectGranted = granted,
            healthConnectTodaySteps = hcTodaySteps.coerceAtLeast(0),
            healthConnectDistanceKm = hcDistanceKm,
            activeSourceLabel = sourceLabel,
            statusMessage = msg
        )
    }

    fun refreshAndStartIfPermitted() {
        val anySensor = hasHardwareStepSensor()
        val granted = hasActivityRecognitionPermission()
        if (!anySensor || !granted) {
            stop()
            val cur = _status.value
            val refreshed = computeInitialStatus()
            _status.value = refreshed.copy(
                healthConnectAvailable = cur.healthConnectAvailable,
                healthConnectGranted = cur.healthConnectGranted,
                healthConnectTodaySteps = cur.healthConnectTodaySteps,
                healthConnectDistanceKm = cur.healthConnectDistanceKm
            )
            updateHealthConnectOverlay(
                cur.healthConnectAvailable,
                cur.healthConnectGranted,
                cur.healthConnectTodaySteps,
                cur.healthConnectDistanceKm
            )
            return
        }

        if (!counterRegistered && stepCounterSensor != null) {
            counterRegistered = sensorManager?.registerListener(
                this,
                stepCounterSensor,
                SensorManager.SENSOR_DELAY_UI
            ) == true
        }
        if (!detectorRegistered && stepDetectorSensor != null) {
            detectorRegistered = sensorManager?.registerListener(
                this,
                stepDetectorSensor,
                SensorManager.SENSOR_DELAY_UI
            ) == true
        }

        val cur = _status.value
        val refreshed = computeInitialStatus()
        _status.value = refreshed.copy(
            healthConnectAvailable = cur.healthConnectAvailable,
            healthConnectGranted = cur.healthConnectGranted,
            healthConnectTodaySteps = cur.healthConnectTodaySteps,
            healthConnectDistanceKm = cur.healthConnectDistanceKm
        )
        updateHealthConnectOverlay(
            cur.healthConnectAvailable,
            cur.healthConnectGranted,
            cur.healthConnectTodaySteps,
            cur.healthConnectDistanceKm
        )
    }

    fun stop() {
        if (counterRegistered || detectorRegistered) {
            runCatching { sensorManager?.unregisterListener(this) }
            counterRegistered = false
            detectorRegistered = false
        }
        _status.value = _status.value.copy(trackingActive = false, isWalkingNow = false)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val today = DateUtils.todayIso()
        val tzId = TimeZone.getDefault().id
        val nowMs = System.currentTimeMillis()

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val cumulative = event.values.firstOrNull()?.toInt() ?: return
                val previous = loadSensorState()
                val next = StepCalculationEngine.processCumulativeReading(
                    previous = previous,
                    cumulative = cumulative,
                    todayIso = today,
                    timezoneId = tzId
                )
                persistSensorState(next)
                if (next.todaySteps > previous.todaySteps) {
                    lastStepTimestampMs = nowMs
                }
                emitLiveUpdate(today, next.todaySteps, walking = (nowMs - lastStepTimestampMs) <= 15_000L)
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                lastStepTimestampMs = nowMs
                if (stepCounterSensor == null) {
                    // Fallback ONLY when hardware lacks TYPE_STEP_COUNTER
                    val previous = loadSensorState()
                    val next = StepCalculationEngine.processStepDetectorPulse(
                        previous = previous,
                        todayIso = today,
                        timezoneId = tzId
                    )
                    persistSensorState(next)
                    emitLiveUpdate(today, next.todaySteps, walking = true)
                } else {
                    // Live walking cadence signal while TYPE_STEP_COUNTER handles exact total
                    _status.value = _status.value.copy(
                        isWalkingNow = true,
                        lastStepEpochMs = nowMs
                    )
                }
            }
        }
    }

    private fun emitLiveUpdate(today: String, computedTodaySteps: Int, walking: Boolean) {
        val cur = _status.value
        _status.value = cur.copy(
            sensorAvailable = true,
            permissionGranted = true,
            trackingActive = true,
            todaySensorSteps = computedTodaySteps,
            estimatedDistanceKm = StepCalculationEngine.estimateDistanceKm(computedTodaySteps),
            isWalkingNow = walking,
            lastStepEpochMs = lastStepTimestampMs
        )
        if (computedTodaySteps > 0) {
            onDailyStepsUpdated(today, computedTodaySteps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val PREFS_NAME = "lifeos_step_sensor_prefs"
        private const val KEY_DATE = "step_date_iso"
        private const val KEY_CARRIED = "step_carried_count"
        private const val KEY_BASELINE = "step_baseline_cumulative"
        private const val KEY_LAST_CUMULATIVE = "step_last_cumulative"
        private const val KEY_TODAY_STEPS = "step_today_count"
        private const val KEY_TIMEZONE = "step_timezone_id"

        fun estimateDistanceKm(steps: Int): Double = StepCalculationEngine.estimateDistanceKm(steps)
    }
}
