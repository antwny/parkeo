package pe.parkeo.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ════════════════════════════════════════════════════════════════════════════════
// Parkeo EXTENDED DESIGN SYSTEM COLORS
// ════════════════════════════════════════════════════════════════════════════════
@Immutable
data class ParkeoExtendedColors(
    val accent: Color,
    val onAccent: Color,
    val accentGlow: Color,
    val accentContainer: Color,
    val surface0: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val border: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val signalRed: Color = ParkeoRed500,
    val signalGreen: Color = ParkeoGreen500,
    val signalAmber: Color = ParkeoAmber500,
    val signalCyan: Color = ParkeoCyan500,
    val background: Color = surface0,
    val isDark: Boolean
)

val LocalParkeoColors = staticCompositionLocalOf {
    DarkParkeoExtendedColors
}

private val DarkParkeoExtendedColors = ParkeoExtendedColors(
    accent = ParkeoLime,
    onAccent = OnParkeoLime,
    accentGlow = ParkeoLimeGlow,
    accentContainer = ParkeoLimeContainer,
    surface0 = ParkeoObsidian,
    surface1 = ParkeoDarkSurface1,
    surface2 = ParkeoDarkSurface2,
    surface3 = ParkeoDarkSurface3,
    border = ParkeoDarkBorder,
    borderSubtle = ParkeoDarkBorderSubtle,
    textPrimary = ParkeoTextWhite,
    textSecondary = ParkeoTextGrayLight,
    textTertiary = ParkeoTextGrayMuted,
    signalRed = ParkeoRed500,
    signalGreen = ParkeoGreen500,
    signalAmber = ParkeoAmber500,
    signalCyan = ParkeoCyan500,
    background = ParkeoObsidian,
    isDark = true
)

private val LightParkeoExtendedColors = ParkeoExtendedColors(
    accent = ParkeoLimeDim,
    onAccent = OnParkeoLime,
    accentGlow = ParkeoLimeGlow,
    accentContainer = ParkeoLimeContainer,
    surface0 = ParkeoTitaniumBg,
    surface1 = ParkeoLightSurface1,
    surface2 = ParkeoLightSurface2,
    surface3 = ParkeoLightSurface3,
    border = ParkeoLightBorder,
    borderSubtle = ParkeoLightBorderSubtle,
    textPrimary = ParkeoTextBlack,
    textSecondary = ParkeoTextSlate,
    textTertiary = ParkeoTextMutedLight,
    signalRed = ParkeoRed500,
    signalGreen = ParkeoGreen500,
    signalAmber = ParkeoAmber500,
    signalCyan = ParkeoCyan500,
    background = ParkeoTitaniumBg,
    isDark = false
)

private val DarkColorScheme = darkColorScheme(
    primary = ParkeoLime,
    onPrimary = OnParkeoLime,
    primaryContainer = ParkeoLimeContainer,
    onPrimaryContainer = ParkeoLimeBright,
    secondary = ParkeoCyan500,
    onSecondary = OnParkeoLime,
    secondaryContainer = ParkeoDarkSurface3,
    onSecondaryContainer = ParkeoTextWhite,
    background = ParkeoObsidian,
    onBackground = ParkeoTextWhite,
    surface = ParkeoDarkSurface1,
    onSurface = ParkeoTextWhite,
    surfaceVariant = ParkeoDarkSurface2,
    onSurfaceVariant = ParkeoTextGrayLight,
    outline = ParkeoDarkBorder,
    outlineVariant = ParkeoDarkBorderSubtle,
    error = ParkeoRed500,
    onError = White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F1115), // Bold obsidian black in light mode
    onPrimary = White,
    primaryContainer = ParkeoLime,
    onPrimaryContainer = OnParkeoLime,
    secondary = Color(0xFF1E293B),
    onSecondary = White,
    secondaryContainer = ParkeoLightSurface3,
    onSecondaryContainer = Color(0xFF0F1115),
    background = ParkeoTitaniumBg,
    onBackground = ParkeoTextBlack,
    surface = ParkeoLightSurface1,
    onSurface = ParkeoTextBlack,
    surfaceVariant = ParkeoLightSurface2,
    onSurfaceVariant = ParkeoTextSlate,
    outline = ParkeoLightBorder,
    outlineVariant = ParkeoLightBorderSubtle,
    error = ParkeoRed500,
    onError = White
)

object ParkeoTheme {
    val colors: ParkeoExtendedColors
        @Composable
        get() = LocalParkeoColors.current
}

@Composable
fun ParkeoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkParkeoExtendedColors else LightParkeoExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalParkeoColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════════
// SEMANTIC CONVENIENCE ACCESSORS
// ════════════════════════════════════════════════════════════════════════════════
val MaterialTheme.availableColor: Color get() = ParkeoGreen500
val MaterialTheme.occupiedColor: Color get() = ParkeoRed500
val MaterialTheme.reservedColor: Color get() = ParkeoAmber500
val MaterialTheme.closedColor: Color get() = ParkeoGray500
val MaterialTheme.limeAccent: Color get() = ParkeoLime
