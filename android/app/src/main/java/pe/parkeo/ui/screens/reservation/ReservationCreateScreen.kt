package pe.parkeo.ui.screens.reservation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.ParkingDetailViewModel
import pe.parkeo.ui.viewmodel.ReservationViewModel
import pe.parkeo.ui.viewmodel.VehicleViewModel
import pe.parkeo.util.ReminderScheduler
import pe.parkeo.util.RequestNotificationPermission
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    val extended = ParkeoTheme.colors
    val context = LocalContext.current

    // Pide el permiso de notificaciones (Android 13+) para los recordatorios de la reserva
    RequestNotificationPermission()

    var selectedVehicleId by remember { mutableStateOf<Long?>(null) }
    var durationHours by remember { mutableIntStateOf(2) }

    // Hora de Lima, sin segundos ni nanosegundos: el backend espera yyyy-MM-dd'T'HH:mm:ss
    val startTime = remember {
        LocalDateTime.now(ZoneId.of("America/Lima"))
            .plusHours(1)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
    }
    val endTime = startTime.plusHours(durationHours.toLong())
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

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
            // Recordatorios locales: 15 min antes del inicio y 10 min antes del fin
            reservationState.currentReservation?.let { r ->
                ReminderScheduler.schedule(
                    context = context,
                    reservationId = r.id,
                    parkingLotName = r.parkingLotName,
                    startIso = r.startTime,
                    endIso = r.endTime
                )
            }
            reservationViewModel.clearMessages()
            onReservationCreated()
        }
    }

    Scaffold(
        containerColor = extended.background,
        topBar = {
            ParkeoTopBar(
                title = "Nueva reserva",
                onNavigationClick = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.spacingMd),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
        ) {
            // Cochera Seleccionada Card
            parkingState.parkingLot?.let { parking ->
                ParkeoCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(Dimens.spacingLg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(extended.accent.copy(alpha = 0.12f), CircleShape)
                                .border(Dimens.borderHairline, extended.accent.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalParking,
                                contentDescription = null,
                                tint = extended.accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = parking.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = extended.textPrimary
                            )
                            Text(
                                text = parking.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.textSecondary
                            )
                        }
                    }
                }
            }

            // Duración Estimada Card
            ParkeoCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(Dimens.spacingLg),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                ) {
                    Text(
                        text = "TIEMPO ESTIMADO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = extended.textTertiary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3, 4, 8, 12).forEach { hours ->
                            val isSelected = durationHours == hours
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(ParkeoCardShape)
                                    .clickable { durationHours = hours },
                                color = if (isSelected) extended.accent else extended.surface2,
                                shape = ParkeoCardShape,
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
                                        text = "${hours}h",
                                        style = Typography.MonospaceTechnical,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) extended.onAccent else extended.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(extended.surface2, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "INGRESO",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = extended.textTertiary
                            )
                            Text(
                                text = startTime.format(DateTimeFormatter.ofPattern("dd/MM HH:mm")),
                                style = Typography.MonospaceTechnical,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = extended.textPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "SALIDA PREVISTA",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = extended.textTertiary
                            )
                            Text(
                                text = endTime.format(DateTimeFormatter.ofPattern("dd/MM HH:mm")),
                                style = Typography.MonospaceTechnical,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = extended.textPrimary
                            )
                        }
                    }
                }
            }

            // Selección de Vehículo Card
            ParkeoCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(Dimens.spacingLg),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    Text(
                        text = "VEHÍCULO ASOCIADO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = extended.textTertiary
                    )

                    if (vehicleState.vehicles.isEmpty()) {
                        Text(
                            text = "No tienes vehículos registrados. Por favor agrega uno desde la pestaña de Vehículos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.signalRed
                        )
                    } else {
                        vehicleState.vehicles.forEach { vehicle ->
                            val isSelected = selectedVehicleId == vehicle.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(ParkeoCardShape)
                                    .clickable { selectedVehicleId = vehicle.id },
                                color = if (isSelected) extended.accent.copy(alpha = 0.08f) else extended.surface2,
                                shape = ParkeoCardShape,
                                border = androidx.compose.foundation.BorderStroke(
                                    Dimens.borderHairline,
                                    if (isSelected) extended.accent else extended.borderSubtle
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedVehicleId = vehicle.id },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = extended.accent,
                                            unselectedColor = extended.textTertiary
                                        )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = vehicle.licensePlate,
                                            style = Typography.MonospaceTechnical,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = extended.textPrimary
                                        )
                                        Text(
                                            text = "${vehicle.brand ?: ""} ${vehicle.model ?: ""} • ${vehicle.vehicleType.name}".trim(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = extended.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Ticket Boarding Pass Summary
            parkingState.parkingLot?.let { parking ->
                val rate = parking.tariffs.firstOrNull()?.price ?: 5.0
                val estimatedTotal = rate * durationHours

                ParkeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = true
                ) {
                    Column(
                        modifier = Modifier.padding(Dimens.spacingLg),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL ESTIMADO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = extended.textTertiary
                                )
                                Text(
                                    text = "S/ ${String.format("%.2f", estimatedTotal)}",
                                    style = Typography.MonospaceTechnical,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = extended.accent
                                )
                            }

                            Surface(
                                color = extended.surface2,
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    Dimens.borderHairline,
                                    extended.borderSubtle
                                )
                            ) {
                                Text(
                                    text = "PAGO AL FINALIZAR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = extended.textSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Errores
            reservationState.error?.let { error ->
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
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = extended.signalRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = extended.signalRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(Dimens.spacingSm))

            // CTA de confirmación
            ParkeoButton(
                text = "Confirmar reserva",
                onClick = {
                    selectedVehicleId?.let { vehicleId ->
                        // Sin parkingSpaceId: el servidor asigna un espacio libre de este estacionamiento
                        reservationViewModel.createReservation(
                            parkingLotId = parkingId,
                            vehicleId = vehicleId,
                            startTime = formatter.format(startTime),
                            endTime = formatter.format(endTime)
                        )
                    }
                },
                isLoading = reservationState.isLoading,
                enabled = selectedVehicleId != null && vehicleState.vehicles.isNotEmpty(),
                leadingIcon = Icons.Filled.BookmarkAdd,
                style = ParkeoButtonStyle.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeightLarge)
            )

            Spacer(Modifier.height(Dimens.spacingLg))
        }
    }
}