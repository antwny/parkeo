package pe.parkeo.ui.screens.operator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.data.remote.dto.ParkingSpaceDto
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.OperatorViewModel

@Composable
fun OperatorSpacesScreen(
    viewModel: OperatorViewModel,
    onNavigateToReservations: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadMyParkingLots()
    }

    var spaceToEdit by remember { mutableStateOf<ParkingSpaceDto?>(null) }
    var selectedFilterStatus by remember { mutableStateOf("TODOS") }

    val filteredSpaces = remember(uiState.spaces, selectedFilterStatus, uiState.searchQuery) {
        uiState.spaces.filter { space ->
            val matchesFilter = when (selectedFilterStatus) {
                "AVAILABLE" -> space.status == "AVAILABLE"
                "OCCUPIED" -> space.status == "OCCUPIED"
                "RESERVED" -> space.status == "RESERVED"
                "MAINTENANCE" -> space.status == "MAINTENANCE"
                else -> true
            }
            val matchesSearch = uiState.searchQuery.isBlank() ||
                    space.spaceNumber.contains(uiState.searchQuery, ignoreCase = true) ||
                    space.vehicleType.contains(uiState.searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    LaunchedEffect(uiState.successMessage, uiState.error) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = extended.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ParkeoTopBar(
                title = "Control de Espacios",
                subtitle = uiState.selectedLot?.name,
                actions = {
                    IconButton(onClick = { viewModel.loadMyParkingLots(refresh = true) }) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Lot selector if multiple
            if (uiState.parkingLots.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.spacingMd, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.parkingLots, key = { it.id }) { lot ->
                        val isSelected = uiState.selectedLot?.id == lot.id
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewModel.selectParkingLot(lot) },
                            color = if (isSelected) extended.accent else extended.surface2,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                Dimens.borderHairline,
                                if (isSelected) extended.accent else extended.borderSubtle
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Storefront,
                                    contentDescription = null,
                                    tint = if (isSelected) extended.onAccent else extended.accent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = lot.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) extended.onAccent else extended.textPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Quick summary banner
            val available = uiState.spaces.count { it.status == "AVAILABLE" }
            val occupied = uiState.spaces.count { it.status == "OCCUPIED" }
            val reserved = uiState.spaces.count { it.status == "RESERVED" }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingMd, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusMiniCard(
                    modifier = Modifier.weight(1f),
                    label = "Disponibles",
                    count = available,
                    color = extended.signalGreen
                )
                StatusMiniCard(
                    modifier = Modifier.weight(1f),
                    label = "Ocupados",
                    count = occupied,
                    color = extended.signalRed
                )
                StatusMiniCard(
                    modifier = Modifier.weight(1f),
                    label = "Reservados",
                    count = reserved,
                    color = extended.signalAmber
                )
            }

            // Status filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingMd, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "TODOS" to "Todos",
                    "AVAILABLE" to "Libres",
                    "OCCUPIED" to "Ocupados",
                    "RESERVED" to "Reservados",
                    "MAINTENANCE" to "Mtto."
                ).forEach { (key, label) ->
                    val isSelected = selectedFilterStatus == key
                    Surface(
                        onClick = { selectedFilterStatus = key },
                        color = if (isSelected) extended.accent else extended.surface2,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            Dimens.borderHairline,
                            if (isSelected) extended.accent else extended.borderSubtle
                        )
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) extended.onAccent else extended.textPrimary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            when {
                uiState.isLoading && uiState.spaces.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        ParkeoLoadingView(message = "Cargando espacios...")
                    }
                }

                filteredSpaces.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        ParkeoEmptyState(
                            title = "Sin espacios",
                            subtitle = "No hay espacios con el filtro seleccionado",
                            icon = Icons.Filled.GridView
                        )
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(horizontal = Dimens.spacingMd),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        items(filteredSpaces, key = { it.id }) { space ->
                            ParkingSpaceTile(
                                space = space,
                                onClick = { spaceToEdit = space }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal to change space status
    spaceToEdit?.let { space ->
        ChangeSpaceStatusDialog(
            space = space,
            onDismiss = { spaceToEdit = null },
            onStatusSelected = { newStatus ->
                viewModel.updateSpaceStatus(space.id, newStatus)
                spaceToEdit = null
            }
        )
    }
}

@Composable
private fun StatusMiniCard(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    color: Color
) {
    val extended = ParkeoTheme.colors
    ParkeoCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = Typography.MonospaceTechnical,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = extended.textSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ParkingSpaceTile(
    space: ParkingSpaceDto,
    onClick: () -> Unit
) {
    val extended = ParkeoTheme.colors
    val (statusColor, badgeStatus) = when (space.status) {
        "AVAILABLE" -> Pair(extended.signalGreen, ParkeoBadgeStatus.Available)
        "OCCUPIED" -> Pair(extended.signalRed, ParkeoBadgeStatus.Occupied)
        "RESERVED" -> Pair(extended.signalAmber, ParkeoBadgeStatus.Reserved)
        else -> Pair(extended.textTertiary, ParkeoBadgeStatus.Closed)
    }

    ParkeoCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp),
        accentBorder = space.status == "AVAILABLE"
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = space.spaceNumber,
                style = Typography.MonospaceTechnical,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = extended.textPrimary
            )

            Icon(
                imageVector = when {
                    space.vehicleType.contains("MOTO", ignoreCase = true) -> Icons.Filled.TwoWheeler
                    space.vehicleType.contains("CAMION", ignoreCase = true) -> Icons.Filled.LocalShipping
                    else -> Icons.Filled.DirectionsCar
                },
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(24.dp)
            )

            ParkeoBadge(
                status = badgeStatus,
                labelOverride = when (space.status) {
                    "AVAILABLE" -> "Libre"
                    "OCCUPIED" -> "Ocupado"
                    "RESERVED" -> "Reserva"
                    else -> "Mtto."
                }
            )
        }
    }
}

@Composable
private fun ChangeSpaceStatusDialog(
    space: ParkingSpaceDto,
    onDismiss: () -> Unit,
    onStatusSelected: (String) -> Unit
) {
    val extended = ParkeoTheme.colors

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extended.surface1,
        title = {
            Text(
                text = "Espacio ${space.spaceNumber}",
                style = Typography.MonospaceTechnical,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = extended.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Selecciona el nuevo estado del espacio:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.textSecondary
                )
                Spacer(Modifier.height(4.dp))

                StatusOptionButton(
                    title = "Disponible (Libre)",
                    subtitle = "Listo para estacionar o reservar",
                    color = extended.signalGreen,
                    icon = Icons.Filled.CheckCircle,
                    isSelected = space.status == "AVAILABLE",
                    onClick = { onStatusSelected("AVAILABLE") }
                )

                StatusOptionButton(
                    title = "Ocupado",
                    subtitle = "Vehículo físicamente estacionado",
                    color = extended.signalRed,
                    icon = Icons.Filled.DirectionsCar,
                    isSelected = space.status == "OCCUPIED",
                    onClick = { onStatusSelected("OCCUPIED") }
                )

                StatusOptionButton(
                    title = "Reservado",
                    subtitle = "Separado para un cliente",
                    color = extended.signalAmber,
                    icon = Icons.Filled.Bookmark,
                    isSelected = space.status == "RESERVED",
                    onClick = { onStatusSelected("RESERVED") }
                )

                StatusOptionButton(
                    title = "Mantenimiento",
                    subtitle = "Bloqueado por reparaciones",
                    color = extended.textTertiary,
                    icon = Icons.Filled.Build,
                    isSelected = space.status == "MAINTENANCE",
                    onClick = { onStatusSelected("MAINTENANCE") }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = extended.textSecondary)
            }
        }
    )
}

@Composable
private fun StatusOptionButton(
    title: String,
    subtitle: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val extended = ParkeoTheme.colors
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color.copy(alpha = 0.12f) else extended.surface2,
        border = androidx.compose.foundation.BorderStroke(
            Dimens.borderHairline,
            if (isSelected) color else extended.borderSubtle
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) color else extended.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textSecondary
                )
            }
            if (isSelected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
        }
    }
}
