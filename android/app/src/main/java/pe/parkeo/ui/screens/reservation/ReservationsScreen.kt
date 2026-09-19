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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.data.remote.dto.ReservationDto
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.ReservationViewModel

@Composable
fun ReservationsScreen(
    viewModel: ReservationViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Próximas", "Activas", "Historial")
    val statusFilters = listOf("PENDING,CONFIRMED", "ACTIVE", "COMPLETED,CANCELLED")

    LaunchedEffect(selectedTab) {
        viewModel.loadReservations(statusFilters[selectedTab])
    }

    Scaffold(
        containerColor = extended.background,
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
                                onCancel = { viewModel.cancelReservation(reservation.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationItemCard(
    reservation: ReservationDto,
    onCancel: () -> Unit
) {
    val extended = ParkeoTheme.colors
    val canCancel = reservation.status in listOf("PENDING", "CONFIRMED")

    val badgeStatus = when (reservation.status) {
        "PENDING" -> ParkeoBadgeStatus.Reserved
        "CONFIRMED" -> ParkeoBadgeStatus.Reserved
        "ACTIVE" -> ParkeoBadgeStatus.Available
        "COMPLETED" -> ParkeoBadgeStatus.Closed
        "CANCELLED" -> ParkeoBadgeStatus.Occupied
        else -> ParkeoBadgeStatus.Closed
    }

    ParkeoCard(
        modifier = Modifier.fillMaxWidth(),
        accentBorder = reservation.status == "ACTIVE"
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

                ParkeoBadge(
                    status = badgeStatus,
                    labelOverride = when (reservation.status) {
                        "PENDING" -> "Pendiente"
                        "CONFIRMED" -> "Confirmada"
                        "ACTIVE" -> "En curso"
                        "COMPLETED" -> "Completada"
                        "CANCELLED" -> "Cancelada"
                        else -> reservation.status
                    }
                )
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
                        val rawDate = reservation.startTime.take(16).replace("T", " ")
                        val displayDate = if (rawDate.length >= 10 && rawDate.contains("-")) {
                            val parts = rawDate.split(" ", "T")
                            val ymd = parts[0].split("-")
                            if (ymd.size == 3) {
                                "${ymd[2]}/${ymd[1]} ${if (parts.size > 1) parts[1] else ""}".trim()
                            } else rawDate
                        } else rawDate
                        Text(
                            text = displayDate,
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

            if (canCancel) {
                Spacer(Modifier.height(4.dp))
                ParkeoButton(
                    text = "Cancelar reserva",
                    onClick = onCancel,
                    style = ParkeoButtonStyle.Destructive,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                )
            }
        }
    }
}
