package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.LocalLifeOsGlass
import com.example.ui.theme.LocalReducedMotion
import java.io.File

data class FloatingNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Rich Apple iOS ambient atmospheric backdrop so translucent Liquid Glass cards
 * and floating capsules show genuine depth and soft refraction in Light & Dark modes.
 */
@Composable
fun AppleAmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val glass = LocalLifeOsGlass.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(glass.ambientCanvasTop, glass.ambientCanvasBottom)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.ambientGlowPrimary, Color.Transparent),
                    center = Offset(w * 0.12f, h * 0.06f),
                    radius = w * 0.88f
                ),
                radius = w * 0.88f,
                center = Offset(w * 0.12f, h * 0.06f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.ambientGlowSecondary, Color.Transparent),
                    center = Offset(w * 0.90f, h * 0.24f),
                    radius = w * 0.82f
                ),
                radius = w * 0.82f,
                center = Offset(w * 0.90f, h * 0.24f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.ambientGlowTertiary, Color.Transparent),
                    center = Offset(w * 0.22f, h * 0.72f),
                    radius = w * 0.76f
                ),
                radius = w * 0.76f,
                center = Offset(w * 0.22f, h * 0.72f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glass.ambientGlowPrimary.copy(alpha = glass.ambientGlowPrimary.alpha * 0.65f), Color.Transparent),
                    center = Offset(w * 0.84f, h * 0.92f),
                    radius = w * 0.70f
                ),
                radius = w * 0.70f,
                center = Offset(w * 0.84f, h * 0.92f)
            )
        }
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground
        ) {
            content()
        }
    }
}

/**
 * Primary reusable LIFE OS Liquid Glass surface (`LifeOSGlassCard`):
 * - Multi-layer translucent frost body over ambient backdrop
 * - Hardware-accelerated Android 12+ (API 31+) background blur sheen + optical frosted fallback on API <31
 * - Crisp top inner specular highlight hairline + directional glass rim border
 * - Soft realistic diffused shadow and tactile spring press physics
 */
@Composable
fun LifeOSGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    accentTint: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = LocalLifeOsGlass.current
    val reducedMotion = LocalReducedMotion.current
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && pressed && !reducedMotion) 0.976f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "lifeos_glass_press_scale"
    )

    val fillBrush = remember(glass, accentTint) {
        if (accentTint != null) {
            Brush.linearGradient(
                colors = listOf(
                    accentTint.copy(alpha = if (glass.isDark) 0.26f else 0.16f),
                    glass.glassFillTop,
                    glass.glassFillBottom
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(glass.glassFillTop, glass.glassFillBottom)
            )
        }
    }

    val specularRimBrush = remember(glass, accentTint) {
        Brush.linearGradient(
            colors = listOf(
                glass.glassBorderHighlight,
                glass.glassBorderMid,
                (accentTint ?: glass.glassBorderShadow).copy(alpha = if (glass.isDark) 0.42f else 0.28f)
            )
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (glass.isDark) 12.dp else 8.dp,
                shape = shape,
                ambientColor = glass.elevationShadow,
                spotColor = glass.elevationShadow
            )
            .clip(shape)
            .background(fillBrush)
            .border(width = 1.15.dp, brush = specularRimBrush, shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        // Layer 1: Fast optical frosted refraction gradient strictly behind foreground content
        // (avoids per-card RenderEffect FBO stalls on SurfaceSyncGroup)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (glass.isDark) 0.11f else 0.56f),
                            Color.Transparent,
                            (accentTint ?: Color.White).copy(alpha = if (glass.isDark) 0.09f else 0.08f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(720f, 540f)
                    )
                )
        )

        // Layer 2: Crisp overhead specular reflection hairline along top edge of glass
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            glass.glassTopSpecular,
                            Color.Transparent
                        )
                    )
                )
        )

        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface
        ) {
            Column(content = content)
        }
    }
}

/**
 * Convenience aliases ensuring every screen across LIFE OS inherits the full LifeOSGlassCard optics.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    accentTint: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) = LifeOSGlassCard(
    modifier = modifier,
    cornerRadius = cornerRadius,
    accentTint = accentTint,
    onClick = onClick,
    content = content
)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    accentTint: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) = LifeOSGlassCard(
    modifier = modifier,
    cornerRadius = cornerRadius,
    accentTint = accentTint,
    onClick = onClick,
    content = content
)

/**
 * Apple iOS Liquid Glass floating capsule navigation dock (`LifeOSFloatingNavigation`).
 * Respects WindowInsets.navigationBars via navigationBarsPadding() so it never overlaps gesture bars.
 */
@Composable
fun LifeOSFloatingNavigation(
    tabs: List<FloatingNavTab>,
    currentRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val glass = LocalLifeOsGlass.current
    val reducedMotion = LocalReducedMotion.current
    val capsuleShape = RoundedCornerShape(36.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = if (glass.isDark) 20.dp else 16.dp,
                    shape = capsuleShape,
                    ambientColor = glass.elevationShadow,
                    spotColor = glass.elevationShadow
                )
                .clip(capsuleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(glass.dockFillTop, glass.dockFillBottom)
                    )
                )
                .border(
                    width = 1.15.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            glass.glassBorderHighlight,
                            glass.glassBorderMid,
                            scheme.primary.copy(alpha = if (glass.isDark) 0.36f else 0.22f)
                        )
                    ),
                    shape = capsuleShape
                )
        ) {
            // Fast optical frosted top reflection sheen behind navigation icons
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (glass.isDark) 0.11f else 0.58f),
                                Color.Transparent
                            )
                        )
                    )
            )
            // Specular overhead rim line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, glass.glassTopSpecular, Color.Transparent)
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    val pillColor by animateColorAsState(
                        targetValue = if (selected) {
                            scheme.primary.copy(alpha = if (glass.isDark) 0.28f else 0.15f)
                        } else {
                            Color.Transparent
                        },
                        label = "dock_pill_color"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) {
                            if (glass.isDark) scheme.primary else scheme.onSurface
                        } else {
                            scheme.onSurfaceVariant.copy(alpha = 0.75f)
                        },
                        label = "dock_content_color"
                    )
                    val horizontalPad by animateDpAsState(
                        targetValue = if (selected) 15.dp else 10.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "dock_pad"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected && !reducedMotion) 1.09f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "dock_icon_scale"
                    )
                    val interaction = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(26.dp))
                            .background(pillColor)
                            .then(
                                if (selected) {
                                    Modifier.border(
                                        width = 0.85.dp,
                                        color = scheme.primary.copy(alpha = if (glass.isDark) 0.44f else 0.26f),
                                        shape = RoundedCornerShape(26.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = { onSelect(tab.route) }
                            )
                            .padding(horizontal = horizontalPad, vertical = 7.dp)
                            .testTag("nav_${tab.route}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                                tint = contentColor,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                            )
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = contentColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingGlassBottomBar(
    tabs: List<FloatingNavTab>,
    currentRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) = LifeOSFloatingNavigation(
    tabs = tabs,
    currentRoute = currentRoute,
    onSelect = onSelect,
    modifier = modifier
)

/**
 * Apple iOS-style frosted pill segmented control for crisp 2–4 option switching
 * (e.g., System / Light / Dark, Weekly / Monthly, Habits / Challenges).
 */
@Composable
fun AppleSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val glass = LocalLifeOsGlass.current
    val outerShape = RoundedCornerShape(18.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(outerShape)
            .background(
                if (glass.isDark) Color(0x99141724) else Color(0x88DCE3F2)
            )
            .border(0.8.dp, glass.glassBorderMid, outerShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val pillBg by animateColorAsState(
                targetValue = if (selected) {
                    if (glass.isDark) Color(0xE02A3046) else Color(0xFAFFFFFF)
                } else {
                    Color.Transparent
                },
                label = "seg_bg_$index"
            )
            val textColor by animateColorAsState(
                targetValue = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
                label = "seg_text_$index"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .then(
                        if (selected) {
                            Modifier.shadow(4.dp, RoundedCornerShape(14.dp), ambientColor = glass.elevationShadow)
                        } else {
                            Modifier
                        }
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .background(pillBg)
                    .then(
                        if (selected) {
                            Modifier.border(0.75.dp, glass.glassBorderHighlight, RoundedCornerShape(14.dp))
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Consistent Apple-style glossy squircle visual icons used across LIFE OS
 * so every module shares a single cohesive icon language without mixed clip-art.
 */
enum class LifeOsBadgeType(
    val icon: ImageVector,
    val startColor: Color,
    val endColor: Color,
    val label: String
) {
    WATER(Icons.Rounded.WaterDrop, Color(0xFF38BDF8), Color(0xFF007AFF), "Water"),
    STEPS(Icons.AutoMirrored.Rounded.DirectionsWalk, Color(0xFF34D399), Color(0xFF10B981), "Steps"),
    GOALS(Icons.Rounded.Flag, Color(0xFF818CF8), Color(0xFF5856D6), "Goals"),
    TASKS(Icons.Rounded.CheckCircle, Color(0xFF60A5FA), Color(0xFF2563EB), "Tasks"),
    LEARNING(Icons.Rounded.School, Color(0xFFA78BFA), Color(0xFF7C3AED), "Learning"),
    JAVA(Icons.Rounded.Code, Color(0xFFFB923C), Color(0xFFEA580C), "Java"),
    PYTHON(Icons.Rounded.Terminal, Color(0xFF38BDF8), Color(0xFF0284C7), "Python"),
    AI(Icons.Rounded.SmartToy, Color(0xFFC084FC), Color(0xFF6366F1), "AI Assistant"),
    FINANCE(Icons.Rounded.AccountBalanceWallet, Color(0xFF34C759), Color(0xFF059669), "Finance"),
    SAVINGS(Icons.Rounded.Savings, Color(0xFFFBBF24), Color(0xFFD97706), "Savings"),
    FOOD(Icons.Rounded.Restaurant, Color(0xFFFB7185), Color(0xFFE11D48), "Nutrition"),
    HEALTH(Icons.Rounded.Favorite, Color(0xFFFF6B8B), Color(0xFFFF2D55), "Health"),
    MOTIVATION(Icons.Rounded.AutoAwesome, Color(0xFFFBBF24), Color(0xFFFF9500), "Motivation"),
    MEMORIES(Icons.Rounded.PhotoLibrary, Color(0xFFF472B6), Color(0xFFDB2777), "Memories"),
    NOTES(Icons.Rounded.Description, Color(0xFFFACC15), Color(0xFFEAB308), "Notes"),
    NEWS(Icons.Rounded.Newspaper, Color(0xFF60A5FA), Color(0xFF007AFF), "Tech News"),
    VIDEO(Icons.Rounded.PlayCircle, Color(0xFFFF6961), Color(0xFFFF3B30), "Videos"),
    CALENDAR(Icons.Rounded.CalendarMonth, Color(0xFFFB7185), Color(0xFFFF3B30), "Calendar"),
    CAREER(Icons.Rounded.Work, Color(0xFF6366F1), Color(0xFF4338CA), "Career"),
    STREAK(Icons.Rounded.LocalFireDepartment, Color(0xFFFF9F0A), Color(0xFFFF3B30), "Streak"),
    FOCUS(Icons.Rounded.Timer, Color(0xFF8B5CF6), Color(0xFF5856D6), "Focus"),
    INSIGHTS(Icons.AutoMirrored.Rounded.TrendingUp, Color(0xFF2DD4BF), Color(0xFF0D9488), "Insights"),
    NOTIFICATIONS(Icons.Rounded.NotificationsActive, Color(0xFFFF6B6B), Color(0xFFFF3B30), "Notifications"),
    JOURNAL(Icons.Rounded.Book, Color(0xFFC084FC), Color(0xFF9333EA), "Journal"),
    ROUTINES(Icons.Rounded.Schedule, Color(0xFF38BDF8), Color(0xFF0284C7), "Routines"),
    CHALLENGE(Icons.Rounded.Bolt, Color(0xFFF59E0B), Color(0xFFD97706), "Challenges"),
    ACHIEVEMENTS(Icons.Rounded.EmojiEvents, Color(0xFFFACC15), Color(0xFFF59E0B), "Achievements"),
    SETTINGS(Icons.Rounded.Settings, Color(0xFF8E8E93), Color(0xFF636366), "Settings"),
    PROFILE(Icons.Rounded.Person, Color(0xFF0A84FF), Color(0xFF5856D6), "Profile")
}

@Composable
fun LifeOsBadgeIcon(
    type: LifeOsBadgeType,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(size * 0.29f)
    Box(
        modifier = modifier
            .size(size)
            .shadow(4.dp, shape, ambientColor = type.endColor.copy(alpha = 0.25f))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(type.startColor, type.endColor)
                )
            )
            .border(0.75.dp, Color.White.copy(alpha = 0.42f), shape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.30f),
                            Color.Transparent
                        )
                    )
                )
        )
        Icon(
            imageVector = type.icon,
            contentDescription = type.label,
            tint = Color.White,
            modifier = Modifier.size(size * 0.54f)
        )
    }
}

/**
 * Clean Apple-style section header with optional right action label.
 */
@Composable
fun AppleSectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = scheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Clean Apple-style sub-screen top bar with translucent glass back pill.
 */
@Composable
fun AppleSubScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    badgeType: LifeOsBadgeType? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val glass = LocalLifeOsGlass.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(42.dp)
                .shadow(4.dp, CircleShape, ambientColor = glass.elevationShadow)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(listOf(glass.glassFillTop, glass.glassFillBottom))
                )
                .border(1.dp, glass.glassBorderHighlight, CircleShape)
                .clickable(onClick = onBack)
                .testTag("subscreen_back_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = scheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
        if (badgeType != null) {
            LifeOsBadgeIcon(type = badgeType, size = 38.dp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = scheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * Apple Settings-inspired grouped row inside a LifeOSGlassCard.
 */
@Composable
fun AppleGroupedRow(
    title: String,
    subtitle: String? = null,
    badgeType: LifeOsBadgeType? = null,
    trailingText: String? = null,
    showChevron: Boolean = true,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val glass = LocalLifeOsGlass.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (badgeType != null) {
                LifeOsBadgeIcon(type = badgeType, size = 36.dp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!trailingText.isNullOrBlank()) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant
                )
            }
            trailingContent?.invoke()
            if (showChevron && onClick != null && trailingContent == null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = if (badgeType != null) 64.dp else 16.dp),
                thickness = 0.6.dp,
                color = glass.separator
            )
        }
    }
}

/**
 * Refined capsule pill badge for metrics, streaks, categories, and status indicators.
 */
@Composable
fun ApplePillBadge(
    text: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val glass = LocalLifeOsGlass.current
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = if (glass.isDark) 0.24f else 0.14f))
            .border(0.75.dp, tint.copy(alpha = 0.34f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = tint
        )
    }
}

/**
 * Circular profile avatar supporting user-selected local image or clean illustrated initials fallback.
 */
@Composable
fun UserAvatarCircle(
    avatarPath: String,
    displayName: String,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val fileExists = remember(avatarPath) {
        avatarPath.isNotBlank() && File(avatarPath).exists()
    }
    val initials = remember(displayName) {
        displayName.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "LO" }
    }

    Box(
        modifier = modifier
            .size(size)
            .shadow(6.dp, CircleShape, ambientColor = scheme.primary.copy(alpha = 0.35f))
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(scheme.primary, scheme.secondary, scheme.tertiary)
                )
            )
            .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (fileExists) {
            AsyncImage(
                model = File(avatarPath),
                contentDescription = "Profile photo of $displayName",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else {
            Text(
                text = initials,
                style = if (size >= 72.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
