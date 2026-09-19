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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.data.remote.dto.ParkingSpaceDto
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.OperatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorSpacesScreen(
    viewModel: OperatorViewModel,
    onNavigateToReservations: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Control de Espacios",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        uiState.selectedLot?.let { lot ->
                            Text(
                                text = lot.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadMyParkingLots(refresh = true) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
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
            // If operator has multiple lots, show selector tabs/chips
            if (uiState.parkingLots.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.parkingLots, key = { it.id }) { lot ->
                        val isSelected = uiState.selectedLot?.id == lot.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectParkingLot(lot) },
                            label = { Text(lot.name, maxLines = 1) },
                            leadingIcon = {
                                Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            // Quick summary banner
            val total = uiState.spaces.size
            val available = uiState.spaces.count { it.status == "AVAILABLE" }
            val occupied = uiState.spaces.count { it.status == "OCCUPIED" }
            val reserved = uiState.spaces.count { it.status == "RESERVED" }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusMiniCard(
                    modifier = Modifier.weight(1f),
                    label = "Disponibles",
                    count = available,
                    color = ParkeoGreen500
                )
                StatusMiniCard(
                    modifier = Modifier.weight(1f),
                    label = "Ocupados",
                    count = occupied,
                    color = ParkeoRed500
                )
                StatusMiniCard(
                    modifier = Modifier.weight(1f),
                    label = "Reservados",
                    count = reserved,
                    color = ParkeoAmber500
                )
            }

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "TODOS" to "Todos",
                    "AVAILABLE" to "Libres",
                    "OCCUPIED" to "Ocupados",
                    "RESERVED" to "Reservados",
                    "MAINTENANCE" to "Mtto."
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedFilterStatus == key,
                        onClick = { selectedFilterStatus = key },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            if (uiState.isLoading && uiState.spaces.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredSpaces.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay espacios con el filtro seleccionado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
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
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = color,
                maxLines = 1
            )
        }
    }
}

private data class SpaceStatusUi(
    val bg: Color,
    val border: Color,
    val text: String,
    val color: Color
)

@Composable
private fun ParkingSpaceTile(
    space: ParkingSpaceDto,
    onClick: () -> Unit
) {
    val style = when (space.status) {
        "AVAILABLE" -> SpaceStatusUi(
            bg = ParkeoGreen500.copy(alpha = 0.12f),
            border = ParkeoGreen500,
            text = "LIBRE",
            color = ParkeoGreen500
        )
        "OCCUPIED" -> SpaceStatusUi(
            bg = ParkeoRed500.copy(alpha = 0.12f),
            border = ParkeoRed500,
            text = "OCUPADO",
            color = ParkeoRed500
        )
        "RESERVED" -> SpaceStatusUi(
            bg = ParkeoAmber500.copy(alpha = 0.12f),
            border = ParkeoAmber500,
            text = "RESERVADO",
            color = ParkeoAmber500
        )
        else -> SpaceStatusUi(
            bg = ParkeoGray500.copy(alpha = 0.12f),
            border = ParkeoGray500,
            text = "MTTO",
            color = ParkeoGray500
        )
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(105.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = style.bg),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, style.border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = space.spaceNumber,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Icon(
                imageVector = when {
                    space.vehicleType.contains("MOTO", ignoreCase = true) -> Icons.Filled.TwoWheeler
                    space.vehicleType.contains("CAMION", ignoreCase = true) -> Icons.Filled.LocalShipping
                    else -> Icons.Filled.DirectionsCar
                },
                contentDescription = null,
                tint = style.color,
                modifier = Modifier.size(24.dp)
            )

            Surface(
                color = style.color,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = style.text,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ChangeSpaceStatusDialog(
    space: ParkingSpaceDto,
    onDismiss: () -> Unit,
    onStatusSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Espacio ${space.spaceNumber}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Selecciona el nuevo estado del espacio:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))

                StatusOptionButton(
                    title = "Disponible (Libre)",
                    subtitle = "Listo para estacionar o reservar",
                    color = ParkeoGreen500,
                    icon = Icons.Filled.CheckCircle,
                    isSelected = space.status == "AVAILABLE",
                    onClick = { onStatusSelected("AVAILABLE") }
                )

                StatusOptionButton(
                    title = "Ocupado",
                    subtitle = "Vehículo físicamente estacionado",
                    color = ParkeoRed500,
                    icon = Icons.Filled.DirectionsCar,
                    isSelected = space.status == "OCCUPIED",
                    onClick = { onStatusSelected("OCCUPIED") }
                )

                StatusOptionButton(
                    title = "Reservado",
                    subtitle = "Separado para un cliente",
                    color = ParkeoAmber500,
                    icon = Icons.Filled.Bookmark,
                    isSelected = space.status == "RESERVED",
                    onClick = { onStatusSelected("RESERVED") }
                )

                StatusOptionButton(
                    title = "Mantenimiento",
                    subtitle = "Bloqueado por reparaciones",
                    color = ParkeoGray500,
                    icon = Icons.Filled.Build,
                    isSelected = space.status == "MAINTENANCE",
                    onClick = { onStatusSelected("MAINTENANCE") }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
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
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = color)
            }
        }
    }
}
