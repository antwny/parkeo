package pe.parkeo.ui.screens.vehicles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import pe.parkeo.data.remote.dto.VehicleDto
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.VehicleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesScreen(
    viewModel: VehicleViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            showAddDialog = false
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = extended.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            ParkeoTopBar(
                title = "Mis vehículos",
                onNavigationClick = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.clearMessages()
                    showAddDialog = true
                },
                containerColor = extended.accent,
                contentColor = extended.onAccent,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, "Agregar vehículo")
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    ParkeoLoadingView(message = "Cargando vehículos...")
                }
            }

            uiState.vehicles.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    ParkeoEmptyState(
                        title = "Sin vehículos",
                        subtitle = "Agrega tu primer vehículo para comenzar a reservar en nuestra red",
                        icon = Icons.Filled.DirectionsCar,
                        actionLabel = "Agregar vehículo",
                        onAction = {
                            viewModel.clearMessages()
                            showAddDialog = true
                        }
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(Dimens.spacingMd),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    items(uiState.vehicles, key = { it.id }) { vehicle ->
                        VehicleCard(
                            vehicle = vehicle,
                            onDelete = { viewModel.deleteVehicle(vehicle.id) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddVehicleDialog(
                vehicleTypes = uiState.vehicleTypes,
                isLoading = uiState.isLoading,
                errorMessage = uiState.error,
                onDismiss = {
                    showAddDialog = false
                    viewModel.clearMessages()
                },
                onAdd = { plate, brand, model, color, typeId ->
                    viewModel.addVehicle(plate, brand, model, color, typeId)
                }
            )
        }
    }
}

@Composable
private fun VehicleCard(vehicle: VehicleDto, onDelete: () -> Unit) {
    val extended = ParkeoTheme.colors
    var showConfirmDialog by remember { mutableStateOf(false) }

    ParkeoCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.spacingLg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Peruvian License Plate Graphic Element
            LicensePlateBadge(plate = vehicle.licensePlate)

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listOfNotNull(vehicle.brand, vehicle.model).filter { it.isNotBlank() }.joinToString(" ")
                        .ifBlank { "Vehículo" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = extended.textPrimary
                )

                Text(
                    text = listOfNotNull(vehicle.color, vehicle.vehicleType.name).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textSecondary
                )
            }

            IconButton(onClick = { showConfirmDialog = true }) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Eliminar",
                    tint = extended.signalRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = extended.surface1,
            title = {
                Text(
                    text = "Eliminar vehículo",
                    fontWeight = FontWeight.Bold,
                    color = extended.textPrimary
                )
            },
            text = {
                Text(
                    text = "¿Confirmas que deseas eliminar el vehículo con placa ${vehicle.licensePlate}?",
                    color = extended.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showConfirmDialog = false
                }) {
                    Text("Eliminar", color = extended.signalRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar", color = extended.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun LicensePlateBadge(plate: String) {
    val extended = ParkeoTheme.colors
    // Technical license plate box inspired by Peruvian standard plate
    Surface(
        color = extended.surface3,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, extended.accent.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Blue header strip for "PERÚ"
            Text(
                text = "PERÚ",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = extended.accent
            )
            Text(
                text = plate,
                style = Typography.MonospaceTechnical,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = extended.textPrimary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVehicleDialog(
    vehicleTypes: List<pe.parkeo.data.remote.dto.VehicleTypeDto>,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onAdd: (String, String?, String?, String?, Long) -> Unit
) {
    val extended = ParkeoTheme.colors
    var plate by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var selectedTypeId by remember(vehicleTypes) {
        mutableStateOf<Long?>(vehicleTypes.firstOrNull()?.id)
    }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(vehicleTypes) {
        if (selectedTypeId == null && vehicleTypes.isNotEmpty()) {
            selectedTypeId = vehicleTypes.first().id
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extended.surface1,
        title = {
            Text(
                text = "Agregar vehículo",
                fontWeight = FontWeight.Bold,
                color = extended.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
            ) {
                errorMessage?.let { errorText ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ParkeoCardShape,
                        color = extended.signalRed.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            Dimens.borderHairline,
                            extended.signalRed.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(Dimens.spacingSm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = extended.signalRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(Dimens.spacingSm))
                            Text(
                                text = errorText,
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.signalRed,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                ParkeoTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase() },
                    label = "Placa *",
                    placeholder = "ABC-123",
                    modifier = Modifier.fillMaxWidth()
                )

                ParkeoTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = "Marca",
                    placeholder = "Toyota",
                    modifier = Modifier.fillMaxWidth()
                )

                ParkeoTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = "Modelo",
                    placeholder = "Corolla",
                    modifier = Modifier.fillMaxWidth()
                )

                ParkeoTextField(
                    value = color,
                    onValueChange = { color = it },
                    label = "Color",
                    placeholder = "Gris Metálico",
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    ParkeoTextField(
                        value = vehicleTypes.firstOrNull { it.id == selectedTypeId }?.name
                            ?: if (vehicleTypes.isEmpty()) "Cargando tipos..." else "Seleccionar tipo",
                        onValueChange = {},
                        readOnly = true,
                        label = "Tipo de vehículo",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        vehicleTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedTypeId = type.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            ParkeoButton(
                text = "Guardar",
                isLoading = isLoading,
                onClick = {
                    selectedTypeId?.let { typeId ->
                        onAdd(
                            plate.trim(),
                            brand.trim().ifBlank { null },
                            model.trim().ifBlank { null },
                            color.trim().ifBlank { null },
                            typeId
                        )
                    }
                },
                enabled = plate.isNotBlank() && selectedTypeId != null && !isLoading,
                style = ParkeoButtonStyle.Primary,
                modifier = Modifier.height(Dimens.buttonHeightDefault)
            )
        },
        dismissButton = {
            ParkeoButton(
                text = "Cancelar",
                onClick = onDismiss,
                style = ParkeoButtonStyle.Ghost,
                modifier = Modifier.height(Dimens.buttonHeightDefault)
            )
        }
    )
}
