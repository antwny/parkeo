package pe.parkeo.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.AdminViewModel

@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onNavigateToParkingLots: () -> Unit = {},
    onNavigateToUsers: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors
    val stats = uiState.statistics

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    Scaffold(
        containerColor = extended.background,
        topBar = {
            ParkeoTopBar(
                title = "Panel de Administración",
                subtitle = "Supervisión global de la red Parkeo",
                actions = {
                    IconButton(onClick = { viewModel.loadDashboardData(refresh = true) }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Actualizar",
                            tint = extended.textSecondary
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading && !uiState.isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                ParkeoLoadingView(message = "Cargando métricas de la red...")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Dimens.spacingMd),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
            ) {
                item {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "MÉTRICAS CLAVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = extended.textTertiary
                    )
                }

                item {
                    // KPI Row 1: Estacionamientos y Total Reservas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                    ) {
                        ModernKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Estacionamientos",
                            value = "${stats.totalParkingLots}",
                            icon = Icons.Filled.LocalParking,
                            tintColor = extended.accent,
                            onClick = onNavigateToParkingLots
                        )
                        ModernKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Total Reservas",
                            value = "${stats.totalReservations}",
                            icon = Icons.Filled.BookmarkBorder,
                            tintColor = extended.textPrimary
                        )
                    }
                }

                item {
                    // KPI Row 2: Reservas Activas y Pendientes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                    ) {
                        ModernKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Reservas Activas",
                            value = "${stats.activeReservations}",
                            icon = Icons.Filled.DirectionsCar,
                            tintColor = extended.signalGreen
                        )
                        ModernKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Pendientes",
                            value = "${stats.pendingReservations}",
                            icon = Icons.Filled.Schedule,
                            tintColor = extended.signalAmber
                        )
                    }
                }

                item {
                    // KPI Row 3: Completadas y Canceladas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                    ) {
                        ModernKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Completadas",
                            value = "${stats.completedReservations}",
                            icon = Icons.Filled.CheckCircleOutline,
                            tintColor = extended.signalGreen
                        )
                        ModernKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Canceladas",
                            value = "${stats.cancelledReservations}",
                            icon = Icons.Filled.Cancel,
                            tintColor = extended.signalRed
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "GESTIÓN DIRECTA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = extended.textTertiary
                    )
                }

                item {
                    ParkeoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToParkingLots)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.spacingLg),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(extended.accent.copy(alpha = 0.12f), CircleShape)
                                    .border(Dimens.borderHairline, extended.accent.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Storefront,
                                    contentDescription = null,
                                    tint = extended.accent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gestión de Estacionamientos",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = extended.textPrimary
                                )
                                Text(
                                    text = "${uiState.parkingLots.size} locales en la red. Control de disponibilidad y estado.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.textSecondary
                                )
                            }

                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = extended.textTertiary
                            )
                        }
                    }
                }

                item {
                    ParkeoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToUsers)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.spacingLg),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(extended.surface3, CircleShape)
                                    .border(Dimens.borderHairline, extended.borderSubtle, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Group,
                                    contentDescription = null,
                                    tint = extended.textPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gestión de Usuarios",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = extended.textPrimary
                                )
                                Text(
                                    text = "${uiState.users.size} cuentas registradas (Admin, Operadores, Clientes).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.textSecondary
                                )
                            }

                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = extended.textTertiary
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ModernKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    tintColor: Color,
    onClick: (() -> Unit)? = null
) {
    val extended = ParkeoTheme.colors
    ParkeoCard(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.spacingLg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = extended.textSecondary,
                    maxLines = 1
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(tintColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = value,
                style = Typography.MonospaceTechnical,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = extended.textPrimary
            )
        }
    }
}
