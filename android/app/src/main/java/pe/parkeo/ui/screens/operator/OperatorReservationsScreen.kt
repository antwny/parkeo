package pe.parkeo.ui.screens.operator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import pe.parkeo.data.remote.dto.ReservationDto
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.OperatorViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorReservationsScreen(
    viewModel: OperatorViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Por Ingresar", "En Cochera", "Finalizadas", "Todas")
    var searchQuery by remember { mutableStateOf("") }

    var reservationToConfirmAction by remember { mutableStateOf<Pair<ReservationDto, String>?>(null) }

    LaunchedEffect(uiState.selectedLot) {
        uiState.selectedLot?.id?.let { lotId ->
            viewModel.loadSpacesAndReservations(lotId)
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

    val filteredReservations = remember(uiState.reservations, selectedTab, searchQuery) {
        uiState.reservations.filter { r ->
            val matchesTab = when (selectedTab) {
                0 -> r.status in listOf("PENDING", "CONFIRMED")
                1 -> r.status == "ACTIVE"
                2 -> r.status in listOf("COMPLETED", "CANCELLED", "NO_SHOW")
                else -> true
            }
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                    (r.vehicleLicensePlate ?: "").lowercase().contains(q) ||
                    (r.confirmationCode ?: "").lowercase().contains(q) ||
                    (r.spaceNumber ?: "").lowercase().contains(q) ||
                    (r.parkingLotName ?: "").lowercase().contains(q)

            matchesTab && matchesQuery
        }
    }

    Scaffold(
        containerColor = extended.background,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ParkeoTopBar(
                title = "Control de Reservas",
                subtitle = uiState.selectedLot?.name ?: "Gestión de ingresos y salidas",
                onNavigationClick = onNavigateBack,
                actions = {
                    IconButton(onClick = {
                        uiState.selectedLot?.id?.let { viewModel.loadSpacesAndReservations(it) }
                    }) {
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
            // Selector de cochera si tiene más de 1
            if (uiState.parkingLots.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.spacingMd, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.parkingLots) { lot ->
                        val isSelected = uiState.selectedLot?.id == lot.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectParkingLot(lot) },
                            label = { Text(lot.name, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = extended.accent,
                                selectedLabelColor = extended.onAccent
                            )
                        )
                    }
                }
            }

            // Buscador
            Box(modifier = Modifier.padding(horizontal = Dimens.spacingMd, vertical = 6.dp)) {
                ParkeoTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Buscar placa, código PKO o espacio...",
                    leadingIcon = Icons.Filled.Search,
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Limpiar",
                                    tint = extended.textSecondary
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Pestañas de estado
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = extended.surface1,
                contentColor = extended.accent
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Lista de reservas
            if (uiState.isLoading && uiState.reservations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ParkeoLoadingView(message = "Cargando reservas...")
                }
            } else if (filteredReservations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ParkeoEmptyState(
                        title = "No hay reservas",
                        description = if (searchQuery.isNotBlank()) "No se encontraron coincidencias para '$searchQuery'"
                        else "No hay reservas en este estado para la cochera seleccionada"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSm),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    items(filteredReservations, key = { it.id }) { reservation ->
                        OperatorReservationCard(
                            reservation = reservation,
                            onCheckIn = { reservationToConfirmAction = reservation to "CHECK_IN" },
                            onCheckOut = { reservationToConfirmAction = reservation to "CHECK_OUT" }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para Check-In o Check-Out
    reservationToConfirmAction?.let { (reservation, action) ->
        val isCheckIn = action == "CHECK_IN"
        AlertDialog(
            onDismissRequest = { reservationToConfirmAction = null },
            title = {
                Text(if (isCheckIn) "¿Registrar ingreso del vehículo?" else "¿Registrar salida del vehículo?")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Vehículo: ${reservation.vehicleLicensePlate ?: "Sin placa"}",
                        fontWeight = FontWeight.Bold
                    )
                    Text("Espacio asignado: ${reservation.spaceNumber ?: "N/A"}")
                    Text("Código: ${reservation.confirmationCode ?: "PKO-${reservation.id}"}")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isCheckIn)
                            "El vehículo ingresará a la cochera y el espacio pasará a estado OCUPADO."
                        else
                            "El vehículo saldrá de la cochera y el espacio se liberará a DISPONIBLE.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = reservation.id
                        reservationToConfirmAction = null
                        if (isCheckIn) {
                            viewModel.checkIn(id)
                        } else {
                            viewModel.checkOut(id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCheckIn) extended.accent else extended.signalGreen
                    )
                ) {
                    Text(
                        if (isCheckIn) "Confirmar Ingreso" else "Confirmar Salida",
                        color = if (isCheckIn) extended.onAccent else Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { reservationToConfirmAction = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun OperatorReservationCard(
    reservation: ReservationDto,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit
) {
    val extended = ParkeoTheme.colors
    val status = reservation.status

    val statusBadgeColor = when (status) {
        "ACTIVE" -> extended.signalGreen
        "CONFIRMED" -> extended.accent
        "PENDING" -> extended.signalAmber
        "COMPLETED" -> extended.textTertiary
        "CANCELLED", "NO_SHOW" -> extended.signalRed
        else -> extended.textSecondary
    }

    val statusLabel = when (status) {
        "ACTIVE" -> "En Cochera"
        "CONFIRMED" -> "Confirmada"
        "PENDING" -> "Pendiente"
        "COMPLETED" -> "Completada"
        "CANCELLED" -> "Cancelada"
        "NO_SHOW" -> "No asistió"
        else -> status
    }

    ParkeoCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Dimens.spacingMd),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cabecera: Placa y Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge de placa peruana
                    Surface(
                        color = extended.surface3,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, extended.borderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "PERÚ",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = extended.signalRed,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = reservation.vehicleLicensePlate ?: "SIN PLACA",
                                style = Typography.MonospaceTechnical,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = extended.textPrimary
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Espacio: ${reservation.spaceNumber ?: "Automático"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = extended.textPrimary
                        )
                        Text(
                            text = reservation.confirmationCode ?: "PKO-${reservation.id}",
                            style = Typography.MonospaceTechnical,
                            fontSize = 11.sp,
                            color = extended.textTertiary
                        )
                    }
                }

                // Badge de estado
                Surface(
                    color = statusBadgeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = statusLabel,
                        color = statusBadgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Divider(color = extended.borderSubtle, thickness = 0.5.dp)

            // Fechas y montos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "PROGRAMADO",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = extended.textTertiary
                    )
                    Text(
                        text = "${formatIso(reservation.startTime)} - ${formatIso(reservation.endTime)}",
                        style = Typography.MonospaceTechnical,
                        fontSize = 12.sp,
                        color = extended.textPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "MONTO",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = extended.textTertiary
                    )
                    Text(
                        text = "S/ ${String.format(java.util.Locale.US, "%.2f", reservation.totalPrice ?: 0.0)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = extended.accent
                    )
                }
            }

            // Botones de acción según estado
            when (status) {
                "PENDING", "CONFIRMED" -> {
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onCheckIn,
                        colors = ButtonDefaults.buttonColors(containerColor = extended.accent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Registrar Ingreso (Check-In)",
                            color = extended.onAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
                "ACTIVE" -> {
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onCheckOut,
                        colors = ButtonDefaults.buttonColors(containerColor = extended.signalGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Icon(Icons.Filled.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Registrar Salida (Check-Out)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatIso(iso: String?): String {
    if (iso.isNullOrBlank()) return "--:--"
    return try {
        val dt = LocalDateTime.parse(iso.replace("Z", ""))
        dt.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        iso.takeLast(8).take(5)
    }
}
