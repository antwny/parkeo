/*
 * PARKeo · Android
 * Archivo: `android/app/src/main/java/pe/parkeo/ui/components/ParkeoCard.kt`
 * Capa: componentes reutilizables de interfaz.
 * Propósito: proporcionar el contenedor base de tarjetas de PARKeo: superficie, forma, borde,
 *   padding opcional, borde de acento y franja lateral de color, con o sin interacción.
 * Conceptos clave: composable stateless, slot de contenido (`ColumnScope`), `Modifier`,
 *   `Surface`, `BorderStroke`, `Shape`, elevación visual por capas y accesibilidad del clic.
 * Flujo de uso: la pantalla envuelve su contenido en `ParkeoCard`. Si pasa `onClick`, la tarjeta
 *   se vuelve interactiva; si lo omite, es puramente informativa. El contenido se declara como
 *   lambda con acceso al alcance de `Column`.
 * Relación con pantallas/tema: es la base de `ParkingCard` y de las tarjetas de vehículos,
 *   reservas y formularios. Usa `ParkeoTheme.colors` para superficie/borde y `ParkeoCardShape`
 *   para mantener el radio común.
 * Guía: `docs/GUIA_MAESTRA_ESTUDIANTE.md`.
 */
package pe.parkeo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.parkeo.ui.theme.*

/**
 * Tarjeta contenedora de PARKeo. No conoce el dominio: recibe un slot de contenido y decide
 * únicamente la presentación. Es stateless, ya que todo su estado (color, borde, clic y
 * contenido) llega como parámetros.
 *
 * Si [onClick] no es nulo, se usa la sobrecarga interactiva de `Surface`: la tarjeta obtiene
 * semántica de clic y el indicador táctil de Material. Si es nulo, se usa una `Surface` pasiva.
 * En ambos casos la forma recorta el fondo y el borde.
 *
 * La franja de acento, cuando se indica [accentStripeColor], ocupa 4 dp a la izquierda y se
 * estira a la altura del contenido; el padding se aplica dentro de la columna de contenido, no
 * a la franja. Sin franja, el contenido ocupa todo el ancho disponible.
 *
 * @param modifier `Modifier` externo aplicado a la `Surface` (tamaño, padding, animaciones).
 * @param onClick Callback de clic. Si es `null`, la tarjeta no es interactiva.
 * @param shape Forma de la tarjeta; por defecto [ParkeoCardShape] (14 dp).
 * @param containerColor Color de fondo; por defecto `surface1` del tema actual.
 * @param borderColor Color del borde; por defecto `border` del tema.
 * @param borderWidth Grosor del borde en `dp`; por defecto `Dimens.borderThin`.
 * @param accentBorder Si es `true`, sustituye el borde por el acento del tema al 50 % de alfa.
 * @param accentStripeColor Color de la franja lateral izquierda. `null` elimina la franja.
 * @param contentPadding Padding interno del contenido dentro de la tarjeta.
 * @param content Slot de contenido con alcance de `ColumnScope`; puede usar `weight`, etc.
 */
@Composable
fun ParkeoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = ParkeoCardShape,
    containerColor: Color = ParkeoTheme.colors.surface1,
    borderColor: Color = ParkeoTheme.colors.border,
    borderWidth: Dp = Dimens.borderThin,
    accentBorder: Boolean = false,
    accentStripeColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    // El borde de acento es translúcido para no competir con el contenido.
    val finalBorderColor = if (accentBorder) ParkeoTheme.colors.accent.copy(alpha = 0.5f) else borderColor

    // Contenido reutilizable para las dos variantes (interactiva y pasiva).
    val cardContent: @Composable () -> Unit = {
        if (accentStripeColor != null) {
            // Variante con franja: fila de 4 dp + columna de contenido con el padding indicado.
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(accentStripeColor)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(contentPadding)
                ) {
                    content()
                }
            }
        } else {
            // Variante simple: columna a todo el ancho con el padding indicado.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding)
            ) {
                content()
            }
        }
    }

    // Tarjeta interactiva: `Surface(onClick)` aporta clic, ripple y semántica de botón.
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = BorderStroke(borderWidth, finalBorderColor)
        ) {
            cardContent()
        }
    } else {
        // Tarjeta pasiva: solo presenta contenido, sin acción táctil.
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = BorderStroke(borderWidth, finalBorderColor)
        ) {
            cardContent()
        }
    }
}
