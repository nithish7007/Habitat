package com.example.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.ThemeMode

@Immutable
data class LifeOsGlassTokens(
    val isDark: Boolean,
    val glassFillTop: Color,
    val glassFillBottom: Color,
    val glassBorderHighlight: Color,
    val glassBorderMid: Color,
    val glassBorderShadow: Color,
    val glassTopSpecular: Color,
    val dockFillTop: Color,
    val dockFillBottom: Color,
    val ambientGlowPrimary: Color,
    val ambientGlowSecondary: Color,
    val ambientGlowTertiary: Color,
    val ambientCanvasTop: Color,
    val ambientCanvasBottom: Color,
    val separator: Color,
    val sheetScrim: Color,
    val elevationShadow: Color
)

private val LightGlassTokens = LifeOsGlassTokens(
    isDark = false,
    // Genuine frosted translucent white glass so the atmospheric background subtly refracts through
    glassFillTop = Color(0xC9FFFFFF),
    glassFillBottom = Color(0xA6F4F7FF),
    glassBorderHighlight = Color(0xFAFFFFFF),
    glassBorderMid = Color(0x9ED4DCF0),
    glassBorderShadow = Color(0x478A98B8),
    glassTopSpecular = Color(0xE6FFFFFF),
    dockFillTop = Color(0xD8FFFFFF),
    dockFillBottom = Color(0xBFF0F4FC),
    ambientGlowPrimary = Color(0x40007AFF),
    ambientGlowSecondary = Color(0x335856D6),
    ambientGlowTertiary = Color(0x2B34C759),
    ambientCanvasTop = Color(0xFFEAF0FA),
    ambientCanvasBottom = Color(0xFFF4F5FA),
    separator = Color(0xFFDAE0EC),
    sheetScrim = Color(0x52000000),
    elevationShadow = Color(0x1E0D1B3E)
)

private val DarkGlassTokens = LifeOsGlassTokens(
    isDark = true,
    // Smoked obsidian Liquid Glass with crisp specular rim contrast
    glassFillTop = Color(0xBF24293B),
    glassFillBottom = Color(0xA6131622),
    glassBorderHighlight = Color(0x5EFFFFFF),
    glassBorderMid = Color(0x28FFFFFF),
    glassBorderShadow = Color(0x260A84FF),
    glassTopSpecular = Color(0x42FFFFFF),
    dockFillTop = Color(0xD1222638),
    dockFillBottom = Color(0xBD12141E),
    ambientGlowPrimary = Color(0x420A84FF),
    ambientGlowSecondary = Color(0x387D7AFF),
    ambientGlowTertiary = Color(0x2930D158),
    ambientCanvasTop = Color(0xFF070912),
    ambientCanvasBottom = Color(0xFF05060A),
    separator = Color(0x30FFFFFF),
    sheetScrim = Color(0x88000000),
    elevationShadow = Color(0x66000000)
)

val LocalLifeOsGlass = staticCompositionLocalOf { LightGlassTokens }
val LocalReducedMotion = staticCompositionLocalOf { false }

val LifeOSDarkColorScheme = darkColorScheme(
    primary = SkyBluePrimary,
    onPrimary = Color.White,
    primaryContainer = AppleDarkBlueContainer,
    onPrimaryContainer = SkyBlueLight,
    secondary = AppleDarkIndigo,
    onSecondary = Color.White,
    secondaryContainer = AppleDarkIndigoContainer,
    onSecondaryContainer = Color(0xFFE6E5FF),
    tertiary = AppleDarkGreen,
    onTertiary = Color.White,
    tertiaryContainer = AppleDarkGreenContainer,
    onTertiaryContainer = Color(0xFFC2F7D0),
    error = AppleDarkRed,
    onError = Color.White,
    errorContainer = AppleDarkRedContainer,
    onErrorContainer = Color(0xFFFFD8D6),
    background = AppleDarkCanvas,
    onBackground = AppleDarkPrimaryInk,
    surface = AppleDarkGlassSurface,
    onSurface = AppleDarkPrimaryInk,
    surfaceVariant = AppleDarkGlassElevated,
    onSurfaceVariant = SkyBlueLight,
    surfaceContainerLowest = AppleDarkCanvas,
    surfaceContainerLow = AppleDarkBackgroundSecondary,
    surfaceContainer = AppleDarkGlassSurface,
    surfaceContainerHigh = AppleDarkGlassElevated,
    surfaceContainerHighest = AppleDarkGlassHigh,
    outline = AppleDarkSeparator,
    outlineVariant = AppleDarkSeparatorSubtle
)

val LifeOSLightColorScheme = lightColorScheme(
    primary = SkyBluePrimary,
    onPrimary = Color.White,
    primaryContainer = AppleLightBlueContainer,
    onPrimaryContainer = SkyBlueDeep,
    secondary = AppleLightIndigo,
    onSecondary = Color.White,
    secondaryContainer = AppleLightIndigoContainer,
    onSecondaryContainer = SkyBlueDeep,
    tertiary = AppleLightGreen,
    onTertiary = Color.White,
    tertiaryContainer = AppleLightGreenContainer,
    onTertiaryContainer = Color(0xFF0B461D),
    error = AppleLightRed,
    onError = Color.White,
    errorContainer = AppleLightRedContainer,
    onErrorContainer = Color(0xFF7D110A),
    background = Color(0xFFEDF1F8),
    onBackground = SkyBlueDeep,
    surface = AppleLightSurface,
    onSurface = SkyBlueDeep,
    surfaceVariant = AppleLightGlassLow,
    onSurfaceVariant = SkyBluePrimary,
    surfaceContainerLowest = AppleLightSurface,
    surfaceContainerLow = Color(0xFFF0F3FA),
    surfaceContainer = AppleLightGlassLow,
    surfaceContainerHigh = AppleLightGlassHigh,
    surfaceContainerHighest = Color(0xFFE0E4F0),
    outline = AppleLightSeparator,
    outlineVariant = AppleLightSeparatorSubtle
)

val LifeOsShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun LifeOSLightTheme(content: @Composable () -> Unit) {
    LifeOsThemeInternal(useDark = false, content = content)
}

@Composable
fun LifeOSDarkTheme(content: @Composable () -> Unit) {
    LifeOsThemeInternal(useDark = true, content = content)
}

@Composable
fun LifeOsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    if (useDark) {
        LifeOSDarkTheme(content = content)
    } else {
        LifeOSLightTheme(content = content)
    }
}

@Composable
private fun LifeOsThemeInternal(
    useDark: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }

    val glassTokens = if (useDark) DarkGlassTokens else LightGlassTokens
    val colorScheme = if (useDark) LifeOSDarkColorScheme else LifeOSLightColorScheme

    CompositionLocalProvider(
        LocalLifeOsGlass provides glassTokens,
        LocalReducedMotion provides reducedMotion,
        LocalContentColor provides colorScheme.onBackground
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LifeOSTypography,
            shapes = LifeOsShapes,
            content = content
        )
    }
}
