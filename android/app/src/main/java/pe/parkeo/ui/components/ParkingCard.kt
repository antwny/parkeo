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

@Composable
fun ParkingCard(
    parking: ParkingLotDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = ParkeoTheme.colors

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = parking.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = extended.textPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = parking.district ?: parking.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textSecondary
                )
            }

            ParkeoBadge(
                text = badgeText,
                variant = badgeVariant
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
