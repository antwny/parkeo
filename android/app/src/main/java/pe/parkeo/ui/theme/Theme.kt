package pe.parkeo.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = ParkeoBlue700,
    onPrimary = White,
    primaryContainer = ParkeoBlue100,
    onPrimaryContainer = ParkeoBlue900,
    secondary = ParkeoCyan500,
    onSecondary = White,
    secondaryContainer = ParkeoCyan100,
    onSecondaryContainer = ParkeoBlue900,
    background = SurfaceLight,
    onBackground = ParkeoGray900,
    surface = White,
    onSurface = ParkeoGray900,
    surfaceVariant = ParkeoBlue50,
    onSurfaceVariant = ParkeoGray800,
    error = ParkeoRed500,
    onError = White,
    outline = ParkeoGray500
)

private val DarkColorScheme = darkColorScheme(
    primary = ParkeoBlue500,
    onPrimary = ParkeoBlue900,
    primaryContainer = ParkeoBlue800,
    onPrimaryContainer = ParkeoBlue100,
    secondary = ParkeoCyan400,
    onSecondary = ParkeoBlue900,
    secondaryContainer = ParkeoBlue800,
    onSecondaryContainer = ParkeoCyan100,
    background = ParkeoGray900,
    onBackground = White,
    surface = ParkeoGray800,
    onSurface = White,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = Color(0xFFF87171),
    onError = ParkeoGray900,
    outline = ParkeoGray500
)

@Composable
fun ParkeoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

val MaterialTheme.availableColor get() = ParkeoGreen500
val MaterialTheme.occupiedColor get() = ParkeoRed500
val MaterialTheme.reservedColor get() = ParkeoAmber500
val MaterialTheme.closedColor get() = ParkeoGray500
