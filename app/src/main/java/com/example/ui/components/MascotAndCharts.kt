package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun AnimatedLifeOsMascot(
    size: Dp = 156.dp,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "mascot_motion")
    val floatY by infinite.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_float"
    )
    val auraScale by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_aura"
    )

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Ambient animated luminous aura behind 3D mascot
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(auraScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.35f),
                            secondary.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .size(size * 0.84f)
                .offset(y = floatY.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primary.copy(alpha = 0.8f),
                            secondary.copy(alpha = 0.7f),
                            tertiary.copy(alpha = 0.65f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_lifeos_mascot_1795076223652),
                contentDescription = "LIFE OS 3D mascot working at a computer",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun ProductivityScoreRing(
    scorePercent: Int?,
    size: Dp = 112.dp,
    strokeWidth: Dp = 11.dp,
    modifier: Modifier = Modifier
) {
    val targetSweep = if (scorePercent != null) (scorePercent.coerceIn(0, 100) / 100f) * 360f else 0f
    val animatedSweep by animateFloatAsState(
        targetValue = targetSweep,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "score_ring"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            if (animatedSweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(primary, secondary, tertiary, primary)),
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (scorePercent != null) "$scorePercent%" else "--",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (scorePercent != null) "SCORE" else "NO DATA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun WeeklyTrendBarChart(
    labels: List<String>,
    values: List<Int>,
    maxValue: Int = 100,
    barColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val safeMax = maxValue.coerceAtLeast(1)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
        ) {
            if (values.isEmpty()) return@Canvas
            val count = values.size
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.46f).coerceAtMost(28.dp.toPx())
            val radius = CornerRadius(8.dp.toPx(), 8.dp.toPx())

            values.forEachIndexed { idx, v ->
                val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                drawRoundRect(
                    color = track,
                    topLeft = Offset(left, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = radius
                )
                val fraction = (v.toFloat() / safeMax).coerceIn(0f, 1f)
                val barHeight = (size.height * fraction).coerceAtLeast(if (v > 0) 6.dp.toPx() else 0f)
                if (barHeight > 0f) {
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = radius
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            labels.forEachIndexed { i, label ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${values.getOrElse(i) { 0 }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Minimal reactive animated mascot character reacting to real daily progress & streak:
 * - HAPPY: bright smile, lively bounce, celebratory sparkles
 * - NEUTRAL: calm focused smile, gentle float
 * - SAD: gentle supportive expression (never aggressive or shaming)
 */
@Composable
fun ReactiveMinimalMascot(
    mood: com.example.data.MascotMood,
    celebrate: Boolean = false,
    size: Dp = 74.dp,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "reactive_mascot")
    val floatY by infinite.animateFloat(
        initialValue = if (mood == com.example.data.MascotMood.HAPPY) -4f else -2f,
        targetValue = if (mood == com.example.data.MascotMood.HAPPY) 4f else 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (mood == com.example.data.MascotMood.HAPPY) 1400 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reactive_float"
    )
    val blinkPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200),
            repeatMode = RepeatMode.Restart
        ),
        label = "reactive_blink"
    )
    val isBlinking = blinkPhase > 0.93f

    val scheme = MaterialTheme.colorScheme
    val bodyBrush = when (mood) {
        com.example.data.MascotMood.HAPPY -> Brush.linearGradient(listOf(scheme.tertiary, scheme.secondary))
        com.example.data.MascotMood.NEUTRAL -> Brush.linearGradient(listOf(scheme.primary, scheme.secondary))
        com.example.data.MascotMood.SAD -> Brush.linearGradient(listOf(scheme.secondary.copy(alpha = 0.78f), scheme.primary.copy(alpha = 0.72f)))
    }
    val glowColor = when (mood) {
        com.example.data.MascotMood.HAPPY -> scheme.tertiary
        com.example.data.MascotMood.NEUTRAL -> scheme.primary
        com.example.data.MascotMood.SAD -> scheme.secondary
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = floatY.dp)
        ) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val r = minOf(w, h) * 0.38f

            // Soft glow halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r * 1.45f
                ),
                radius = r * 1.45f,
                center = Offset(cx, cy)
            )

            // Minimal rounded mascot body
            drawCircle(
                brush = bodyBrush,
                radius = r,
                center = Offset(cx, cy)
            )

            // Gloss highlight (Apple glass touch)
            drawCircle(
                color = Color.White.copy(alpha = 0.22f),
                radius = r * 0.34f,
                center = Offset(cx - r * 0.32f, cy - r * 0.34f)
            )

            // Eyes (blinking animation)
            val eyeY = cy - r * 0.12f
            val eyeDx = r * 0.34f
            val eyeRadius = r * 0.11f
            if (isBlinking) {
                drawLine(
                    color = Color.White,
                    start = Offset(cx - eyeDx - eyeRadius, eyeY),
                    end = Offset(cx - eyeDx + eyeRadius, eyeY),
                    strokeWidth = r * 0.08f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(cx + eyeDx - eyeRadius, eyeY),
                    end = Offset(cx + eyeDx + eyeRadius, eyeY),
                    strokeWidth = r * 0.08f,
                    cap = StrokeCap.Round
                )
            } else {
                drawCircle(Color.White, radius = eyeRadius, center = Offset(cx - eyeDx, eyeY))
                drawCircle(Color.White, radius = eyeRadius, center = Offset(cx + eyeDx, eyeY))
                drawCircle(Color(0xFF0B132B), radius = eyeRadius * 0.48f, center = Offset(cx - eyeDx, eyeY))
                drawCircle(Color(0xFF0B132B), radius = eyeRadius * 0.48f, center = Offset(cx + eyeDx, eyeY))
            }

            // Expression mouth
            val mouthStroke = Stroke(width = r * 0.09f, cap = StrokeCap.Round)
            when (mood) {
                com.example.data.MascotMood.HAPPY -> {
                    drawArc(
                        color = Color.White,
                        startAngle = 15f,
                        sweepAngle = 150f,
                        useCenter = false,
                        topLeft = Offset(cx - r * 0.34f, cy + r * 0.02f),
                        size = Size(r * 0.68f, r * 0.48f),
                        style = mouthStroke
                    )
                }
                com.example.data.MascotMood.NEUTRAL -> {
                    drawArc(
                        color = Color.White,
                        startAngle = 25f,
                        sweepAngle = 130f,
                        useCenter = false,
                        topLeft = Offset(cx - r * 0.28f, cy + r * 0.10f),
                        size = Size(r * 0.56f, r * 0.28f),
                        style = mouthStroke
                    )
                }
                com.example.data.MascotMood.SAD -> {
                    drawArc(
                        color = Color.White.copy(alpha = 0.92f),
                        startAngle = 205f,
                        sweepAngle = 130f,
                        useCenter = false,
                        topLeft = Offset(cx - r * 0.28f, cy + r * 0.20f),
                        size = Size(r * 0.56f, r * 0.30f),
                        style = mouthStroke
                    )
                }
            }

            // Celebratory sparkles
            if (celebrate || mood == com.example.data.MascotMood.HAPPY) {
                val sparkColor = Color(0xFFFBBF24)
                drawCircle(sparkColor, radius = r * 0.11f, center = Offset(cx + r * 0.95f, cy - r * 0.78f))
                drawCircle(scheme.tertiary, radius = r * 0.08f, center = Offset(cx - r * 0.98f, cy - r * 0.58f))
                drawCircle(Color.White.copy(alpha = 0.85f), radius = r * 0.07f, center = Offset(cx + r * 1.05f, cy + r * 0.35f))
            }
        }
    }
}

/**
 * Animated translucent glass water bottle filling smoothly with hydration progress.
 */
@Composable
fun AnimatedWaterBottle(
    progressFraction: Float,
    currentMl: Int,
    targetMl: Int,
    modifier: Modifier = Modifier
) {
    val animatedFill by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "water_fill"
    )
    val infinite = rememberInfiniteTransition(label = "water_wave")
    val waveShift by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "water_wave_shift"
    )
    val waterColor = MaterialTheme.colorScheme.secondary
    val waterAccent = MaterialTheme.colorScheme.tertiary
    val outline = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier.size(width = 96.dp, height = 140.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val capW = w * 0.38f
            val capH = h * 0.12f
            val bodyTop = capH + 4.dp.toPx()
            val bodyH = h - bodyTop - 4.dp.toPx()

            // Bottle cap
            drawRoundRect(
                color = outline.copy(alpha = 0.55f),
                topLeft = Offset((w - capW) / 2f, 2.dp.toPx()),
                size = Size(capW, capH),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
            // Bottle glass outline
            drawRoundRect(
                color = waterColor.copy(alpha = 0.08f),
                topLeft = Offset(8.dp.toPx(), bodyTop),
                size = Size(w - 16.dp.toPx(), bodyH),
                cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
            )
            // Water level
            val fillH = (bodyH - 8.dp.toPx()) * animatedFill
            if (fillH > 2f) {
                val topY = bodyTop + bodyH - 4.dp.toPx() - fillH + (waveShift * 2.dp.toPx())
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(waterAccent.copy(alpha = 0.85f), waterColor)),
                    topLeft = Offset(12.dp.toPx(), topY.coerceAtLeast(bodyTop + 4.dp.toPx())),
                    size = Size(w - 24.dp.toPx(), (bodyTop + bodyH - 4.dp.toPx() - topY).coerceAtLeast(4f)),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )
            }
            // Outer glass stroke
            drawRoundRect(
                color = outline.copy(alpha = 0.65f),
                topLeft = Offset(8.dp.toPx(), bodyTop),
                size = Size(w - 16.dp.toPx(), bodyH),
                cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(progressFraction * 100).toInt().coerceIn(0, 999)}%",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${currentMl}ml",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
