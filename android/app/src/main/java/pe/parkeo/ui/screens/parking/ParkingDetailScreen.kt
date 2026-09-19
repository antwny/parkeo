package pe.parkeo.ui.screens.parking

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingDetailScreen(
    parkingId: Long,
    viewModel: ParkingDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReservation: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(parkingId) {
        viewModel.loadParkingLot(parkingId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.parkingLot?.name ?: "Detalle") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Regresar")
                    }
                }
            )
        },
        bottomBar = {
            uiState.parkingLot?.let { parking ->
                Surface(shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Map, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Ver mapa")
                        }
                        Button(
                            onClick = { onNavigateToReservation(parking.id) },
                            modifier = Modifier.weight(1f),
                            enabled = parking.isOpen && parking.availableSpaces > 0
                        ) {
                            Icon(Icons.Filled.BookmarkAdd, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reservar")
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.error != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.ErrorOutline,
                    null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.loadParkingLot(parkingId) }) { Text("Reintentar") }
            }

            uiState.parkingLot != null -> {
                val parking = uiState.parkingLot!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tarjeta de disponibilidad
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (parking.availableSpaces > 0)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer
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
                                Text("Espacios disponibles", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    "${parking.availableSpaces} de ${parking.totalCapacity}",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                            Icon(
                                Icons.Filled.LocalParking,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = {
                                if (parking.totalCapacity > 0)
                                    1f - parking.availableSpaces.toFloat() / parking.totalCapacity
                                else 0f
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 12.dp)
                        )
                    }

                    // Información general
                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Información", style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(parking.address, style = MaterialTheme.typography.bodyMedium)
                            }
                            parking.phone?.let {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Phone,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(it, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            val statusText = if (parking.isOpen) "Abierto hoy" else "Cerrado"
                            val statusIcon = if (parking.isOpen) Icons.Filled.CheckCircle else Icons.Filled.Cancel
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    statusIcon,
                                    null,
                                    tint = if (parking.isOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(statusText, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // Tarifas
                    if (parking.tariffs.isNotEmpty()) {
                        Card {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Tarifas", style = MaterialTheme.typography.titleMedium)
                                parking.tariffs.forEach { tariff ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${tariff.vehicleType} (${tariff.tariffType})", style = MaterialTheme.typography.bodyMedium)
                                        Text("S/ ${String.format("%.2f", tariff.price)}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    // Servicios
                    if (parking.services.isNotEmpty()) {
                        Card {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text("Servicios incluidos", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                parking.services.forEach { service ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(service.name, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}
