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
    val finalBorderColor = if (accentBorder) ParkeoTheme.colors.accent.copy(alpha = 0.5f) else borderColor

    val cardContent: @Composable () -> Unit = {
        if (accentStripeColor != null) {
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding)
            ) {
                content()
            }
        }
    }

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
