package pe.parkeo.ui.screens.reservation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.parkeo.ui.viewmodel.ParkingDetailViewModel
import pe.parkeo.ui.viewmodel.ReservationViewModel
import pe.parkeo.ui.viewmodel.VehicleViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationCreateScreen(
    parkingId: Long,
    parkingViewModel: ParkingDetailViewModel,
    vehicleViewModel: VehicleViewModel,
    reservationViewModel: ReservationViewModel,
    onNavigateBack: () -> Unit,
    onReservationCreated: () -> Unit
) {
    val parkingState by parkingViewModel.uiState.collectAsState()
    val vehicleState by vehicleViewModel.uiState.collectAsState()
    val reservationState by reservationViewModel.uiState.collectAsState()

    var selectedVehicleId by remember { mutableStateOf<Long?>(null) }
    var durationHours by remember { mutableIntStateOf(2) }

    val startTime = remember {
        LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0)
    }
    val endTime = startTime.plusHours(durationHours.toLong())
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    LaunchedEffect(parkingId) {
        parkingViewModel.loadParkingLot(parkingId)
        vehicleViewModel.loadVehicles()
    }

    LaunchedEffect(vehicleState.vehicles) {
        if (selectedVehicleId == null && vehicleState.vehicles.isNotEmpty()) {
            selectedVehicleId = vehicleState.vehicles.first().id
        }
    }

    LaunchedEffect(reservationState.reservationCreated) {
        if (reservationState.reservationCreated) {
            reservationViewModel.clearMessages()
            onReservationCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva reserva") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "Regresar") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Información del estacionamiento
            parkingState.parkingLot?.let { parking ->
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.LocalParking,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(parking.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                parking.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                            )
                        }
                    }
                }
            }

            // Duración
            Card {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Duración estimada", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(1, 2, 3, 4, 8, 12).forEach { hours ->
                            FilterChip(
                                selected = durationHours == hours,
                                onClick = { durationHours = hours },
                                label = { Text("${hours}h") }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Inicio: ${startTime.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Fin: ${endTime.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Selección de vehículo
            Card {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Selecciona tu vehículo", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    if (vehicleState.vehicles.isEmpty()) {
                        Text(
                            "No tienes vehículos registrados. Agrega uno en la sección de Vehículos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        vehicleState.vehicles.forEach { vehicle ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedVehicleId = vehicle.id }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedVehicleId == vehicle.id,
                                    onClick = { selectedVehicleId = vehicle.id }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(vehicle.licensePlate, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${vehicle.brand ?: ""} ${vehicle.model ?: ""} • ${vehicle.vehicleType.name}".trim(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Estimación de costo
            parkingState.parkingLot?.let { parking ->
                val rate = parking.tariffs.firstOrNull()?.price ?: 5.0
                val estimatedTotal = rate * durationHours
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total estimado", style = MaterialTheme.typography.bodySmall)
                            Text("S/ ${String.format("%.2f", estimatedTotal)}", style = MaterialTheme.typography.titleLarge)
                        }
                        Text("Pago al finalizar", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Errores
            reservationState.error?.let { error ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Botón de confirmación
            Button(
                onClick = {
                    selectedVehicleId?.let { vehicleId ->
                        // Usa el primer espacio del parking o el asignado
                        val spaceId = 1L
                        reservationViewModel.createReservation(
                            parkingSpaceId = spaceId,
                            vehicleId = vehicleId,
                            startTime = formatter.format(startTime),
                            endTime = formatter.format(endTime)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !reservationState.isLoading && selectedVehicleId != null && vehicleState.vehicles.isNotEmpty()
            ) {
                if (reservationState.isLoading) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Filled.BookmarkAdd, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Confirmar reserva")
                }
            }
        }
    }
}
