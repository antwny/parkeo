/*
 * PARKeo · Android
 * Archivo: `android/app/src/main/java/pe/parkeo/ui/components/ParkeoButton.kt`
 * Capa: componentes reutilizables de interfaz.
 * Propósito: ofrecer un botón único con las variantes visuales de PARKeo (primario, secundario,
 *   fantasma y destructivo), iconos opcionales, estado de carga y tamaño compacto, sin que cada
 *   pantalla repita colores, formas y alturas.
 * Conceptos clave: composable stateless, slot de iconos, `Modifier`, `Button` de Material 3,
 *   `ParkeoButtonStyle`, tokens de tema (`ParkeoTheme.colors`, `Dimens`), accesibilidad táctil y
 *   contenido decorativo.
 * Flujo de uso: la pantalla pasa `text`, `onClick` y, si hace falta, `style`, iconos, `enabled` o
 *   `isLoading`; el botón no guarda estado propio ni conoce la lógica de negocio. El callback
 *   eleva el evento y la pantalla decide qué hacer.
 * Relación con pantallas/tema: se usa en formularios, estados vacíos/error y acciones de
 *   reservas. Consume el acento, las superficies y los textos de `ParkeoTheme` para mantener la
 *   identidad en claro/oscuro.
 * Guía: `docs/GUIA_MAESTRA_ESTUDIANTE.md`.
 */
package pe.parkeo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.theme.*

/**
 * Variantes visuales de `ParkeoButton`. La variante no cambia el comportamiento del botón, solo
 * su jerarquía y su color; el `onClick` sigue siendo responsabilidad del llamador.
 *
 * - [Primary]: acción principal. Fondo lima y contenido casi negro para máximo contraste.
 * - [Secondary]: acción alternativa. Superficie elevada con borde nítido.
 * - [Ghost]: acción terciaria. Fondo transparente y contenido de texto; no dibuja borde.
 * - [Destructive]: acción irreversible o peligrosa. Fondo rojo y contenido blanco.
 */
enum class ParkeoButtonStyle {
    Primary,      // Lima de marca con texto obsidiana: máxima prioridad.
    Secondary,    // Superficie elevada y borde nítido: alternativa segura.
    Ghost,        // Fondo transparente: acción terciaria y discreta.
    Destructive   // Rojo de error: acciones irreversibles o peligrosas.
}

/**
 * Botón reutilizable de PARKeo. Renderiza un `Button` de Material 3 de ancho completo, con
 * altura estándar o compacta, color según [style] y contenido centrado. Es un composable
 * stateless: recibe el texto y los callbacks; no recuerda clics ni estado de carga. El estado de
 * carga lo aporta la pantalla mediante [isLoading].
 *
 * Variantes visuales:
 * - `Primary`: contenedor lima (`extended.accent`) y contenido `onAccent`.
 * - `Secondary`: contenedor `surface2`, texto primario y borde `border`.
 * - `Ghost`: contenedor transparente y texto primario.
 * - `Destructive`: contenedor rojo y contenido blanco.
 *
 * Cuando [enabled] es `false` o [isLoading] es `true`, el botón queda deshabilitado y usa los
 * colores deshabilitados de Material. Durante la carga se oculta el texto/iconos y se muestra un
 * indicador circular; por eso, si la pantalla necesita anunciar el progreso a accesibilidad, debe
 * complementarlo con otro texto o semántica.
 *
 * @param text Etiqueta visible del botón. También es el nombre accesible principal de la acción.
 * @param onClick Callback que se ejecuta al pulsar. Se eleva a la pantalla; el botón no navega ni
 *   llama repositorios por sí mismo.
 * @param modifier `Modifier` del llamador. Se aplica antes de que el componente añada ancho
 *   completo y altura; puede aportar padding externo o alineación, y sus restricciones siguen
 *   influyendo en el tamaño final.
 * @param style Variante visual; por defecto [ParkeoButtonStyle.Primary].
 * @param leadingIcon Icono opcional antes del texto. Se marca decorativo (`contentDescription`
 *   nulo) porque el texto ya nombra la acción.
 * @param trailingIcon Icono opcional después del texto, también decorativo.
 * @param enabled Habilita o deshabilita la interacción. Un botón deshabilitado no dispara
 *   [onClick].
 * @param isLoading Sustituye el contenido por un indicador de progreso y deshabilita el botón
 *   mientras dura la operación.
 * @param compact Usa la altura pequeña (`Dimens.buttonHeightSmall`) y una etiqueta más ajustada.
 */
@Composable
fun ParkeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ParkeoButtonStyle = ParkeoButtonStyle.Primary,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    compact: Boolean = false
) {
    val extended = ParkeoTheme.colors

    // Paleta del contenedor según la variante. En Primary se evita el lima cuando está
    // deshabilitado para que no parezca una acción disponible.
    val containerColor = when (style) {
        ParkeoButtonStyle.Primary -> if (enabled) extended.accent else extended.surface3
        ParkeoButtonStyle.Secondary -> extended.surface2
        ParkeoButtonStyle.Ghost -> Color.Transparent
        ParkeoButtonStyle.Destructive -> ParkeoRed500
    }

    // Color del texto e iconos; debe contrastar con el contenedor elegido.
    val contentColor = when (style) {
        ParkeoButtonStyle.Primary -> if (enabled) extended.onAccent else extended.textTertiary
        ParkeoButtonStyle.Secondary -> extended.textPrimary
        ParkeoButtonStyle.Ghost -> extended.textPrimary
        ParkeoButtonStyle.Destructive -> White
    }

    // Solo la variante secundaria dibuja un borde; las demás se apoyan en el color de fondo.
    val border = when (style) {
        ParkeoButtonStyle.Primary -> null
        ParkeoButtonStyle.Secondary -> BorderStroke(Dimens.borderThin, extended.border)
        ParkeoButtonStyle.Ghost -> null
        ParkeoButtonStyle.Destructive -> null
    }

    // Botón Material con ancho completo, sin elevación y con forma propia de PARKeo.
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) Dimens.buttonHeightSmall else Dimens.buttonHeight),
        enabled = enabled && !isLoading,
        shape = ParkeoButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = extended.surface2,
            disabledContentColor = extended.textTertiary
        ),
        border = border,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            hoveredElevation = 0.dp
        )
    ) {
        // Contenido centrado: indicador de carga o fila de icono + texto + icono.
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                // Estado de carga: reemplaza la etiqueta mientras se ejecuta la acción.
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Icono inicial decorativo, alineado con el texto.
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.iconSizeMedium),
                            tint = contentColor
                        )
                        Spacer(Modifier.width(Dimens.space8))
                    }
                    // Etiqueta principal de la acción.
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        fontSize = if (compact) 13.sp else 14.5.sp
                    )
                    // Icono final decorativo, útil para flechas o indicaciones de avance.
                    if (trailingIcon != null) {
                        Spacer(Modifier.width(Dimens.space8))
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.iconSizeMedium),
                            tint = contentColor
                        )
                    }
                }
            }
        }
    }
}
