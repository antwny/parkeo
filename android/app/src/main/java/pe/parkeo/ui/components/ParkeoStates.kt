package pe.parkeo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.theme.*

@Composable
fun ParkeoEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String = "",
    subtitle: String = description,
    icon: ImageVector = Icons.Filled.Info,
    actionButtonText: String? = null,
    actionLabel: String? = actionButtonText,
    onActionClick: (() -> Unit)? = null,
    onAction: (() -> Unit)? = onActionClick
) {
    val extended = ParkeoTheme.colors
    val desc = if (description.isNotBlank()) description else subtitle
    val actionText = actionLabel ?: actionButtonText
    val actionHandler = onAction ?: onActionClick

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(extended.surface2, CircleShape)
                .border(Dimens.borderThin, extended.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = extended.accent
            )
        }

        Spacer(Modifier.height(Dimens.space20))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = extended.textPrimary,
            textAlign = TextAlign.Center
        )

        if (desc.isNotBlank()) {
            Spacer(Modifier.height(Dimens.space8))

            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = extended.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }

        if (actionText != null && actionHandler != null) {
            Spacer(Modifier.height(Dimens.space24))
            ParkeoButton(
                text = actionText,
                onClick = actionHandler,
                style = ParkeoButtonStyle.Secondary,
                compact = true,
                modifier = Modifier.widthIn(max = 200.dp)
            )
        }
    }
}

@Composable
fun ParkeoErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = ParkeoTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(ParkeoRedContainer, CircleShape)
                .border(Dimens.borderThin, ParkeoRed500.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = ParkeoRed500
            )
        }

        Spacer(Modifier.height(Dimens.space16))

        Text(
            text = "Algo salió mal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = extended.textPrimary
        )

        Spacer(Modifier.height(Dimens.space8))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = extended.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(Dimens.space24))

        ParkeoButton(
            text = "Reintentar",
            onClick = onRetry,
            style = ParkeoButtonStyle.Primary,
            compact = true,
            modifier = Modifier.widthIn(max = 180.dp)
        )
    }
}

@Composable
fun ParkeoLoadingView(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    val extended = ParkeoTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = extended.accent,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(36.dp)
        )
        if (message != null) {
            Spacer(Modifier.height(Dimens.space16))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = extended.textSecondary
            )
        }
    }
}
