package pe.parkeo.ui.screens.reservation

import android.content.Intent
import android.net.Uri
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
import pe.parkeo.data.remote.dto.VehicleTypeDto
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

enum class ArrivalOption(val title: String, val subtitle: String, val minutesOffset: Long) {
    NOW("Inmediato", "+5 min", 5),
    IN_30("En 30 min", "+30 min", 30),
    IN_60("En 1 hora", "+60 min", 60),
    IN_120("En 2 horas", "+2h", 120),
    CUSTOM("Otra hora", "Manual", 0)
}

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
    val extended = ParkeoTheme.colors
    val context = LocalContext.current
    val limaZone = remember { ZoneId.of("America/Lima") }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss") }
    val displayFormatter = remember { DateTimeFormatter.ofPattern("dd/MM HH:mm") }

    // Solicitar permiso de notificaciones (Android 13+)
    RequestNotificationPermission()

    var selectedVehicleId by remember { mutableStateOf<Long?>(null) }
    var durationHours by remember { mutableIntStateOf(2) }
    var arrivalOption by remember { mutableStateOf(ArrivalOption.NOW) }
    var customMinutesOffset by remember { mutableLongStateOf(180L) } // 3h a futuro por defecto si es custom

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Calcular horas dinámicas para visualización en pantalla
    val plannedStartTime = remember(arrivalOption, customMinutesOffset) {
        val offset = if (arrivalOption == ArrivalOption.CUSTOM) customMinutesOffset else arrivalOption.minutesOffset
        LocalDateTime.now(limaZone).plusMinutes(offset).withSecond(0).withNano(0)
    }
    val plannedEndTime = plannedStartTime.plusHours(durationHours.toLong())

    // Encontrar vehículo seleccionado y su tarifa correspondiente
    val selectedVehicle = vehicleState.vehicles.find { it.id == selectedVehicleId }
    val matchingTariff = parkingState.parkingLot?.tariffs?.firstOrNull { tariff ->
        selectedVehicle != null && (
            tariff.vehicleType.equals(selectedVehicle.vehicleType.name, ignoreCase = true) ||
            tariff.vehicleType.contains(selectedVehicle.vehicleType.name, ignoreCase = true) ||
            selectedVehicle.vehicleType.name.contains(tariff.vehicleType, ignoreCase = true)
        )
    } ?: parkingState.parkingLot?.tariffs?.firstOrNull()

    val hourlyRate = matchingTariff?.price ?: 5.0
    val estimatedTotal = hourlyRate * durationHours

    LaunchedEffect(parkingId) {
        parkingViewModel.loadParkingLot(parkingId)
        vehicleViewModel.loadVehicles()
        vehicleViewModel.loadVehicleTypes()
    }

    LaunchedEffect(vehicleState.vehicles) {
        if (selectedVehicleId == null && vehicleState.vehicles.isNotEmpty()) {
            selectedVehicleId = vehicleState.vehicles.first().id
        }
    }

    LaunchedEffect(reservationState.reservationCreated) {
        if (reservationState.reservationCreated) {
            reservationState.currentReservation?.let { r ->
                ReminderScheduler.schedule(
                    context = context,
                    reservationId = r.id,
                    parkingLotName = r.parkingLotName,
                    startIso = r.startTime,
                    endIso = r.endTime
                )
            }
            showConfirmDialog = false
            showSuccessDialog = true
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
                                .size(48.dp)
                                .background(extended.accent.copy(alpha = 0.12f), CircleShape)
                                .border(Dimens.borderHairline, extended.accent.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalParking,
                                contentDescription = null,
                                tint = extended.accent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = parking.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = extended.textPrimary
                            )
                            Text(
                                text = "${parking.address}${parking.district?.let { " • $it" } ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.textSecondary
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = extended.signalGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${parking.availableSpaces} espacios libres",
                                        style = Typography.MonospaceTechnical,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = extended.signalGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Hora de Ingreso (Timing Options)
            ParkeoCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(Dimens.spacingLg),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HORA DE INGRESO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = extended.textTertiary
                        )
                        Text(
                            text = plannedStartTime.format(displayFormatter),
                            style = Typography.MonospaceTechnical,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = extended.accent
                        )
                    }

                    // Chips de selección de hora de llegada
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ArrivalOption.values().forEach { opt ->
                            val isSelected = arrivalOption == opt
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(ParkeoCardShape)
                                    .clickable { arrivalOption = opt },
                                color = if (isSelected) extended.accent else extended.surface2,
                                shape = ParkeoCardShape,
                                border = androidx.compose.foundation.BorderStroke(
                                    Dimens.borderHairline,
                                    if (isSelected) extended.accent else extended.borderSubtle
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = opt.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) extended.onAccent else extended.textPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = opt.subtitle,
                                        style = Typography.MonospaceTechnical,
                                        fontSize = 10.sp,
                                        color = if (isSelected) extended.onAccent.copy(alpha = 0.8f) else extended.textTertiary
                                    )
                                }
                            }
                        }
                    }

                    // Si seleccionó personalizado: control fino de minutos/horas
                    if (arrivalOption == ArrivalOption.CUSTOM) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(extended.surface2, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Llegar en +${customMinutesOffset / 60}h ${customMinutesOffset % 60}m",
                                style = Typography.MonospaceTechnical,
                                fontSize = 12.sp,
                                color = extended.textPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { if (customMinutesOffset > 15) customMinutesOffset -= 30 },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, "-30m", tint = extended.accent)
                                }
                                IconButton(
                                    onClick = { customMinutesOffset += 30 },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Add, "+30m", tint = extended.accent)
                                }
                            }
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
                        text = "TIEMPO DE ESTADÍA",
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
                                text = "INGRESO ESTIMADO",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = extended.textTertiary
                            )
                            Text(
                                text = plannedStartTime.format(displayFormatter),
                                style = Typography.MonospaceTechnical,
                                fontSize = 13.sp,
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
                                text = plannedEndTime.format(displayFormatter),
                                style = Typography.MonospaceTechnical,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = extended.accent
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VEHÍCULO ASOCIADO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = extended.textTertiary
                        )

                        TextButton(
                            onClick = {
                                vehicleViewModel.clearMessages()
                                showAddVehicleDialog = true
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = extended.accent
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Agregar",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = extended.accent
                            )
                        }
                    }

                    if (vehicleState.vehicles.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = extended.surface2,
                            shape = ParkeoCardShape,
                            border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, extended.borderSubtle)
                        ) {
                            Column(
                                modifier = Modifier.padding(Dimens.spacingMd),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Aún no tienes vehículos registrados",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.textSecondary
                                )
                                Spacer(Modifier.height(8.dp))
                                ParkeoButton(
                                    text = "Registrar vehículo ahora",
                                    onClick = {
                                        vehicleViewModel.clearMessages()
                                        showAddVehicleDialog = true
                                    },
                                    style = ParkeoButtonStyle.Primary,
                                    compact = true
                                )
                            }
                        }
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = vehicle.licensePlate,
                                            style = Typography.MonospaceTechnical,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = extended.textPrimary
                                        )
                                        Text(
                                            text = "${vehicle.brand ?: ""} ${vehicle.model ?: ""}".trim().ifBlank { "Sin marca especificada" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = extended.textSecondary
                                        )
                                    }
                                    Surface(
                                        color = extended.surface3,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = vehicle.vehicleType.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = extended.accent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Ticket Boarding Pass Summary
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
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                                color = extended.accent
                            )
                            Text(
                                text = "Tarifa: S/ ${String.format("%.2f", hourlyRate)}/h (${selectedVehicle?.vehicleType?.name ?: "General"})",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = extended.textSecondary
                            )
                        }

                        Surface(
                            color = extended.surface2,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                Dimens.borderHairline,
                                extended.borderSubtle
                            )
                        ) {
                            Text(
                                text = "PAGO AL SALIR",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = extended.textSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
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
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
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
                text = "Revisar y confirmar reserva",
                onClick = {
                    showConfirmDialog = true
                },
                isLoading = reservationState.isLoading,
                enabled = selectedVehicleId != null && vehicleState.vehicles.isNotEmpty() && !reservationState.isLoading,
                leadingIcon = Icons.Filled.BookmarkAdd,
                style = ParkeoButtonStyle.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeightLarge)
            )

            Spacer(Modifier.height(Dimens.spacingLg))
        }
    }

    // Modal de Confirmación Previo al Envío
    if (showConfirmDialog) {
        val currentStart = if (arrivalOption == ArrivalOption.CUSTOM) {
            LocalDateTime.now(limaZone).plusMinutes(customMinutesOffset).withSecond(0).withNano(0)
        } else {
            LocalDateTime.now(limaZone).plusMinutes(arrivalOption.minutesOffset).withSecond(0).withNano(0)
        }
        val currentEnd = currentStart.plusHours(durationHours.toLong())

        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = extended.surface1,
            title = {
                Text(
                    text = "Confirmar reserva",
                    fontWeight = FontWeight.Bold,
                    color = extended.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "¿Deseas confirmar tu espacio en ${parkingState.parkingLot?.name ?: "la cochera"}?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = extended.textSecondary
                    )
                    Divider(color = extended.borderSubtle, thickness = 1.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vehículo:", color = extended.textTertiary, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${selectedVehicle?.licensePlate} (${selectedVehicle?.vehicleType?.name})",
                            color = extended.textPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ingreso estimado:", color = extended.textTertiary, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = currentStart.format(displayFormatter),
                            color = extended.textPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Salida estimada:", color = extended.textTertiary, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = currentEnd.format(displayFormatter),
                            color = extended.textPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Monto estimado:", color = extended.textTertiary, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "S/ ${String.format("%.2f", estimatedTotal)}",
                            color = extended.accent,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                ParkeoButton(
                    text = "Confirmar",
                    onClick = {
                        selectedVehicleId?.let { vId ->
                            // Cálculo exacto al milisegundo al disparar la acción
                            val freshStart = if (arrivalOption == ArrivalOption.CUSTOM) {
                                LocalDateTime.now(limaZone).plusMinutes(customMinutesOffset).withSecond(0).withNano(0)
                            } else {
                                LocalDateTime.now(limaZone).plusMinutes(arrivalOption.minutesOffset).withSecond(0).withNano(0)
                            }
                            val freshEnd = freshStart.plusHours(durationHours.toLong())

                            reservationViewModel.createReservation(
                                parkingLotId = parkingId,
                                vehicleId = vId,
                                startTime = formatter.format(freshStart),
                                endTime = formatter.format(freshEnd)
                            )
                        }
                    },
                    isLoading = reservationState.isLoading,
                    style = ParkeoButtonStyle.Primary
                )
            },
            dismissButton = {
                ParkeoButton(
                    text = "Atrás",
                    onClick = { showConfirmDialog = false },
                    style = ParkeoButtonStyle.Ghost
                )
            }
        )
    }

    // Modal de Éxito / Celebración con Navegación y Código
    if (showSuccessDialog) {
        val reservation = reservationState.currentReservation
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                reservationViewModel.clearMessages()
                onReservationCreated()
            },
            containerColor = extended.surface1,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = extended.accent,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "¡Reserva Confirmada!",
                        fontWeight = FontWeight.Black,
                        color = extended.textPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Tu lugar está reservado en ${parkingState.parkingLot?.name ?: "la cochera"}.",
                        color = extended.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Código de reserva destacado
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = extended.surface2,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, extended.accent.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CÓDIGO DE RESERVA",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                color = extended.textTertiary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = reservation?.confirmationCode ?: "PKO-${reservation?.id ?: "OK"}",
                                style = Typography.MonospaceTechnical,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = extended.accent
                            )
                            reservation?.spaceNumber?.let { space ->
                                if (space.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Espacio asignado: $space",
                                        style = Typography.MonospaceTechnical,
                                        fontSize = 12.sp,
                                        color = extended.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Muestra este código al operador cuando llegues a la cochera.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textTertiary
                    )
                }
            },
            confirmButton = {
                ParkeoButton(
                    text = "Ver mis reservas",
                    onClick = {
                        showSuccessDialog = false
                        reservationViewModel.clearMessages()
                        onReservationCreated()
                    },
                    style = ParkeoButtonStyle.Primary
                )
            },
            dismissButton = {
                parkingState.parkingLot?.let { lot ->
                    ParkeoButton(
                        text = "Cómo llegar",
                        leadingIcon = Icons.Filled.Directions,
                        onClick = {
                            val uri = Uri.parse("geo:${lot.latitude},${lot.longitude}?q=${Uri.encode(lot.name)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(mapIntent)
                        },
                        style = ParkeoButtonStyle.Secondary
                    )
                }
            }
        )
    }

    // Modal para Registrar Vehículo en línea
    if (showAddVehicleDialog) {
        InlineAddVehicleDialog(
            vehicleTypes = vehicleState.vehicleTypes,
            isLoading = vehicleState.isLoading,
            errorMessage = vehicleState.error,
            onDismiss = {
                vehicleViewModel.clearMessages()
                showAddVehicleDialog = false
            },
            onAdd = { plate, brand, model, color, typeId ->
                vehicleViewModel.addVehicle(plate, brand, model, color, typeId)
                showAddVehicleDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineAddVehicleDialog(
    vehicleTypes: List<VehicleTypeDto>,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onAdd: (String, String?, String?, String?, Long) -> Unit
) {
    val extended = ParkeoTheme.colors
    var plate by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var selectedTypeId by remember(vehicleTypes) {
        mutableStateOf<Long?>(vehicleTypes.firstOrNull()?.id)
    }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(vehicleTypes) {
        if (selectedTypeId == null && vehicleTypes.isNotEmpty()) {
            selectedTypeId = vehicleTypes.first().id
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extended.surface1,
        title = {
            Text(
                text = "Registrar vehículo",
                fontWeight = FontWeight.Bold,
                color = extended.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
            ) {
                errorMessage?.let { errorText ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ParkeoCardShape,
                        color = extended.signalRed.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            Dimens.borderHairline,
                            extended.signalRed.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.signalRed,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                ParkeoTextField(
                    value = plate,
                    onValueChange = { input ->
                        val clean = input.uppercase().replace(Regex("[^A-Z0-9]"), "")
                        plate = if (clean.length > 3) "${clean.take(3)}-${clean.drop(3).take(3)}" else clean.take(6)
                    },
                    label = "Placa *",
                    placeholder = "ABC-123",
                    modifier = Modifier.fillMaxWidth()
                )

                ParkeoTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = "Marca",
                    placeholder = "Ej. Toyota",
                    modifier = Modifier.fillMaxWidth()
                )

                ParkeoTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = "Modelo",
                    placeholder = "Ej. Yaris",
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    ParkeoTextField(
                        value = vehicleTypes.firstOrNull { it.id == selectedTypeId }?.name ?: "Seleccionar tipo",
                        onValueChange = {},
                        readOnly = true,
                        label = "Tipo de vehículo",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        vehicleTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedTypeId = type.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            ParkeoButton(
                text = "Guardar",
                isLoading = isLoading,
                onClick = {
                    selectedTypeId?.let { typeId ->
                        onAdd(
                            plate.trim(),
                            brand.trim().ifBlank { null },
                            model.trim().ifBlank { null },
                            color.trim().ifBlank { null },
                            typeId
                        )
                    }
                },
                enabled = plate.length >= 6 && selectedTypeId != null && !isLoading,
                style = ParkeoButtonStyle.Primary
            )
        },
        dismissButton = {
            ParkeoButton(
                text = "Cancelar",
                onClick = onDismiss,
                style = ParkeoButtonStyle.Ghost
            )
        }
    )
}