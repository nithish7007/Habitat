package com.example.data

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class HealthConnectAvailability {
    AVAILABLE,
    UPDATE_REQUIRED,
    UNAVAILABLE
}

data class HealthConnectSnapshot(
    val availability: HealthConnectAvailability = HealthConnectAvailability.UNAVAILABLE,
    val permissionsGranted: Boolean = false,
    val todaySteps: Int = 0,
    val todayDistanceKm: Double? = null,
    val dailyStepsByIso: Map<String, Int> = emptyMap()
)

/**
 * Real Jetpack Health Connect integration for automatic deduplicated daily & historical steps
 * and measured walking distance.
 * Uses HealthConnectClient.aggregate() (StepsRecord.COUNT_TOTAL, DistanceRecord.DISTANCE_TOTAL)
 * to avoid double counting across phone + wearable sources.
 */
class HealthConnectStepManager(context: Context) {

    private val appContext = context.applicationContext

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class)
    )

    fun getAvailability(): HealthConnectAvailability {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return HealthConnectAvailability.UNAVAILABLE
        }
        return runCatching {
            when (HealthConnectClient.getSdkStatus(appContext, PROVIDER_PACKAGE)) {
                HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
                else -> HealthConnectAvailability.UNAVAILABLE
            }
        }.getOrDefault(HealthConnectAvailability.UNAVAILABLE)
    }

    suspend fun hasGrantedPermissions(): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@withContext false
        if (getAvailability() != HealthConnectAvailability.AVAILABLE) return@withContext false
        runCatching {
            val client = HealthConnectClient.getOrCreate(appContext)
            val granted = client.permissionController.getGrantedPermissions()
            granted.contains(HealthPermission.getReadPermission(StepsRecord::class))
        }.getOrDefault(false)
    }

    suspend fun readSnapshot(daysToRead: Int = 30): HealthConnectSnapshot = withContext(Dispatchers.IO) {
        val availability = getAvailability()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || availability != HealthConnectAvailability.AVAILABLE) {
            return@withContext HealthConnectSnapshot(availability = availability)
        }
        return@withContext readSnapshotApi26(daysToRead)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun readSnapshotApi26(daysToRead: Int): HealthConnectSnapshot {
        return runCatching {
            val client = HealthConnectClient.getOrCreate(appContext)
            val granted = client.permissionController.getGrantedPermissions()
            val hasStepsPerm = granted.contains(HealthPermission.getReadPermission(StepsRecord::class))
            if (!hasStepsPerm) {
                return@runCatching HealthConnectSnapshot(
                    availability = HealthConnectAvailability.AVAILABLE,
                    permissionsGranted = false
                )
            }
            val hasDistancePerm = granted.contains(HealthPermission.getReadPermission(DistanceRecord::class))
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            val dailyMap = linkedMapOf<String, Int>()
            var todayDistanceKm: Double? = null

            val safeDays = daysToRead.coerceIn(1, 30)
            for (offset in (safeDays - 1) downTo 0) {
                val date = today.minusDays(offset.toLong())
                val startInstant = date.atStartOfDay(zone).toInstant()
                val endInstant = if (offset == 0) {
                    Instant.now()
                } else {
                    date.plusDays(1).atStartOfDay(zone).toInstant()
                }
                if (!endInstant.isAfter(startInstant)) continue

                val metrics = buildSet {
                    add(StepsRecord.COUNT_TOTAL)
                    if (offset == 0 && hasDistancePerm) {
                        add(DistanceRecord.DISTANCE_TOTAL)
                    }
                }
                val agg = client.aggregate(
                    AggregateRequest(
                        metrics = metrics,
                        timeRangeFilter = TimeRangeFilter.between(startInstant, endInstant)
                    )
                )
                val stepCount = (agg[StepsRecord.COUNT_TOTAL] ?: 0L).toInt().coerceAtLeast(0)
                if (stepCount > 0) {
                    dailyMap[date.format(formatter)] = stepCount
                }
                if (offset == 0 && hasDistancePerm) {
                    val meters = agg[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
                    if (meters > 0.0) {
                        todayDistanceKm = meters / 1000.0
                    }
                }
            }

            val todayIso = today.format(formatter)
            HealthConnectSnapshot(
                availability = HealthConnectAvailability.AVAILABLE,
                permissionsGranted = true,
                todaySteps = dailyMap[todayIso] ?: 0,
                todayDistanceKm = todayDistanceKm,
                dailyStepsByIso = dailyMap
            )
        }.getOrDefault(
            HealthConnectSnapshot(
                availability = HealthConnectAvailability.AVAILABLE,
                permissionsGranted = false
            )
        )
    }

    companion object {
        const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
    }
}
