package com.example

import com.example.data.DateUtils
import com.example.data.LiveStepSensorStatus
import com.example.data.StepCalculationEngine
import com.example.data.StepLogEntity
import com.example.data.StepSensorState
import com.example.ui.StepTrackingStateMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StepTrackingEngineTest {

    @Test
    fun initialCumulativeReadingSetsBaselineWithoutInflatingSteps() {
        val state = StepCalculationEngine.processCumulativeReading(
            previous = StepSensorState(),
            cumulative = 48_200,
            todayIso = "2026-11-19"
        )
        assertEquals("2026-11-19", state.dateIso)
        assertEquals(0, state.todaySteps)
        assertEquals(48_200, state.baselineCumulative)
        assertEquals(48_200, state.lastCumulative)
    }

    @Test
    fun cumulativeWalkIncrementsAccuratelyAndNeverDoubleCountsDuplicates() {
        val s0 = StepCalculationEngine.processCumulativeReading(
            previous = StepSensorState(),
            cumulative = 10_000,
            todayIso = "2026-11-19"
        )
        val s1 = StepCalculationEngine.processCumulativeReading(s0, 14_250, "2026-11-19")
        assertEquals(4_250, s1.todaySteps)

        // Duplicate sensor callback with same cumulative reading -> zero double counting
        val s2 = StepCalculationEngine.processCumulativeReading(s1, 14_250, "2026-11-19")
        assertEquals(4_250, s2.todaySteps)

        val s3 = StepCalculationEngine.processCumulativeReading(s2, 15_000, "2026-11-19")
        assertEquals(5_000, s3.todaySteps)
    }

    @Test
    fun midDayPhoneRebootPreservesPriorStepsAndContinuesCounting() {
        val morning = StepCalculationEngine.processCumulativeReading(
            previous = StepSensorState(),
            cumulative = 30_000,
            todayIso = "2026-11-19"
        )
        val preReboot = StepCalculationEngine.processCumulativeReading(morning, 33_400, "2026-11-19")
        assertEquals(3_400, preReboot.todaySteps)

        // Phone reboots: Android TYPE_STEP_COUNTER resets to 0, user walks 250 steps after boot
        val postReboot1 = StepCalculationEngine.processCumulativeReading(preReboot, 250, "2026-11-19")
        assertEquals(3_650, postReboot1.todaySteps)
        assertEquals(3_400, postReboot1.carriedSteps)
        assertEquals(0, postReboot1.baselineCumulative)

        // User walks 600 more steps (cumulative now 850 since reboot)
        val postReboot2 = StepCalculationEngine.processCumulativeReading(postReboot1, 850, "2026-11-19")
        assertEquals(4_250, postReboot2.todaySteps)
    }

    @Test
    fun appRestartPersistedStateContinuesAccurately() {
        // Simulate state restored from SharedPreferences after app restart
        val restored = StepSensorState(
            dateIso = "2026-11-19",
            carriedSteps = 0,
            baselineCumulative = 20_000,
            lastCumulative = 24_250,
            todaySteps = 4_250
        )
        // First sensor emission after restart has accumulated 300 more steps while app was closed
        val afterRestart = StepCalculationEngine.processCumulativeReading(restored, 24_550, "2026-11-19")
        assertEquals(4_550, afterRestart.todaySteps)
    }

    @Test
    fun localCalendarDateRolloverResetsDailyStepsCleanly() {
        val yesterday = StepSensorState(
            dateIso = "2026-11-18",
            carriedSteps = 0,
            baselineCumulative = 12_000,
            lastCumulative = 21_500,
            todaySteps = 9_500
        )
        val newDayFirstEvent = StepCalculationEngine.processCumulativeReading(yesterday, 21_600, "2026-11-19")
        assertEquals("2026-11-19", newDayFirstEvent.dateIso)
        assertEquals(0, newDayFirstEvent.todaySteps)
        assertEquals(21_600, newDayFirstEvent.baselineCumulative)

        val newDayWalk = StepCalculationEngine.processCumulativeReading(newDayFirstEvent, 22_050, "2026-11-19")
        assertEquals(450, newDayWalk.todaySteps)
    }

    @Test
    fun stepDetectorFallbackCountsPulsesAndResetsOnNewDate() {
        var state = StepSensorState(dateIso = "2026-11-19", todaySteps = 0)
        repeat(15) {
            state = StepCalculationEngine.processStepDetectorPulse(state, "2026-11-19")
        }
        assertEquals(15, state.todaySteps)

        val nextDay = StepCalculationEngine.processStepDetectorPulse(state, "2026-11-20")
        assertEquals("2026-11-20", nextDay.dateIso)
        assertEquals(1, nextDay.todaySteps)
    }

    @Test
    fun mergeAutomaticSourcesDeduplicatesWithoutDoubleCounting() {
        val merged = StepCalculationEngine.mergeAutomaticSources(
            sensorTodaySteps = 4_250,
            healthConnectTodaySteps = 4_410,
            persistedRoomSteps = 4_100
        )
        assertEquals(4_410, merged)
        // Must NEVER equal sum (12,760)
        assertTrue(merged < 4_250 + 4_410)
    }

    @Test
    fun computeMetricsCalculatesPercentageAndRemainingStepsAccurately() {
        val mid = StepCalculationEngine.computeMetrics(todaySteps = 4_250, dailyGoal = 10_000)
        assertEquals(4_250, mid.todaySteps)
        assertEquals(10_000, mid.dailyGoal)
        assertEquals(0.425f, mid.progressFraction, 0.0001f)
        assertEquals(42.5f, mid.progressPercent, 0.01f)
        assertEquals(5_750, mid.remainingSteps)
        assertFalse(mid.goalReached)

        val exceeded = StepCalculationEngine.computeMetrics(todaySteps = 11_200, dailyGoal = 10_000)
        assertEquals(1.0f, exceeded.progressFraction, 0.0001f)
        assertEquals(100.0f, exceeded.progressPercent, 0.01f)
        assertEquals(0, exceeded.remainingSteps)
        assertTrue(exceeded.goalReached)
    }

    @Test
    fun stateMapperShowsHonestUnavailableStateWithoutFabricatingSteps() {
        val today = DateUtils.todayIso()
        val unavailableStatus = LiveStepSensorStatus(
            sensorAvailable = false,
            stepDetectorAvailable = false,
            permissionGranted = false,
            trackingActive = false,
            healthConnectAvailable = false,
            healthConnectGranted = false,
            todaySensorSteps = 0,
            healthConnectTodaySteps = 0,
            statusMessage = "Automatic step tracking is unavailable on this device."
        )
        val ui = StepTrackingStateMapper.build(
            selectedDateIso = today,
            dailyGoal = 10_000,
            stepLogs = emptyList(),
            sensorStatus = unavailableStatus
        )
        assertEquals(0, ui.todaySteps)
        assertEquals(10_000, ui.remainingSteps)
        assertTrue(ui.isTrackingUnavailable)
        assertNull(ui.distanceKm)
    }

    @Test
    fun stateMapperUsesMeasuredHealthConnectDistanceWhenAvailable() {
        val today = DateUtils.todayIso()
        val activeStatus = LiveStepSensorStatus(
            sensorAvailable = true,
            permissionGranted = true,
            trackingActive = true,
            healthConnectAvailable = true,
            healthConnectGranted = true,
            todaySensorSteps = 4_100,
            healthConnectTodaySteps = 4_250,
            healthConnectDistanceKm = 3.18
        )
        val ui = StepTrackingStateMapper.build(
            selectedDateIso = today,
            dailyGoal = 10_000,
            stepLogs = listOf(StepLogEntity(userId = "u1", dateIso = today, steps = 4_000, source = "STEP_SENSOR")),
            sensorStatus = activeStatus
        )
        assertEquals(4_250, ui.todaySteps)
        assertEquals(42.5f, ui.progressPercent, 0.01f)
        assertEquals(5_750, ui.remainingSteps)
        assertEquals(3.18, ui.distanceKm ?: 0.0, 0.001)
        assertTrue(ui.distanceIsMeasured)
        assertFalse(ui.isTrackingUnavailable)
    }
}
