package pe.parkeo.ui.screens.reservation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.data.remote.dto.ReservationDto
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.ReservationViewModel
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
    var reservationToRate by remember { mutableStateOf<ReservationDto?>(null) }

    LaunchedEffect(selectedTab) {
        viewModel.loadReservations(statusFilters[selectedTab])
    }

    // Re-agendar recordatorios al cargar la pestaña de próximas
    // (por si el app murió / los workers se eliminaron)
    LaunchedEffect(uiState.reservations) {
        if (selectedTab == 0) {
            uiState.reservations
                .filter { it.status in listOf("PENDING", "CONFIRMED") }
                .forEach { reservation ->
                    ReminderScheduler.schedule(
                        context = context,
                        reservationId = reservation.id,
                        parkingLotName = reservation.parkingLotName.ifBlank { "tu cochera" },
                        startIso = reservation.startTime,
                        endIso = reservation.endTime
                    )
                }
        }
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
                    ParkeoSkeletonList(
                        itemCount = 4,
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(Dimens.spacingMd)
                    )
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
                                onCancelRequest = { reservationToCancel = reservation },
                                onRateRequest = { reservationToRate = reservation }
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

        // Rating dialog (completadas)
        reservationToRate?.let { reservation ->
            RateReservationDialog(
                reservation = reservation,
                onDismiss = { reservationToRate = null },
                onConfirm = { rating, comment ->
                    viewModel.rateReservation(reservation.id, rating, comment)
                    reservationToRate = null
                }
            )
        }
    }
}

@Composable
private fun ReservationItemCard(
    reservation: ReservationDto,
    onOpenBoardingPass: () -> Unit,
    onCancelRequest: () -> Unit,
    onRateRequest: () -> Unit
) {
    val extended = ParkeoTheme.colors
    val canCancel = reservation.status in listOf("PENDING", "CONFIRMED")
    val isActive = reservation.status == "ACTIVE"
    val isCompleted = reservation.status == "COMPLETED"

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
                    text = "Boarding Pass",
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

                if (isCompleted) {
                    ParkeoButton(
                        text = "Calificar",
                        onClick = onRateRequest,
                        style = ParkeoButtonStyle.Ghost,
                        modifier = Modifier.height(38.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RateReservationDialog(
    reservation: ReservationDto,
    onDismiss: () -> Unit,
    onConfirm: (Int, String?) -> Unit
) {
    val extended = ParkeoTheme.colors
    var selectedStars by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extended.surface1,
        titleContentColor = extended.textPrimary,
        textContentColor = extended.textSecondary,
        title = {
            Text(
                text = "¿Cómo estuvo \"${reservation.parkingLotName}\"?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tu opinión ayuda a otros conductores y a mejorar la cochera.",
                    style = MaterialTheme.typography.bodySmall
                )

                // Estrellas 1-5
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { selectedStars = star },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (star <= selectedStars) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "$star estrellas",
                                tint = if (star <= selectedStars) extended.accent else extended.textTertiary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                TextField(
                    value = comment,
                    onValueChange = { comment = it.take(300) },
                    placeholder = { Text("Comentario (opcional)") },
                    minLines = 2,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = extended.surface2,
                        unfocusedContainerColor = extended.surface2,
                        focusedIndicatorColor = extended.accent,
                        unfocusedIndicatorColor = extended.borderSubtle,
                        cursorColor = extended.accent,
                        focusedTextColor = extended.textPrimary,
                        unfocusedTextColor = extended.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            ParkeoButton(
                text = "Enviar calificación",
                onClick = {
                    onConfirm(
                        selectedStars,
                        comment.takeIf { it.isNotBlank() }
                    )
                },
                style = ParkeoButtonStyle.Primary
            )
        },
        dismissButton = {
            ParkeoButton(
                text = "Cerrar",
                onClick = onDismiss,
                style = ParkeoButtonStyle.Ghost
            )
        }
    )
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
