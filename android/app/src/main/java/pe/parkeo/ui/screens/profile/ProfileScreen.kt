package pe.parkeo.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.AuthViewModel

@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToVehicles: () -> Unit,
    onNavigateToReservations: () -> Unit,
    onLogout: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val extended = ParkeoTheme.colors
    var showLogoutDialog by remember { mutableStateOf(false) }

    val roleUpper = (userProfile?.role ?: "").uppercase()
    val isAdmin = roleUpper.contains("ADMIN")
    val isOperator = roleUpper.contains("OPERAT")

    val roleBadgeStatus = when {
        isAdmin -> ParkeoBadgeStatus.Admin
        isOperator -> ParkeoBadgeStatus.Operator
        else -> ParkeoBadgeStatus.Client
    }

    val avatarIcon = when {
        isAdmin -> Icons.Filled.AdminPanelSettings
        isOperator -> Icons.Filled.Badge
        else -> Icons.Filled.Person
    }

    Scaffold(
        containerColor = extended.background,
        topBar = {
            ParkeoTopBar(
                title = "Mi perfil",
                onNavigationClick = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.spacingMd),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
        ) {
            // Identity Hero Card
            ParkeoCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(Dimens.spacingLg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(extended.surface2)
                            .border(Dimens.borderHairline, extended.borderSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = avatarIcon,
                            contentDescription = null,
                            tint = extended.accent,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile?.name?.ifBlank { "Usuario Parkeo" } ?: "Usuario Parkeo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = extended.textPrimary
                        )

                        Text(
                            text = userProfile?.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textSecondary
                        )

                        Spacer(Modifier.height(6.dp))

                        ParkeoBadge(status = roleBadgeStatus)
                    }
                }
            }

            // Admin privileges banner
            if (isAdmin) {
                ParkeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = true
                ) {
                    Column(
                        modifier = Modifier.padding(Dimens.spacingLg),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = extended.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Acceso de Administrador",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = extended.textPrimary
                            )
                        }
                        Text(
                            text = "Tienes permisos totales para gestionar cocheras de la red, actualizar estados, supervisar usuarios y consultar métricas globales.",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Section: Gestión
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                Text(
                    text = "GESTIÓN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = extended.textTertiary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )

                ProfileOptionTile(
                    icon = Icons.Filled.DirectionsCar,
                    title = "Mis vehículos",
                    onClick = onNavigateToVehicles
                )

                ProfileOptionTile(
                    icon = Icons.Filled.BookmarkBorder,
                    title = "Mis reservas",
                    onClick = onNavigateToReservations
                )
            }

            // Section: Aplicación
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                Text(
                    text = "APLICACIÓN",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = extended.textTertiary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )

                ProfileOptionTile(
                    icon = Icons.Filled.Security,
                    title = "Seguridad y privacidad",
                    onClick = {}
                )

                ProfileOptionTile(
                    icon = Icons.Filled.Info,
                    title = "Acerca de Parkeo v1.0",
                    onClick = {}
                )
            }

            Spacer(Modifier.height(8.dp))

            // Logout Button
            ParkeoButton(
                text = "Cerrar sesión",
                onClick = { showLogoutDialog = true },
                leadingIcon = Icons.Filled.Logout,
                style = ParkeoButtonStyle.Destructive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeightLarge)
            )

            Spacer(Modifier.height(Dimens.spacingLg))
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                containerColor = extended.surface1,
                title = {
                    Text(
                        text = "Cerrar sesión",
                        fontWeight = FontWeight.Bold,
                        color = extended.textPrimary
                    )
                },
                text = {
                    Text(
                        text = "¿Estás seguro de que deseas cerrar tu sesión en Parkeo?",
                        color = extended.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onLogout()
                    }) {
                        Text("Cerrar sesión", color = extended.signalRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancelar", color = extended.textSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun ProfileOptionTile(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    val extended = ParkeoTheme.colors
    ParkeoCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacingLg, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = extended.accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = extended.textPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = extended.textTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
