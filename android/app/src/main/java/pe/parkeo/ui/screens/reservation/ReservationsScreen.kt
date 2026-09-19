package pe.parkeo.ui.screens.reservation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import pe.parkeo.data.remote.dto.ReservationDto
import pe.parkeo.ui.viewmodel.ReservationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsScreen(
    viewModel: ReservationViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Próximas", "Activas", "Historial")
    val statusFilters = listOf("PENDING,CONFIRMED", "ACTIVE", "COMPLETED,CANCELLED")

    LaunchedEffect(selectedTab) {
        viewModel.loadReservations(statusFilters[selectedTab])
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis reservas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "Regresar") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.reservations.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.BookmarkBorder,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.3f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No tienes reservas en esta sección",
                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.reservations) { reservation ->
                        ReservationCard(
                            reservation = reservation,
                            onCancel = { viewModel.cancelReservation(reservation.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReservationCard(reservation: ReservationDto, onCancel: () -> Unit) {
    val canCancel = reservation.status in listOf("PENDING", "CONFIRMED")

    val (statusColor, statusText) = when (reservation.status) {
        "PENDING" -> Pair(MaterialTheme.colorScheme.secondary, "Pendiente")
        "CONFIRMED" -> Pair(MaterialTheme.colorScheme.primary, "Confirmada")
        "ACTIVE" -> Pair(Color(0xFF22C55E), "Activa")
        "COMPLETED" -> Pair(MaterialTheme.colorScheme.onSurface.copy(0.5f), "Completada")
        "CANCELLED" -> Pair(MaterialTheme.colorScheme.error, "Cancelada")
        else -> Pair(MaterialTheme.colorScheme.onSurface.copy(0.5f), reservation.status)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(reservation.parkingLotName, style = MaterialTheme.typography.titleMedium)
                SuggestionChip(
                    onClick = {},
                    label = { Text(statusText, style = MaterialTheme.typography.labelSmall) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = statusColor.copy(alpha = 0.1f),
                        labelColor = statusColor
                    )
                )
            }
            Text(
                reservation.parkingLotAddress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarToday, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        reservation.startTime.take(16).replace("T", " "),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DirectionsCar, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(reservation.vehicleLicensePlate, style = MaterialTheme.typography.bodySmall)
                }
            }
            reservation.totalPrice?.let {
                Text(
                    "S/ ${String.format("%.2f", it)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (canCancel) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Cancel, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cancelar reserva")
                }
            }
        }
    }
}
