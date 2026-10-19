/*
 * PARKeo · Android
 * Archivo: `android/app/src/main/java/pe/parkeo/ui/theme/Theme.kt`
 * Capa: sistema de diseño (tema y propagación de tokens).
 * Propósito: construir el tema claro/oscuro de PARKeo, combinar `ColorScheme`, `Typography` y
 *   `Shapes` de Material 3 con una capa de colores extendidos propia, y exponer un punto único
 *   de entrada (`ParkeoTheme`) para toda la aplicación.
 * Conceptos clave: `MaterialTheme`, `ColorScheme`, `Typography`, `Shapes`, `CompositionLocal`,
 *   `staticCompositionLocalOf`, tokens extendidos, tema claro/oscuro, `SideEffect`, barra de
 *   estado, edge-to-edge y composables que reciben `content` como slot.
 * Flujo de uso: `MainActivity` llama a `ParkeoTheme { ParkeoApp() }`; el tema elige el esquema
 *   según `isSystemInDarkTheme()` o el parámetro `darkTheme`, publica `ParkeoExtendedColors` y
 *   envuelve el contenido en `MaterialTheme`. Las pantallas leen `MaterialTheme.colorScheme`,
 *   `MaterialTheme.typography`, `MaterialTheme.shapes` o `ParkeoTheme.colors`.
 * Relación con pantallas/tema: los componentes de `ui/components` dependen de los colores
 *   extendidos para acento, superficies, bordes y textos; las pantallas no deberían crear su
 *   propio `MaterialTheme`. El tema no incluye color dinámico: la identidad lima/obsidiana se
 *   mantiene fija en todos los dispositivos.
 * Guía: `docs/GUIA_MAESTRA_ESTUDIANTE.md`.
 */
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
/**
 * Capa de tokens extendidos que Material 3 no cubre con sus roles estándar. Agrupa el acento de
 * marca, sus variantes translúcidas, la escala de superficies (0 a 3), bordes, textos y colores
 * de señal del dominio. Se marca `@Immutable` porque todos sus campos son `val` y `Color` es
 * inmutable: Compose puede saltarse recomposiciones si la instancia no cambia.
 *
 * `background` se deriva de `surface0`, y las señales tienen valores por defecto de la paleta;
 * `isDark` permite que un componente ajuste decisiones de contraste sin consultar el sistema.
 *
 * @property accent Color de acento adaptado al tema (lima puro en oscuro, lima atenuado en claro).
 * @property onAccent Color de texto/icono que se coloca sobre `accent`.
 * @property accentGlow Acento translúcido para halos y fondos suaves.
 * @property accentContainer Acento translúcido para contenedores y estados seleccionados.
 * @property surface0 Superficie base de la pantalla o scaffold.
 * @property surface1 Superficie de tarjetas y hojas.
 * @property surface2 Superficie de campos y contenedores internos.
 * @property surface3 Superficie de estados activos, chips y diálogos.
 * @property border Borde principal de tarjetas, campos y controles.
 * @property borderSubtle Borde secundario para separadores.
 * @property textPrimary Texto de máxima jerarquía.
 * @property textSecondary Texto de jerarquía media.
 * @property textTertiary Texto auxiliar, pistas y placeholders.
 * @property signalRed Señal de error, ocupado o acción destructiva.
 * @property signalGreen Señal de disponibilidad o éxito.
 * @property signalAmber Señal de reserva, pendiente o advertencia.
 * @property signalCyan Señal tecnológica secundaria.
 * @property background Fondo general; por defecto coincide con `surface0`.
 * @property isDark Indica si la instancia corresponde al tema oscuro.
 */
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

/**
 * `CompositionLocal` que transporta `ParkeoExtendedColors` por el árbol de composición. Se usa
 * `staticCompositionLocalOf` porque el tema cambia muy pocas veces: cuando el valor cambia,
 * Compose recompondrá todo el contenido que lo lee, lo cual es aceptable y evita el coste de
 * rastrear cada lectura. El valor por defecto es el esquema oscuro para que un `@Preview` o un
 * composable aislado tenga tokens válidos incluso sin `ParkeoTheme`.
 */
val LocalParkeoColors = staticCompositionLocalOf {
    DarkParkeoExtendedColors
}

/**
 * Tokens extendidos del tema oscuro. Usa el lima puro como acento y la escala obsidiana de
 * superficies; el texto primario es blanco cálido y los estados conservan sus colores saturados.
 */
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

/**
 * Tokens extendidos del tema claro. Usa el lima atenuado para conservar contraste, superficies
 * titanio/blancas y texto negro azulado. Las señales de estado se mantienen iguales para que el
 * significado no cambie entre temas.
 */
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

/**
 * `ColorScheme` de Material 3 para el tema oscuro. Traduce la paleta PARKeo a los roles que
 * esperan los controles Material: `primary` es el lima, `background`/`surface` son las capas
 * obsidiana, `outline` es el borde y `error` el rojo de estado. Los componentes propios leen
 * además `ParkeoTheme.colors` para matices que Material no modela.
 */
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

/**
 * `ColorScheme` de Material 3 para el tema claro. Aquí `primary` es casi negro para que los
 * controles Material tengan contraste; el lima vive en `primaryContainer` y en
 * `ParkeoTheme.colors.accent`. Esta distinción es deliberada: no debe asumirse que
 * `MaterialTheme.colorScheme.primary` sea siempre el lima de marca.
 */
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

/**
 * Punto de acceso no composable a los tokens extendidos del tema actual. Se usa como
 * `ParkeoTheme.colors` dentro de cualquier `@Composable`: el getter consulta el
 * `CompositionLocal` y devuelve la instancia publicada por la función `ParkeoTheme`.
 *
 * No confundir este objeto con la función homónima `ParkeoTheme(...)`: el objeto expone tokens,
 * la función instala el tema. Kotlin resuelve cada uso por su forma (acceso a propiedad frente a
 * llamada con lambda).
 */
object ParkeoTheme {
    /**
     * Tokens extendidos vigentes (claro u oscuro). Es una propiedad composable porque lee
     * `LocalParkeoColors`; debe invocarse dentro de una composición, no desde lógica de negocio.
     */
    val colors: ParkeoExtendedColors
        @Composable
        get() = LocalParkeoColors.current
}

/**
 * Instala el tema de PARKeo y envuelve `content` en `MaterialTheme`.
 *
 * Elige el `ColorScheme` y los colores extendidos según [darkTheme]; por defecto sigue la
 * preferencia del sistema mediante `isSystemInDarkTheme()`. No ofrece color dinámico
 * (`dynamicColor`): la identidad visual de la app no depende del fondo de pantalla del usuario.
 *
 * Además, fuera del modo de edición de Android Studio, sincroniza el color de la barra de
 * estado con el fondo del esquema y ajusta el contraste de sus iconos. La actividad ya habilita
 * edge-to-edge en `MainActivity`; esta función solo tiñe la barra, no reorganiza los insets.
 *
 * @param darkTheme Indica si se usa el esquema oscuro. Se puede forzar en pruebas o previews.
 * @param content Contenido composable que recibe el tema; actúa como slot y se ejecuta dentro de
 *   `CompositionLocalProvider` y `MaterialTheme`.
 */
@Composable
fun ParkeoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Selección coherente del esquema Material y de la capa extendida.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkParkeoExtendedColors else LightParkeoExtendedColors

    // Barra de estado: se tiñe con el fondo y se ajustan los iconos para mantener contraste.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    // Se publican primero los tokens extendidos y luego se instala MaterialTheme para que
    // `content` pueda leer ambas fuentes de estilo.
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
/**
 * Color semántico de disponibilidad. Atajo de `ParkeoGreen500` para badges y tarjetas que
 * representan un espacio libre. No varía entre tema claro y oscuro.
 */
val MaterialTheme.availableColor: Color get() = ParkeoGreen500
/**
 * Color semántico de ocupación o error. Atajo de `ParkeoRed500`; no varía entre temas.
 */
val MaterialTheme.occupiedColor: Color get() = ParkeoRed500
/**
 * Color semántico de reserva o advertencia. Atajo de `ParkeoAmber500`; no varía entre temas.
 */
val MaterialTheme.reservedColor: Color get() = ParkeoAmber500
/**
 * Color semántico de cierre, mantenimiento o deshabilitado. Atajo de `ParkeoGray500`.
 */
val MaterialTheme.closedColor: Color get() = ParkeoGray500
/**
 * Acceso directo al lima de marca sin pasar por `ParkeoTheme.colors`. A diferencia de
 * `ParkeoTheme.colors.accent`, devuelve siempre `ParkeoLime`, incluso en tema claro; usarlo solo
 * cuando se busque el lima puro y el contraste esté resuelto por el contenedor.
 */
val MaterialTheme.limeAccent: Color get() = ParkeoLime
