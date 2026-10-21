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
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val countPorIngresar = remember(uiState.reservations) {
        uiState.reservations.count { it.status in listOf("PENDING", "CONFIRMED") }
    }
    val countEnCochera = remember(uiState.reservations) {
        uiState.reservations.count { it.status == "ACTIVE" }
    }
    val countFinalizadas = remember(uiState.reservations) {
        uiState.reservations.count { it.status in listOf("COMPLETED", "CANCELLED", "NO_SHOW") }
    }
    val countTodas = remember(uiState.reservations) {
        uiState.reservations.size
    }
    val tabLabels = listOf(
        "Por Ingresar ($countPorIngresar)",
        "En Cochera ($countEnCochera)",
        "Finalizadas ($countFinalizadas)",
        "Todas ($countTodas)"
    )
    var searchQuery by remember { mutableStateOf("") }

    var reservationToConfirmAction by remember { mutableStateOf<Pair<ReservationDto, String>?>(null) }
    var showCodeLookup by remember { mutableStateOf(false) }
    var showWalkIn by remember { mutableStateOf(false) }

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

            // Acciones rápidas: búsqueda por código PKO y walk-in
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingMd, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
            ) {
                ParkeoButton(
                    text = "Código PKO",
                    leadingIcon = Icons.Filled.QrCodeScanner,
                    onClick = {
                        viewModel.clearCodeLookup()
                        showCodeLookup = true
                    },
                    style = ParkeoButtonStyle.Secondary,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                )

                ParkeoButton(
                    text = "Walk-in",
                    leadingIcon = Icons.Filled.GroupAdd,
                    onClick = {
                        viewModel.clearCodeLookup()
                        showWalkIn = true
                    },
                    style = ParkeoButtonStyle.Primary,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                )
            }

            // Pestañas de estado con contadores dinámicos
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = extended.surface1,
                contentColor = extended.accent
            ) {
                tabLabels.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
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

    // Diálogos: búsqueda por código PKO y registro walk-in
    if (showCodeLookup) {
        CodeLookupDialog(
            viewModel = viewModel,
            onDismiss = { showCodeLookup = false }
        )
    }

    if (showWalkIn) {
        WalkInDialog(
            viewModel = viewModel,
            onDismiss = { showWalkIn = false }
        )
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
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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

// ─── Diálogo: buscar reserva por código PKO ───────────────────────────────────

@Composable
private fun CodeLookupDialog(
    viewModel: OperatorViewModel,
    onDismiss: () -> Unit
) {
    val lookup by viewModel.codeLookup.collectAsState()
    val extended = ParkeoTheme.colors
    var code by remember { mutableStateOf("") }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = {
            viewModel.clearCodeLookup()
            onDismiss()
        },
        containerColor = extended.surface1,
        titleContentColor = extended.textPrimary,
        textContentColor = extended.textSecondary,
        title = {
            Text(
                text = "Buscar por código PKO",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                // Prefijo "PKO-" fijo + sufijo de solo dígitos/letras (sin guiones,
                // para evitar problemas con el autocorrector del teclado).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PKO-",
                        style = Typography.MonospaceTechnical,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = extended.accent
                    )
                    ParkeoTextField(
                        value = code,
                        onValueChange = { input ->
                            code = input.uppercase().filter { it.isLetterOrDigit() }.take(8)
                        },
                        placeholder = "XXXXXXXX",
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                when {
                    lookup.isSearching -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = extended.accent
                            )
                            Text("Buscando…", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    lookup.error != null -> {
                        val errorText = lookup.error.orEmpty()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.ErrorOutline, null,
                                tint = extended.signalRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                errorText,
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.signalRed
                            )
                        }
                    }

                    lookup.result != null -> {
                        val found = lookup.result!!
                        val statusLabel = when (found.status) {
                            "PENDING" -> "Pendiente"
                            "CONFIRMED" -> "Confirmada"
                            "ACTIVE" -> "En cochera"
                            "COMPLETED" -> "Completada"
                            "CANCELLED" -> "Cancelada"
                            "NO_SHOW" -> "No se presentó"
                            else -> found.status
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = extended.surface2,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                Dimens.borderHairline, extended.accent.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            found.parkingLotName,
                                            fontWeight = FontWeight.Bold,
                                            color = extended.textPrimary,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            "Espacio ${found.spaceNumber?.ifBlank { "a asignar" } ?: "a asignar"} • ${formatIso(found.startTime)} - ${formatIso(found.endTime)}",
                                            style = Typography.MonospaceTechnical,
                                            fontSize = 11.sp,
                                            color = extended.textSecondary
                                        )
                                    }
                                    Surface(
                                        color = extended.accent.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            statusLabel,
                                            color = extended.accent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        Text(
                            "Escríbelo tal como aparece en el boarding pass del conductor (o pídele que lo muestre en su QR).",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textTertiary
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (lookup.result == null) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(
                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                        )
                        viewModel.findReservationByCode("PKO-" + code)

                    },
                    enabled = code.length >= 4 && !lookup.isSearching,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extended.accent,
                        contentColor = extended.onAccent
                    )
                ) {
                    Text("Buscar")
                }
            } else {
                val found = lookup.result!!
                val canCheckIn = found.status in listOf("PENDING", "CONFIRMED")
                val canCheckOut = found.status == "ACTIVE"
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        canCheckIn -> Button(
                            onClick = {
                                viewModel.checkIn(found.id)
                                viewModel.clearCodeLookup()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = extended.accent,
                                contentColor = extended.onAccent
                            )
                        ) { Text("Check-In") }

                        canCheckOut -> Button(
                            onClick = {
                                viewModel.checkOut(found.id)
                                viewModel.clearCodeLookup()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = extended.signalGreen,
                                contentColor = Color.White
                            )
                        ) { Text("Check-Out") }

                        else -> TextButton(onClick = {
                            viewModel.clearCodeLookup()
                            onDismiss()
                        }) { Text("Listo") }
                    }

                    TextButton(onClick = {
                        viewModel.clearCodeLookup()
                        onDismiss()
                    }) { Text("Cerrar") }
                }
            }
        },
        dismissButton = {}
    )
}

// ─── Diálogo: registro walk-in (sin reserva previa) ───────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalkInDialog(
    viewModel: OperatorViewModel,
    onDismiss: () -> Unit
) {
    val types by viewModel.vehicleTypes.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val lookup by viewModel.codeLookup.collectAsState()
    val extended = ParkeoTheme.colors

    var selectedTypeId by remember(types) {
        mutableStateOf(types.firstOrNull()?.id)
    }
    var selectedSpaceId by remember { mutableStateOf<Long?>(null) } // null = asignación automática
    var expandedTypeMenu by remember { mutableStateOf(false) }

    val freeSpaces = remember(uiState.spaces, selectedTypeId) {
        uiState.spaces.filter {
            it.status == "AVAILABLE" &&
                    it.vehicleTypeId == selectedTypeId
        }
    }

    AlertDialog(
        onDismissRequest = {
            viewModel.clearCodeLookup()
            onDismiss()
        },
        containerColor = extended.surface1,
        titleContentColor = extended.textPrimary,
        textContentColor = extended.textSecondary,
        title = {
            Text(
                text = "Ingreso walk-in",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                Text(
                    text = "Registra un vehículo en ${uiState.selectedLot?.name ?: "la cochera"} sin reserva previa. El espacio queda ocupado por 1 hora y su salida se cierra con Check-Out.",
                    style = MaterialTheme.typography.bodySmall
                )

                if (lookup.isSearching) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = extended.accent
                        )
                        Text("Registrando…", style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (lookup.error != null) {
                    val errorText = lookup.error.orEmpty()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.ErrorOutline, null,
                            tint = extended.signalRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            errorText,
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.signalRed
                        )
                    }
                }

                if (lookup.result != null) {
                    val result = lookup.result!!
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = extended.signalGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            Dimens.borderHairline, extended.signalGreen.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle, null,
                                tint = extended.signalGreen,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Vehículo registrado",
                                fontWeight = FontWeight.Bold,
                                color = extended.signalGreen
                            )
                            Text(
                                text = "Reserva ${result.confirmationCode ?: "PKO-${result.id}"} • Espacio ${result.spaceNumber?.ifBlank { "asignado" } ?: "asignado"}",
                                style = Typography.MonospaceTechnical,
                                fontSize = 12.sp,
                                color = extended.textSecondary
                            )
                        }
                    }
                } else {
                    // Tipo de vehículo
                    ExposedDropdownMenuBox(
                        expanded = expandedTypeMenu,
                        onExpandedChange = { expandedTypeMenu = it }
                    ) {
                        ParkeoTextField(
                            value = types.firstOrNull { it.id == selectedTypeId }?.name ?: "Sin tipo",
                            onValueChange = {},
                            readOnly = true,
                            label = "Tipo de vehículo",
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedTypeMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedTypeMenu,
                            onDismissRequest = { expandedTypeMenu = false }
                        ) {
                            types.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = {
                                        selectedTypeId = type.id
                                        selectedSpaceId = null
                                        expandedTypeMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Espacio concreto (opcional)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            SpaceChipSmall(
                                label = "Auto",
                                isSelected = selectedSpaceId == null,
                                onClick = { selectedSpaceId = null }
                            )
                        }
                        items(freeSpaces, key = { it.id }) { space ->
                            SpaceChipSmall(
                                label = space.spaceNumber,
                                isSelected = selectedSpaceId == space.id,
                                onClick = { selectedSpaceId = space.id }
                            )
                        }
                    }
                    if (freeSpaces.isEmpty()) {
                        Text(
                            "No hay espacios AVAILABLE para ese tipo: se asignará uno automáticamente si existe.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = extended.signalAmber
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (lookup.result == null) {
                Button(
                    onClick = {
                        selectedTypeId?.let { typeId ->
                            viewModel.createWalkIn(
                                vehicleTypeId = typeId,
                                parkingSpaceId = selectedSpaceId,
                                notes = "WALK-IN en puerta"
                            )
                        }
                    },
                    enabled = selectedTypeId != null && !lookup.isSearching,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extended.accent,
                        contentColor = extended.onAccent
                    )
                ) {
                    Text("Registrar ingreso")
                }
            } else {
                Button(
                    onClick = {
                        viewModel.clearCodeLookup()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = extended.accent,
                        contentColor = extended.onAccent
                    )
                ) {
                    Text("Listo")
                }
            }
        },
        dismissButton = {
            if (lookup.result == null) {
                TextButton(onClick = {
                    viewModel.clearCodeLookup()
                    onDismiss()
                }) { Text("Cancelar") }
            }
        }
    )
}

@Composable
private fun SpaceChipSmall(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val extended = ParkeoTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) extended.accent.copy(alpha = 0.16f) else extended.surface2,
        border = androidx.compose.foundation.BorderStroke(
            Dimens.borderHairline,
            if (isSelected) extended.accent else extended.borderSubtle
        )
    ) {
        Text(
            text = label,
            style = Typography.MonospaceTechnical,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) extended.accent else extended.textPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
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
