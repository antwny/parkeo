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

enum class ParkeoButtonStyle {
    Primary,      // Signature Lime + Obsidian Dark text
    Secondary,    // Elevated dark/light surface + crisp border
    Ghost,        // Transparent with subtle hover
    Destructive   // Red accent for irreversible actions
}

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

    val containerColor = when (style) {
        ParkeoButtonStyle.Primary -> if (enabled) extended.accent else extended.surface3
        ParkeoButtonStyle.Secondary -> extended.surface2
        ParkeoButtonStyle.Ghost -> Color.Transparent
        ParkeoButtonStyle.Destructive -> ParkeoRed500
    }

    val contentColor = when (style) {
        ParkeoButtonStyle.Primary -> if (enabled) extended.onAccent else extended.textTertiary
        ParkeoButtonStyle.Secondary -> extended.textPrimary
        ParkeoButtonStyle.Ghost -> extended.textPrimary
        ParkeoButtonStyle.Destructive -> White
    }

    val border = when (style) {
        ParkeoButtonStyle.Primary -> null
        ParkeoButtonStyle.Secondary -> BorderStroke(Dimens.borderThin, extended.border)
        ParkeoButtonStyle.Ghost -> null
        ParkeoButtonStyle.Destructive -> null
    }

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
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
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
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.iconSizeMedium),
                            tint = contentColor
                        )
                        Spacer(Modifier.width(Dimens.space8))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        fontSize = if (compact) 13.sp else 14.5.sp
                    )
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
