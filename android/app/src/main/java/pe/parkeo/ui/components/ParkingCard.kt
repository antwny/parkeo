/*
 * PARKeo · Android
 * Archivo: `android/app/src/main/java/pe/parkeo/ui/components/ParkingCard.kt`
 * Capa: componentes reutilizables de interfaz (tarjeta de dominio).
 * Propósito: resumir un estacionamiento en una tarjeta clicable: nombre, ubicación, estado de
 *   disponibilidad, capacidad y distancia. Es el puente visual entre los datos de
 *   `ParkingLotDto` y la lista de resultados.
 * Conceptos clave: composable stateless, modelo de datos de red (`ParkingLotDto`), `ParkeoCard`,
 *   `ParkeoBadge`, `Modifier`, color semántico, formato numérico y accesibilidad del clic.
 * Flujo de uso: la pantalla recibe una lista de estacionamientos, crea una `ParkingCard` por
 *   elemento y pasa `onClick` para abrir el detalle. La tarjeta no consulta la API ni calcula la
 *   distancia: solo presenta los campos que ya trae el DTO.
 * Relación con pantallas/tema: se usa en la búsqueda de inicio. Consume `ParkeoTheme.colors`,
 *   `ParkeoCardShape`, `MaterialTheme.typography` y la escala de badges del sistema de diseño.
 * Guía: `docs/GUIA_MAESTRA_ESTUDIANTE.md`.
 */
package pe.parkeo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.data.remote.dto.ParkingLotDto
import pe.parkeo.ui.theme.*

/**
 * Tarjeta de estacionamiento. Es stateless: recibe el DTO y un callback de clic; no guarda
 * estado ni modifica el modelo.
 *
 * Datos que muestra:
 * - [ParkingLotDto.name] como título.
 * - [ParkingLotDto.district] si existe; en caso contrario, [ParkingLotDto.address].
 * - Un badge de disponibilidad calculado en este orden: si `!isOpen`, "Cerrado" (variante
 *   `Closed`); si `availableSpaces == 0`, "Completo" (variante `Occupied`); si quedan 3 o menos,
 *   "N libres" (variante `Reserved`); en otro caso, "N libres" (variante `Available`).
 * - La relación `availableSpaces/totalCapacity` con la palabra "espacios". `totalCapacity` es
 *   una propiedad calculada del DTO que prioriza `totalSpaces` y recurre a `totalCapacityVal`.
 * - La distancia si el DTO la trae (`distanceKm` o su alias `distance`), formateada con
 *   `String.format("%.1f", dist)` y el sufijo " km". El formato usa el locale por defecto del
 *   dispositivo, así que el separador decimal puede ser punto o coma.
 *
 * Esta tarjeta no muestra precio ni tarifa: el DTO de lista no incluye un importe y la tarifa se
 * consulta en el detalle. Por eso aquí no hay formato monetario; si una pantalla necesita precio,
 * debe renderizarlo con su propia lógica (por ejemplo, `S/` y dos decimales).
 *
 * La franja lateral de acento solo aparece cuando el estacionamiento está abierto y tiene
 * espacios disponibles. Toda la tarjeta es clicable a través de la sobrecarga interactiva de
 * `ParkeoCard`; los iconos son decorativos y la información textual es el contenido accesible.
 *
 * @param parking Datos del estacionamiento que se van a resumir.
 * @param onClick Callback que abre el detalle u otra acción; se eleva a la pantalla.
 * @param modifier `Modifier` externo; la tarjeta fuerza ancho completo después de aplicarlo.
 */
@Composable
fun ParkingCard(
    parking: ParkingLotDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = ParkeoTheme.colors

    // Estado resumido: cerrado tiene prioridad; luego completo; luego pocas plazas; luego normal.
    val (badgeText, badgeVariant) = when {
        !parking.isOpen -> "Cerrado" to BadgeVariant.Closed
        parking.availableSpaces == 0 -> "Completo" to BadgeVariant.Occupied
        parking.availableSpaces <= 3 -> "${parking.availableSpaces} libres" to BadgeVariant.Reserved
        else -> "${parking.availableSpaces} libres" to BadgeVariant.Available
    }

    ParkeoCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        accentStripeColor = if (parking.isOpen && parking.availableSpaces > 0) extended.accent else null
    ) {
        // Encabezado: nombre y ubicación a la izquierda; badge de estado a la derecha.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                // Nombre del estacionamiento.
                Text(
                    text = parking.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = extended.textPrimary
                )
                Spacer(Modifier.height(3.dp))
                // Ubicación: distrito si existe; si no, dirección.
                Text(
                    text = parking.district ?: parking.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textSecondary
                )
            }

            // Badge de disponibilidad calculado arriba.
            ParkeoBadge(
                text = badgeText,
                variant = badgeVariant
            )
        }

        Spacer(Modifier.height(14.dp))

        // Fila inferior: capacidad disponible y distancia.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador de espacios: icono de auto + relación disponible/total en monoespaciado.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = extended.accent
                )
                Text(
                    text = "${parking.availableSpaces}/${parking.totalCapacity} espacios",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = MonospaceTechnical.fontFamily),
                    fontWeight = FontWeight.SemiBold,
                    color = extended.textPrimary
                )
            }

            // Distancia opcional: solo se dibuja si el DTO trae un valor no nulo.
            parking.distance?.let { dist ->
                Surface(
                    shape = ParkeoTagShape,
                    color = extended.surface2,
                    border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, extended.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NearMe,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = extended.textSecondary
                        )
                        // Un decimal y sufijo "km"; el separador depende del locale del sistema.
                        Text(
                            text = "${String.format("%.1f", dist)} km",
                            style = MaterialTheme.typography.labelSmall,
                            color = extended.textSecondary
                        )
                    }
                }
            }
        }
    }
}
