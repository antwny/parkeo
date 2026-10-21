package pe.parkeo.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.data.remote.dto.ReservationDto
import pe.parkeo.ui.components.*
import pe.parkeo.ui.screens.reservation.TicketBoardingPassDialog
import pe.parkeo.ui.screens.reservation.formatDisplayDate
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.AdminViewModel

private data class AdminReservationFilter(val label: String, val status: String?)

private val adminReservationFilters = listOf(
    AdminReservationFilter("Todas", null),
    AdminReservationFilter("Confirmadas", "CONFIRMED"),
    AdminReservationFilter("En curso", "ACTIVE"),
    AdminReservationFilter("Completadas", "COMPLETED"),
    AdminReservationFilter("Canceladas", "CANCELLED"),
    AdminReservationFilter("No-show", "NO_SHOW")
)

/**
 * Vista global de reservas del sistema para el administrador, con filtro por estado.
 * Se abre desde el dashboard (quick action) o desde el perfil; no es una pestaña del bottom bar.
 */
@Composable
fun AdminReservationsScreen(
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors

    var selectedFilter by remember { mutableStateOf(adminReservationFilters.first()) }
    var boardingPassReservation by remember { mutableStateOf<ReservationDto?>(null) }

    // Carga al entrar y al cambiar filtro
    LaunchedEffect(selectedFilter) {
        viewModel.loadAdminReservations(selectedFilter.status)
    }

    Scaffold(
        containerColor = extended.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            ParkeoTopBar(
                title = "Reservas de la red",
                onNavigationClick = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filtros por estado
            Surface(
                color = extended.surface1,
                border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, extended.borderSubtle)
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.spacingMd, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    items(adminReservationFilters) { filter ->
                        val isSelected = filter == selectedFilter
                        Surface(
                            onClick = { selectedFilter = filter },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) extended.accent else extended.surface2,
                            border = androidx.compose.foundation.BorderStroke(
                                Dimens.borderHairline,
                                if (isSelected) extended.accent else extended.borderSubtle
                            )
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) extended.onAccent else extended.textSecondary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Resumen compacto
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedFilter.label.uppercase(),
                    style = Typography.MonospaceTechnical,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = extended.textTertiary
                )
                Text(
                    text = "${uiState.adminReservations.size} ${if (uiState.adminReservations.size == 1) "registro" else "registros"}",
                    style = Typography.MonospaceTechnical,
                    fontSize = 11.sp,
                    color = extended.accent
                )
            }

            when {
                uiState.isLoadingReservations -> {
                    ParkeoSkeletonList(
                        itemCount = 4,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Dimens.spacingMd)
                    )
                }

                uiState.adminReservations.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ParkeoEmptyState(
                            title = "Sin reservas",
                            subtitle = "No hay reservas con el estado \"${selectedFilter.label}\"",
                            icon = Icons.Filled.EventBusy
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Dimens.spacingMd),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                    ) {
                        items(uiState.adminReservations, key = { it.id }) { reservation ->
                            AdminReservationCard(
                                reservation = reservation,
                                onOpenBoardingPass = { boardingPassReservation = reservation }
                            )
                        }
                    }
                }
            }
        }

        boardingPassReservation?.let { reservation ->
            TicketBoardingPassDialog(
                reservation = reservation,
                onDismiss = { boardingPassReservation = null }
            )
        }
    }
}

@Composable
private fun AdminReservationCard(
    reservation: ReservationDto,
    onOpenBoardingPass: () -> Unit
) {
    val extended = ParkeoTheme.colors
    val isActive = reservation.status == "ACTIVE"

    val badgeStatus = when (reservation.status) {
        "PENDING", "CONFIRMED" -> ParkeoBadgeStatus.Reserved
        "ACTIVE" -> ParkeoBadgeStatus.Available
        "COMPLETED" -> ParkeoBadgeStatus.Closed
        "CANCELLED" -> ParkeoBadgeStatus.Occupied
        "NO_SHOW" -> ParkeoBadgeStatus.Occupied
        else -> ParkeoBadgeStatus.Closed
    }

    val statusLabel = when (reservation.status) {
        "PENDING" -> "Pendiente"
        "CONFIRMED" -> "Confirmada"
        "ACTIVE" -> "En curso"
        "COMPLETED" -> "Completada"
        "CANCELLED" -> "Cancelada"
        "NO_SHOW" -> "No se presentó"
        else -> reservation.status
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

                ParkeoBadge(status = badgeStatus, labelOverride = statusLabel)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                reservation.confirmationCode?.let { code ->
                    Text(
                        text = code,
                        style = Typography.MonospaceTechnical,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = extended.accent
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        tint = extended.textTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = reservation.vehicleLicensePlate,
                        style = Typography.MonospaceTechnical,
                        fontSize = 12.sp,
                        color = extended.textPrimary
                    )
                }
            }

            androidx.compose.material3.HorizontalDivider(
                color = extended.borderSubtle,
                thickness = Dimens.borderHairline,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatDisplayDate(reservation.startTime)} → ${formatDisplayDate(reservation.endTime)}",
                    style = Typography.MonospaceTechnical,
                    fontSize = 11.sp,
                    color = extended.textSecondary
                )

                reservation.totalPrice?.let { price ->
                    Text(
                        text = "S/ ${String.format(java.util.Locale.US, "%.2f", price)}",
                        style = Typography.MonospaceTechnical,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = extended.accent
                    )
                }
            }

            ParkeoButton(
                text = "Ver boarding pass",
                onClick = onOpenBoardingPass,
                style = ParkeoButtonStyle.Secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            )
        }
    }
}
