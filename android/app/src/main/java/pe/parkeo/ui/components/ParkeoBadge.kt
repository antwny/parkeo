package pe.parkeo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.theme.*

enum class BadgeVariant {
    Available,
    Occupied,
    Reserved,
    Closed,
    Neutral,
    RoleAdmin,
    RoleOperator,
    RoleClient,
    Admin,
    Operator,
    Client
}

typealias ParkeoBadgeStatus = BadgeVariant

@Composable
fun ParkeoBadge(
    status: BadgeVariant,
    modifier: Modifier = Modifier,
    labelOverride: String? = null,
    showDot: Boolean = true
) {
    val defaultText = when (status) {
        BadgeVariant.Available -> "Disponible"
        BadgeVariant.Occupied -> "Ocupado"
        BadgeVariant.Reserved -> "Reservado"
        BadgeVariant.Closed -> "Cerrado"
        BadgeVariant.Neutral -> "Inactivo"
        BadgeVariant.RoleAdmin, BadgeVariant.Admin -> "Admin"
        BadgeVariant.RoleOperator, BadgeVariant.Operator -> "Operador"
        BadgeVariant.RoleClient, BadgeVariant.Client -> "Cliente"
    }

    ParkeoBadge(
        text = labelOverride ?: defaultText,
        modifier = modifier,
        variant = status,
        showDot = showDot
    )
}

@Composable
fun ParkeoBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.Available,
    showDot: Boolean = true
) {
    val (bgColor, textColor, borderColor) = when (variant) {
        BadgeVariant.Available -> Triple(
            ParkeoLime.copy(alpha = 0.15f),
            if (ParkeoTheme.colors.isDark) ParkeoLime else Color(0xFF4D7300),
            ParkeoLime.copy(alpha = 0.35f)
        )
        BadgeVariant.Occupied -> Triple(
            ParkeoRed500.copy(alpha = 0.15f),
            ParkeoRed500,
            ParkeoRed500.copy(alpha = 0.35f)
        )
        BadgeVariant.Reserved -> Triple(
            ParkeoAmber500.copy(alpha = 0.15f),
            ParkeoAmber500,
            ParkeoAmber500.copy(alpha = 0.35f)
        )
        BadgeVariant.Closed, BadgeVariant.Neutral -> Triple(
            ParkeoGray500.copy(alpha = 0.15f),
            ParkeoGray500,
            ParkeoGray500.copy(alpha = 0.3f)
        )
        BadgeVariant.RoleAdmin, BadgeVariant.Admin -> Triple(
            Color(0xFF8B5CF6).copy(alpha = 0.15f),
            Color(0xFFA78BFA),
            Color(0xFF8B5CF6).copy(alpha = 0.4f)
        )
        BadgeVariant.RoleOperator, BadgeVariant.Operator -> Triple(
            Color(0xFFF59E0B).copy(alpha = 0.15f),
            Color(0xFFFBBF24),
            Color(0xFFF59E0B).copy(alpha = 0.4f)
        )
        BadgeVariant.RoleClient, BadgeVariant.Client -> Triple(
            ParkeoCyan500.copy(alpha = 0.15f),
            ParkeoCyan500,
            ParkeoCyan500.copy(alpha = 0.35f)
        )
    }

    Surface(
        modifier = modifier,
        shape = ParkeoTagShape,
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(textColor, CircleShape)
                )
            }
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.5.sp,
                letterSpacing = 0.6.sp,
                color = textColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
