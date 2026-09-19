package pe.parkeo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkeoTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    onNavigationClick: (() -> Unit)? = onNavigateBack,
    actions: @Composable RowScope.() -> Unit = {},
    showBrandLogo: Boolean = false,
    isBrandTitle: Boolean = showBrandLogo,
    showBottomDivider: Boolean = true
) {
    val extended = ParkeoTheme.colors
    val backHandler = onNavigationClick ?: onNavigateBack
    val isBrand = showBrandLogo || isBrandTitle

    Column(modifier = modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                if (isBrand) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Parkeo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                            color = extended.textPrimary
                        )
                        Box(
                            modifier = Modifier
                                .padding(start = 2.dp, top = 2.dp)
                                .size(6.dp)
                                .background(extended.accent, ParkeoPillShape)
                        )
                    }
                } else {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = extended.textPrimary,
                            maxLines = 1
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                if (backHandler != null) {
                    IconButton(onClick = backHandler) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = extended.textPrimary
                        )
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = extended.surface0,
                titleContentColor = extended.textPrimary,
                navigationIconContentColor = extended.textPrimary,
                actionIconContentColor = extended.textPrimary
            )
        )
        if (showBottomDivider) {
            HorizontalDivider(
                color = extended.borderSubtle,
                thickness = Dimens.borderHairline
            )
        }
    }
}
