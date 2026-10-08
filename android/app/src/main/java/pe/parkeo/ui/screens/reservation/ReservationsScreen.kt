package pe.parkeo.ui.screens.reservation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import pe.parkeo.data.remote.dto.ReservationDto
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.ReservationViewModel
import pe.parkeo.util.DirectionsButton
import pe.parkeo.util.ReminderScheduler

@Composable
fun ReservationsScreen(
    viewModel: ReservationViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Próximas", "Activas", "Historial")
    val statusFilters = listOf("PENDING,CONFIRMED", "ACTIVE", "COMPLETED,CANCELLED,NO_SHOW")

    var boardingPassReservation by remember { mutableStateOf<ReservationDto?>(null) }
    var reservationToCancel by remember { mutableStateOf<ReservationDto?>(null) }

    LaunchedEffect(selectedTab) {
        viewModel.loadReservations(statusFilters[selectedTab])
    }

    // Cuando el ViewModel confirma una cancelación, se apagan sus recordatorios
    LaunchedEffect(uiState.cancelledReservationId) {
        uiState.cancelledReservationId?.let { id ->
            ReminderScheduler.cancel(context, id)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = extended.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            ParkeoTopBar(
                title = "Mis reservas",
                onNavigationClick = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Sleek Tab Bar
            Surface(
                color = extended.surface1,
                border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, extended.borderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.spacingMd, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = index },
                            color = if (isSelected) extended.accent else extended.surface2,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                Dimens.borderHairline,
                                if (isSelected) extended.accent else extended.borderSubtle
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) extended.onAccent else extended.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        ParkeoLoadingView(message = "Cargando tus reservas...")
                    }
                }

                uiState.reservations.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        ParkeoEmptyState(
                            title = "No hay reservas",
                            subtitle = "No encontramos reservas en la sección \"${tabs[selectedTab]}\"",
                            icon = Icons.Filled.BookmarkBorder
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(Dimens.spacingMd),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                    ) {
                        items(uiState.reservations, key = { it.id }) { reservation ->
                            ReservationItemCard(
                                reservation = reservation,
                                onOpenBoardingPass = { boardingPassReservation = reservation },
                                onCancelRequest = { reservationToCancel = reservation }
                            )
                        }
                    }
                }
            }
        }

        // Digital Boarding Pass Ticket Dialog
        boardingPassReservation?.let { reservation ->
            TicketBoardingPassDialog(
                reservation = reservation,
                onDismiss = { boardingPassReservation = null }
            )
        }

        // Cancellation Confirmation Dialog
        reservationToCancel?.let { reservation ->
            CancelReservationDialog(
                reservation = reservation,
                onDismiss = { reservationToCancel = null },
                onConfirm = { reason ->
                    viewModel.cancelReservation(reservation.id, reason)
                    reservationToCancel = null
                }
            )
        }
    }
}

@Composable
private fun ReservationItemCard(
    reservation: ReservationDto,
    onOpenBoardingPass: () -> Unit,
    onCancelRequest: () -> Unit
) {
    val extended = ParkeoTheme.colors
    val canCancel = reservation.status in listOf("PENDING", "CONFIRMED")
    val isActive = reservation.status == "ACTIVE"

    val badgeStatus = when (reservation.status) {
        "PENDING" -> ParkeoBadgeStatus.Reserved
        "CONFIRMED" -> ParkeoBadgeStatus.Reserved
        "ACTIVE" -> ParkeoBadgeStatus.Available
        "COMPLETED" -> ParkeoBadgeStatus.Closed
        "CANCELLED" -> ParkeoBadgeStatus.Occupied
        else -> ParkeoBadgeStatus.Closed
    }

    ParkeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenBoardingPass),
        accentBorder = isActive
    ) {
        Column(
            modifier = Modifier.padding(Dimens.spacingLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
        ) {
            // Header Row: Lot name + Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reservation.parkingLotName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = extended.textPrimary
                    )
                    Text(
                        text = reservation.parkingLotAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textSecondary
                    )
                }

                ParkeoBadge(
                    status = badgeStatus,
                    labelOverride = when (reservation.status) {
                        "PENDING" -> "Pendiente"
                        "CONFIRMED" -> "Confirmada"
                        "ACTIVE" -> "En curso"
                        "COMPLETED" -> "Completada"
                        "CANCELLED" -> "Cancelada"
                        "NO_SHOW" -> "No se presentó"
                        else -> reservation.status
                    }
                )
            }

            // Monospace Confirmation Code Tag
            reservation.confirmationCode?.let { code ->
                Surface(
                    color = extended.surface2,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, extended.accent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCode2,
                            contentDescription = null,
                            tint = extended.accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = code,
                            style = Typography.MonospaceTechnical,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = extended.accent
                        )
                    }
                }
            }

            HorizontalDivider(
                color = extended.borderSubtle,
                thickness = Dimens.borderHairline,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Technical details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = extended.accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = formatDisplayDate(reservation.startTime),
                            style = Typography.MonospaceTechnical,
                            fontSize = 12.sp,
                            color = extended.textPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsCar,
                            contentDescription = null,
                            tint = extended.textTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = reservation.vehicleLicensePlate,
                            style = Typography.MonospaceTechnical,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = extended.accent
                        )
                    }
                }

                reservation.totalPrice?.let { price ->
                    Text(
                        text = "S/ ${String.format(java.util.Locale.US, "%.2f", price)}",
                        style = Typography.MonospaceTechnical,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = extended.accent,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Actions Row
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ParkeoButton(
                    text = "Ver Boarding Pass QR",
                    onClick = onOpenBoardingPass,
                    style = ParkeoButtonStyle.Secondary,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                )

                if (canCancel) {
                    ParkeoButton(
                        text = "Cancelar",
                        onClick = onCancelRequest,
                        style = ParkeoButtonStyle.Destructive,
                        modifier = Modifier.height(38.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TicketBoardingPassDialog(
    reservation: ReservationDto,
    onDismiss: () -> Unit
) {
    val extended = ParkeoTheme.colors
    val code = reservation.confirmationCode ?: "PKO-${reservation.id}"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = extended.surface1,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, extended.accent.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.spacingLg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ConfirmationNumber,
                            contentDescription = null,
                            tint = extended.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "BOARDING PASS DIGITAL",
                            style = Typography.MonospaceTechnical,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = extended.accent,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = extended.textTertiary
                        )
                    }
                }

                // QR Code Presentation Box
                Surface(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QrCodeMatrix(
                            content = code,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Monospace code display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CÓDIGO DE RESERVA",
                        style = Typography.MonospaceTechnical,
                        fontSize = 10.sp,
                        color = extended.textTertiary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = code,
                        style = Typography.MonospaceTechnical,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = extended.accent,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Muestra este código al operador en puerta",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = extended.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                HorizontalDivider(
                    color = extended.borderSubtle,
                    thickness = Dimens.borderHairline
                )

                // Details Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BoardingPassRow(label = "Estacionamiento", value = reservation.parkingLotName)
                    BoardingPassRow(label = "Dirección", value = reservation.parkingLotAddress)
                    BoardingPassRow(
                        label = "Espacio asignado",
                        value = if (reservation.spaceNumber.isNotBlank()) reservation.spaceNumber else "En puerta",
                        isHighlight = true
                    )
                    BoardingPassRow(
                        label = "Placa del vehículo",
                        value = reservation.vehicleLicensePlate,
                        isHighlight = true
                    )
                    BoardingPassRow(label = "Inicio", value = formatDisplayDate(reservation.startTime))
                    BoardingPassRow(label = "Salida programada", value = formatDisplayDate(reservation.endTime))
                    reservation.totalPrice?.let { price ->
                        BoardingPassRow(
                            label = "Tarifa estimada",
                            value = "S/ ${String.format(java.util.Locale.US, "%.2f", price)}",
                            isHighlight = true
                        )
                    }
                }

                // Route action if available
                val lat = reservation.parkingLotLatitude
                val lng = reservation.parkingLotLongitude
                if (lat != null && lng != null) {
                    Spacer(Modifier.height(4.dp))
                    DirectionsButton(latitude = lat, longitude = lng)
                }

                ParkeoButton(
                    text = "Cerrar",
                    onClick = onDismiss,
                    style = ParkeoButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BoardingPassRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    val extended = ParkeoTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = extended.textSecondary
        )
        Text(
            text = value,
            style = if (isHighlight) Typography.MonospaceTechnical else MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) extended.accent else extended.textPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun CancelReservationDialog(
    reservation: ReservationDto,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val extended = ParkeoTheme.colors
    val reasons = listOf(
        "Cambio de planes",
        "Encontré otro estacionamiento",
        "Error en la fecha u horario",
        "Otro motivo"
    )
    var selectedReason by remember { mutableStateOf(reasons[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extended.surface1,
        titleContentColor = extended.textPrimary,
        textContentColor = extended.textSecondary,
        title = {
            Text(
                text = "¿Cancelar reserva?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Estás a punto de cancelar tu reserva en \"${reservation.parkingLotName}\". Selecciona el motivo:",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textSecondary
                )

                reasons.forEach { reason ->
                    val isSelected = reason == selectedReason
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedReason = reason },
                        color = if (isSelected) extended.accent.copy(alpha = 0.15f) else extended.surface2,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            Dimens.borderHairline,
                            if (isSelected) extended.accent else extended.borderSubtle
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = extended.textPrimary
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedReason = reason },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = extended.accent,
                                    unselectedColor = extended.textTertiary
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            ParkeoButton(
                text = "Confirmar Cancelación",
                onClick = { onConfirm(selectedReason) },
                style = ParkeoButtonStyle.Destructive
            )
        },
        dismissButton = {
            ParkeoButton(
                text = "Regresar",
                onClick = onDismiss,
                style = ParkeoButtonStyle.Ghost
            )
        }
    )
}

@Composable
private fun QrCodeMatrix(
    content: String,
    modifier: Modifier = Modifier
) {
    val gridSize = 21
    val bitMatrix = remember(content) {
        val matrix = Array(gridSize) { BooleanArray(gridSize) }

        // Finder patterns (7x7 con centro 3x3) en las 3 esquinas
        fun markFinder(startR: Int, startC: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    matrix[startR + r][startC + c] = isBorder || isCenter
                }
            }
        }
        markFinder(0, 0)
        markFinder(0, gridSize - 7)
        markFinder(gridSize - 7, 0)

        // Líneas de sincronización (timing patterns)
        for (i in 7 until gridSize - 7) {
            matrix[6][i] = (i % 2 == 0)
            matrix[i][6] = (i % 2 == 0)
        }

        // Datos generados determinísticamente a partir del hash del código
        var hash = content.hashCode()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val inFinder1 = r < 8 && c < 8
                val inFinder2 = r < 8 && c >= gridSize - 8
                val inFinder3 = r >= gridSize - 8 && c < 8
                val inTiming = r == 6 || c == 6
                if (!inFinder1 && !inFinder2 && !inFinder3 && !inTiming) {
                    hash = (hash * 31 + r * 17 + c * 23)
                    matrix[r][c] = (hash and 1) == 1
                }
            }
        }
        matrix
    }

    Canvas(modifier = modifier) {
        val cellSize = size.minDimension / gridSize
        val paddingX = (size.width - cellSize * gridSize) / 2f
        val paddingY = (size.height - cellSize * gridSize) / 2f

        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (bitMatrix[r][c]) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(paddingX + c * cellSize, paddingY + r * cellSize),
                        size = Size(cellSize * 0.95f, cellSize * 0.95f)
                    )
                }
            }
        }
    }
}

private fun formatDisplayDate(raw: String): String {
    val clean = raw.take(16).replace("T", " ")
    return if (clean.length >= 10 && clean.contains("-")) {
        val parts = clean.split(" ")
        val ymd = parts[0].split("-")
        if (ymd.size == 3) {
            "${ymd[2]}/${ymd[1]} ${if (parts.size > 1) parts[1] else ""}".trim()
        } else clean
    } else clean
}