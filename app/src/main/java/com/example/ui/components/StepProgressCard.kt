package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.SensorsOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.StepTrackingUiState
import com.example.ui.theme.WarmAmber
import java.util.Locale

/**
 * Reusable Apple Liquid Glass Automatic Step Progress Card (`StepProgressCard`).
 * Shows real-time automatic step count, daily goal, exact percentage, remaining steps,
 * walking activity status, and distance — NEVER offers manual step entry.
 */
@Composable
fun StepProgressCard(
    stepState: StepTrackingUiState,
    onOpenDetails: (() -> Unit)? = null,
    onEnableActivityPermission: (() -> Unit)? = null,
    onConnectHealthConnect: (() -> Unit)? = null,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val animatedProgress by animateFloatAsState(
        targetValue = stepState.progressFraction,
        animationSpec = tween(650),
        label = "step_progress_card_anim"
    )
    val accent = when {
        stepState.isTrackingUnavailable -> WarmAmber
        stepState.goalReached -> scheme.tertiary
        stepState.sensorTrackingActive || stepState.healthConnectGranted -> scheme.tertiary
        else -> scheme.primary
    }

    LifeOSGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("step_progress_card"),
        cornerRadius = 26.dp,
        accentTint = accent,
        onClick = onOpenDetails
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 16.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LifeOsBadgeIcon(type = LifeOsBadgeType.STEPS, size = 36.dp)
                    Column {
                        Text(
                            text = "Automatic Steps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stepState.activeSourceLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
                ApplePillBadge(
                    text = stepState.walkingActivityLabel,
                    tint = if (stepState.isTrackingUnavailable) WarmAmber else accent
                )
            }

            if (stepState.isTrackingUnavailable) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(WarmAmber.copy(alpha = 0.14f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SensorsOff,
                        contentDescription = "Automatic step sensor unavailable",
                        tint = WarmAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automatic Step Tracking Unavailable",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "This device has no built-in step sensor and no authorized Health Connect data. LIFE OS never generates fake step counts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(if (compact) 86.dp else 108.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = if (compact) 10.dp.toPx() else 12.dp.toPx()
                            drawArc(
                                color = accent.copy(alpha = 0.16f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = accent,
                                startAngle = -90f,
                                sweepAngle = animatedProgress * 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (stepState.goalReached) Icons.Rounded.EmojiEvents else Icons.AutoMirrored.Rounded.DirectionsWalk,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(if (compact) 18.dp else 22.dp)
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%%", stepState.progressPercent),
                                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = accent
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${String.format(Locale.getDefault(), "%,d", stepState.todaySteps)} / ${String.format(Locale.getDefault(), "%,d", stepState.dailyGoal)}",
                            style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface
                        )
                        Text(
                            text = if (stepState.goalReached) {
                                "Daily step goal achieved!"
                            } else {
                                "Remaining: ${String.format(Locale.getDefault(), "%,d", stepState.remainingSteps)} steps"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (stepState.goalReached) scheme.tertiary else scheme.onSurfaceVariant
                        )
                        stepState.distanceKm?.let { km ->
                            Text(
                                text = String.format(
                                    Locale.getDefault(),
                                    "%.2f km %s",
                                    km,
                                    if (stepState.distanceIsMeasured) "(Health Connect)" else "(est. stride)"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.primary
                            )
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = accent,
                    trackColor = scheme.surfaceContainerHighest.copy(alpha = 0.65f),
                    strokeCap = StrokeCap.Round
                )

                // Contextual permission activation actions (only shown when permission isn't granted)
                val needsSensorPerm = stepState.sensorAvailable && !stepState.activityPermissionGranted
                val canConnectHc = stepState.healthConnectAvailable && !stepState.healthConnectGranted
                if ((needsSensorPerm && onEnableActivityPermission != null) || (canConnectHc && onConnectHealthConnect != null)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (needsSensorPerm && onEnableActivityPermission != null) {
                            Button(
                                onClick = onEnableActivityPermission,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("step_card_enable_sensor_button")
                            ) {
                                Icon(Icons.Rounded.Sensors, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Enable Sensor")
                            }
                        }
                        if (canConnectHc && onConnectHealthConnect != null) {
                            FilledTonalButton(
                                onClick = onConnectHealthConnect,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("step_card_connect_hc_button")
                            ) {
                                Text("Health Connect")
                            }
                        }
                    }
                }
            }
        }
    }
}
