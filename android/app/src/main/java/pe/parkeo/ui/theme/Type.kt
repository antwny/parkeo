/*
 * PARKeo · Android
 * Archivo: `android/app/src/main/java/pe/parkeo/ui/theme/Type.kt`
 * Capa: sistema de diseño (tipografía).
 * Propósito: definir la escala tipográfica de PARKeo como un `Typography` de Material 3 y un
 *   estilo monoespaciado para datos técnicos. Una escala central evita que cada pantalla elija
 *   tamaños y pesos improvisados.
 * Conceptos clave: `Typography`, `TextStyle`, `FontFamily`, `FontWeight`, `sp` (píxeles
 *   escalables según la preferencia de tamaño de texto del usuario), altura de línea,
 *   interletraje y jerarquía visual.
 * Flujo de uso: `Theme.kt` pasa esta escala a `MaterialTheme.typography`; los componentes
 *   consultan `MaterialTheme.typography.titleMedium`, `bodySmall`, etc. Las pantallas deben
 *   preferir esos roles antes que fijar tamaños locales.
 * Relación con pantallas/tema: los componentes usan los roles `display*` para héroes,
 *   `headline*`/`title*` para encabezados, `body*` para contenido y `label*` para botones,
 *   badges y metadatos. `MonospaceTechnical` se usa para códigos, placas y cifras alineadas.
 * Guía: `docs/GUIA_MAESTRA_ESTUDIANTE.md`.
 */
package pe.parkeo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Escala tipográfica completa de PARKeo. Cada rol declara familia, peso, tamaño (`sp`), altura
 * de línea (`lineHeight`, también en `sp`) e interletraje (`letterSpacing`).
 *
 * La familia es `FontFamily.Default` (la fuente del sistema) para no añadir binarios de fuentes
 * al APK y respetar la legibilidad de cada versión de Android. Los títulos usan pesos fuertes
 * (`Bold`/`ExtraBold`) y un interletraje ligeramente negativo para dar una apariencia compacta;
 * los textos pequeños usan interletraje positivo para mejorar la lectura.
 *
 * Esta escala se entrega a `MaterialTheme.typography`; no es una lista de estilos para usar de
 * forma aislada, sino el contrato tipográfico que comparten todas las pantallas.
 */
val Typography = Typography(
    // Héroe de pantalla: el texto más grande y pesado. Se usa con moderación.
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp
    ),
    // Título de bienvenida o cifra destacada de una pantalla principal.
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    // Encabezado principal de una sección o pantalla.
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp
    ),
    // Encabezado de sección de menor jerarquía que `headlineLarge`.
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    // Título de tarjeta o de bloque destacado.
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),
    // Título habitual de tarjetas, filas y encabezados de app bar.
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp
    ),
    // Título compacto para subtítulos o encabezados de listas densas.
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    // Texto de lectura principal: párrafos y contenido de formularios.
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    // Texto secundario de lectura, descripciones y celdas de listas.
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp
    ),
    // Texto auxiliar, metadatos y mensajes breves.
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    // Etiqueta de botones y acciones principales. El peso fuerte compensa el tamaño pequeño.
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.3.sp
    ),
    // Etiqueta de controles, campos y navegación secundaria.
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    // Etiqueta mínima de badges y metadatos; el interletraje amplio mantiene la legibilidad.
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp
    )
)

/**
 * Estilo monoespaciado para datos que deben alinearse o leerse carácter a carácter: códigos de
 * reserva (por ejemplo `PKO-12345`), placas vehiculares, números de espacio y precios.
 *
 * Solo fija familia, peso e interletraje; no impone tamaño ni altura de línea, de modo que cada
 * pantalla pueda combinarlo con un `fontSize` adecuado al contexto. La familia monoespaciada
 * evita que los dígitos “bailen” al actualizarse o compararse.
 */
val MonospaceTechnical = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.sp
)

/**
 * Acceso de conveniencia para escribir `MaterialTheme.typography.MonospaceTechnical` sin perder
 * el estilo técnico global. La implementación delega en el valor de nivel superior
 * `pe.parkeo.ui.theme.MonospaceTechnical`; no crea una copia ni modifica la escala Material.
 */
val Typography.MonospaceTechnical: TextStyle
    get() = pe.parkeo.ui.theme.MonospaceTechnical
