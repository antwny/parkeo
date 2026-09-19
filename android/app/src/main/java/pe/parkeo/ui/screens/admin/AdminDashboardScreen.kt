package pe.parkeo.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onNavigateToParkingLots: () -> Unit = {},
    onNavigateToUsers: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val stats = uiState.statistics

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Panel de Administración",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Supervisión global de la red PARKeo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboardData(refresh = true) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
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
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Métricas Principales",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    // KPI Row 1: Estacionamientos y Total Reservas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Estacionamientos",
                            value = "${stats.totalParkingLots}",
                            icon = Icons.Filled.LocalParking,
                            tintColor = ParkeoBlue600,
                            onClick = onNavigateToParkingLots
                        )
                        KpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Total Reservas",
                            value = "${stats.totalReservations}",
                            icon = Icons.Filled.BookmarkBorder,
                            tintColor = ParkeoCyan500
                        )
                    }
                }

                item {
                    // KPI Row 2: Reservas Activas y Pendientes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Reservas Activas",
                            value = "${stats.activeReservations}",
                            icon = Icons.Filled.DirectionsCar,
                            tintColor = ParkeoGreen500
                        )
                        KpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Pendientes",
                            value = "${stats.pendingReservations}",
                            icon = Icons.Filled.Schedule,
                            tintColor = ParkeoAmber500
                        )
                    }
                }

                item {
                    // KPI Row 3: Completadas y Canceladas
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Completadas",
                            value = "${stats.completedReservations}",
                            icon = Icons.Filled.CheckCircleOutline,
                            tintColor = Color(0xFF10B981)
                        )
                        KpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Canceladas",
                            value = "${stats.cancelledReservations}",
                            icon = Icons.Filled.Cancel,
                            tintColor = ParkeoRed500
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Accesos Rápidos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card(
                        onClick = onNavigateToParkingLots,
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(ParkeoBlue600.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Storefront,
                                    contentDescription = null,
                                    tint = ParkeoBlue600,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gestión de Estacionamientos",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${uiState.parkingLots.size} locales registrados. Control de apertura y actividad.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null)
                        }
                    }
                }

                item {
                    Card(
                        onClick = onNavigateToUsers,
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(ParkeoAmber500.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Group,
                                    contentDescription = null,
                                    tint = ParkeoAmber500,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gestión de Usuarios",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${uiState.users.size} usuarios (Admin, Operadores, Clientes).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null)
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
private fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    tintColor: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(tintColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
