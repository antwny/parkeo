/*
 * PARKeo · Android
 * Archivo: `android/app/src/main/java/pe/parkeo/ui/components/ParkeoBottomBar.kt`
 * Capa: componentes reutilizables de interfaz.
 * Propósito: proporcionar la barra de navegación inferior de la app, con elementos configurables,
 *   selección controlada desde la pantalla y transición de color suave.
 * Conceptos clave: composable stateless, estado elevado (`selectedIndex`/`onItemSelected`),
 *   `ParkeoBottomNavItem`, `ImageVector`, `Modifier`, animación de color, insets de navegación y
 *   accesibilidad.
 * Flujo de uso: `MainScreen` construye la lista de elementos, guarda el índice seleccionado y
 *   pasa ambos valores. Al tocar un elemento, la barra invoca `onItemSelected(index)`; no cambia
 *   de pestaña por sí misma.
 * Relación con pantallas/tema: se usa en la pantalla principal con roles cliente, operador y
 *   administrador. Consume `surface1`, `borderSubtle`, acento y textos de `ParkeoTheme`.
 * Guía: `docs/GUIA_MAESTRA_ESTUDIANTE.md`.
 */
package pe.parkeo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.theme.*

/**
 * Elemento de la barra inferior. Es un modelo de presentación inmutable: el título es la etiqueta
 * visible y también la descripción accesible del icono; el icono representa la sección.
 *
 * @property title Etiqueta visible y nombre accesible del elemento.
 * @property icon Icono de la sección. No se distingue entre estado seleccionado y no seleccionado
 *   en este modelo.
 */
data class ParkeoBottomNavItem(
    val title: String,
    val icon: ImageVector
)

/**
 * Barra de navegación inferior de PARKeo. Es stateless: el índice seleccionado y la lista de
 * elementos son datos del llamador.
 *
 * Distribuye los elementos con `SpaceAround` y asigna el mismo peso a cada uno. El contenedor
 * pinta `surface1`, añade `navigationBarsPadding()` para no quedar debajo de la barra del
 * sistema (la actividad usa edge-to-edge) y separa el contenido con un divisor capilar.
 *
 * Cada elemento es una columna clicable de 58 dp de alto. Al seleccionarse, el icono se tiñe con
 * el acento y aparece un fondo tipo píldora translúcido; el color se anima durante 200 ms. El
 * clic no muestra ripple (`indication = null`) porque la selección de color ya comunica el
 * cambio. La selección es visual: no se declara un rol de pestaña ni un estado `selected` de
 * semántica; el icono aporta [ParkeoBottomNavItem.title] como `contentDescription` y además se
 * dibuja la etiqueta textual.
 *
 * @param items Elementos a mostrar, en orden. Una lista vacía produce una barra sin contenido.
 * @param selectedIndex Índice del elemento activo. Si queda fuera de rango, ningún elemento se
 *   pinta como seleccionado, pero la barra sigue siendo interactiva.
 * @param onItemSelected Callback con el índice pulsado; la pantalla decide la navegación.
 * @param modifier `Modifier` aplicado a la columna contenedora.
 */
@Composable
fun ParkeoBottomBar(
    items: List<ParkeoBottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = ParkeoTheme.colors

    // Contenedor de la barra: fondo, inset inferior y divisor superior.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(extended.surface1)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(
            color = extended.borderSubtle,
            thickness = Dimens.borderHairline
        )

        // Fila de elementos, todos con el mismo peso.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                // Transición de color de 200 ms para que la selección no sea abrupta.
                val itemColor by animateColorAsState(
                    targetValue = if (isSelected) extended.accent else extended.textTertiary,
                    animationSpec = tween(durationMillis = 200),
                    label = "itemColor"
                )

                // Área táctil de cada elemento. Sin ripple: el cambio de color es la señal.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onItemSelected(index) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Fondo píldora del icono seleccionado.
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .padding(horizontal = 12.dp)
                            .background(
                                color = if (isSelected) extended.accent.copy(alpha = 0.12f) else Color.Transparent,
                                shape = ParkeoPillShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Icono de la sección; su descripción accesible es el título.
                        Icon(
                            imageVector = itemItemIcon(item.icon, isSelected),
                            contentDescription = item.title,
                            modifier = Modifier.size(20.dp),
                            tint = itemColor
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    // Etiqueta textual: refuerza el icono y mantiene la barra legible.
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = itemColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Resuelve el icono de un elemento según su selección. En la implementación actual devuelve el
 * mismo `ImageVector` sin importar [isSelected]; la función existe como punto de extensión para
 * poder usar iconos rellenos/contorneados en el futuro sin cambiar los llamadores.
 *
 * @param icon Icono original del elemento.
 * @param isSelected Estado de selección del elemento; hoy no altera el resultado.
 * @return El mismo [icon] recibido.
 */
private fun itemItemIcon(icon: ImageVector, isSelected: Boolean): ImageVector {
    return icon
}
