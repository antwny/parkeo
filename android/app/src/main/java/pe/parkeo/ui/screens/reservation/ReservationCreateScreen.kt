package pe.parkeo.ui.screens.reservation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Opciones de hora de ingreso. El copy es corto para que las pills nunca
 * se corten en pantallas angostas y el offset se aplica al backend.
 */
enum class ArrivalOption(
    val title: String,
    val minutesOffset: Long,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    NOW("Inmediato", 5, Icons.Filled.Bolt),
    IN_30("30 min", 30, Icons.Filled.Schedule),
    IN_60("1 hora", 60, Icons.Filled.Schedule),
    IN_120("2 horas", 120, Icons.Filled.Schedule),
    CUSTOM("Personalizada", 0, Icons.Filled.EditCalendar)
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
    val availabilityState by reservationViewModel.availability.collectAsState()
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

    // Fecha/hora exactas (M3 pickers) para la opción personalizada
    var customStart by remember {
        mutableStateOf(LocalDateTime.now(limaZone).plusMinutes(180).withSecond(0).withNano(0))
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Espacio concreto elegido tras el chequeo de disponibilidad (null = asignación automática)
    var selectedSpaceId by remember { mutableStateOf<Long?>(null) }

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Calcular horas dinámicas para visualización en pantalla
    // (exactStart/exactEnd se definen más abajo, junto al chequeo de disponibilidad)

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

    // ─── Hora de inicio/reserva exacta según la opción elegida ────────────────
    fun computeStart(): LocalDateTime = when (arrivalOption) {
        ArrivalOption.CUSTOM -> customStart
        else -> LocalDateTime.now(limaZone).plusMinutes(arrivalOption.minutesOffset).withSecond(0).withNano(0)
    }

    val exactStart = computeStart()
    val exactEnd = exactStart.plusHours(durationHours.toLong())

    // Al cambiar cualquier parámetro relevante, se consulta la disponibilidad real
    // al backend (precio de la cochera para ese horario + espacios libres).
    val selectedVehicleType = selectedVehicle?.vehicleType
    LaunchedEffect(parkingId, arrivalOption, customMinutesOffset, customStart, durationHours, selectedVehicleType) {
        val vehicle = vehicleState.vehicles.find { it.id == selectedVehicleId } ?: return@LaunchedEffect
        reservationViewModel.checkAvailability(
            parkingLotId = parkingId,
            vehicleTypeId = vehicle.vehicleType.id,
            startTime = formatter.format(exactStart),
            endTime = formatter.format(exactEnd)
        )
    }

    // Deja limpiar el chequeo al salir de la pantalla
    DisposableEffect(Unit) {
        onDispose { reservationViewModel.resetAvailabilityCheck() }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = extended.textTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "HORA DE INGRESO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = extended.textTertiary
                            )
                        }
                        Text(
                            text = exactStart.format(displayFormatter),
                            style = Typography.MonospaceTechnical,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = extended.accent
                        )
                    }

                    // Pills de hora de llegada: ancho natural y deslizables
                    // (antes 5 chips de igual peso que se cortaban en pantallas angostas)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ArrivalOption.values().toList()) { opt ->
                            val isSelected = arrivalOption == opt
                            Surface(
                                onClick = { arrivalOption = opt },
                                shape = ParkeoPillShape,
                                color = if (isSelected) extended.accent else extended.surface2,
                                border = androidx.compose.foundation.BorderStroke(
                                    Dimens.borderHairline,
                                    if (isSelected) extended.accent else extended.borderSubtle
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = opt.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) extended.onAccent else extended.textSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = opt.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) extended.onAccent else extended.textPrimary,
                                        maxLines = 1
                                    )
                                    if (isSelected) {
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = extended.onAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Si seleccionó personalizado: fecha/hora exactos con pickers M3
                    if (arrivalOption == ArrivalOption.CUSTOM) {
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(extended.surface2, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = customStart.format(displayFormatter),
                                    style = Typography.MonospaceTechnical,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = extended.accent
                                )
                                Text(
                                    text = if (customStart.isBefore(LocalDateTime.now(limaZone).plusMinutes(15)))
                                        "Muy pronto" else "A futuro",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (customStart.isBefore(LocalDateTime.now(limaZone).plusMinutes(15)))
                                        extended.signalRed else extended.textTertiary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                                ParkeoButton(
                                    text = "Elegir día",
                                    leadingIcon = Icons.Filled.CalendarMonth,
                                    onClick = { showDatePicker = true },
                                    style = ParkeoButtonStyle.Ghost,
                                    modifier = Modifier.weight(1f)
                                )
                                ParkeoButton(
                                    text = "Elegir hora",
                                    leadingIcon = Icons.Filled.Schedule,
                                    onClick = { showTimePicker = true },
                                    style = ParkeoButtonStyle.Ghost,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Ajuste fino por pasos de 15 min
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(extended.surface2, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Ajuste fino ±15 min",
                                    style = Typography.MonospaceTechnical,
                                    fontSize = 12.sp,
                                    color = extended.textPrimary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            customStart = customStart.minusMinutes(15)
                                            customMinutesOffset = java.time.Duration.between(
                                                LocalDateTime.now(limaZone), customStart).toMinutes().coerceAtLeast(5)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Remove, "-15 min", tint = extended.accent)
                                    }
                                    IconButton(
                                        onClick = {
                                            customStart = customStart.plusMinutes(15)
                                            customMinutesOffset = java.time.Duration.between(
                                                LocalDateTime.now(limaZone), customStart).toMinutes()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Add, "+15 min", tint = extended.accent)
                                    }
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
                                text = exactStart.format(displayFormatter),
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
                                text = exactEnd.format(displayFormatter),
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

            // Chequeo de disponibilidad real (backend) + elección opcional de espacio
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
                            text = "DISPONIBILIDAD REAL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = extended.textTertiary
                        )
                    }

                    when {
                        availabilityState.isChecking -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = extended.accent
                                )
                                Text(
                                    text = "Consultando la cochera para tu horario…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.textSecondary
                                )
                            }
                        }

                        availabilityState.error != null -> {
                            val availabilityError = availabilityState.error ?: "No se pudo consultar la disponibilidad"
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = extended.signalRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = availabilityError,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = extended.signalRed
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        selectedVehicle?.let { vehicle ->
                                            reservationViewModel.checkAvailability(
                                                parkingId, vehicle.vehicleType.id,
                                                formatter.format(exactStart), formatter.format(exactEnd)
                                            )
                                        }
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Reintentar", style = MaterialTheme.typography.labelSmall, color = extended.accent)
                                }
                            }
                        }

                        availabilityState.check?.isAvailable == true -> {
                            val check = availabilityState.check
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = extended.signalGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Disponible para tu horario",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = extended.signalGreen
                                )
                            }
                            Text(
                                text = "${check?.availableSpaces?.size ?: 0} espacio(s) compatibles con tu vehículo en ese rango.",
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.textSecondary
                            )

                            // Elección opcional de espacio concreto
                            if (!check?.availableSpaces.isNullOrEmpty()) {
                                Text(
                                    text = "¿PREFIERES UN ESPACIO CONCRETO?",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp,
                                    color = extended.textTertiary
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    item {
                                        SpaceChip(
                                            label = "Automático",
                                            subtitle = "Cualquiera",
                                            isSelected = selectedSpaceId == null,
                                            onClick = { selectedSpaceId = null }
                                        )
                                    }
                                    items(check.availableSpaces, key = { it.id }) { space ->
                                        SpaceChip(
                                            label = space.spaceNumber,
                                            subtitle = listOfNotNull(
                                                space.floor?.takeIf { it.isNotBlank() }?.let { "Piso $it" },
                                                space.isCovered?.let { covered -> if (covered) "Techo" else null }
                                            ).joinToString(" • ").ifBlank { "Elegir" },
                                            isSelected = selectedSpaceId == space.id,
                                            onClick = { selectedSpaceId = space.id }
                                        )
                                    }
                                }
                            }
                        }

                        else -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.EventBusy,
                                    contentDescription = null,
                                    tint = extended.signalAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "La cochera no tiene espacios libres para ese horario. Ajusta hora o duración.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.signalAmber
                                )
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
                        Column(modifier = Modifier.weight(1f)) {
                            val serverPrice = availabilityState.check?.calculatedPrice
                            Text(
                                text = if (serverPrice != null) "TOTAL CALCULADO" else "TOTAL ESTIMADO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = extended.textTertiary
                            )
                            Text(
                                text = "S/ ${String.format("%.2f", serverPrice ?: estimatedTotal)}",
                                style = Typography.MonospaceTechnical,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                                color = extended.accent
                            )
                            Text(
                                text = if (serverPrice != null) {
                                    "Tarifa real de la cochera para tu horario (${availabilityState.check?.totalHours ?: durationHours}h)"
                                } else {
                                    "Tarifa: S/ ${String.format("%.2f", hourlyRate)}/h (${selectedVehicle?.vehicleType?.name ?: "General"})"
                                },
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
        val currentStart = computeStart()
        val currentEnd = currentStart.plusHours(durationHours.toLong())
        val serverPrice = availabilityState.check?.calculatedPrice

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
                        Text("Monto:", color = extended.textTertiary, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "S/ ${String.format("%.2f", serverPrice ?: estimatedTotal)}",
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
                            val freshStart = computeStart()
                            val freshEnd = freshStart.plusHours(durationHours.toLong())

                            reservationViewModel.createReservation(
                                parkingLotId = parkingId,
                                parkingSpaceId = selectedSpaceId,
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

    // ─── Picker de FECHA (Material 3) ──────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = customStart.toLocalDate()
                .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // selectedDateMillis es medianoche UTC del día local elegido
                        val pickedDay = Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        customStart = pickedDay.atTime(customStart.toLocalTime())
                            .withSecond(0).withNano(0)
                    }
                    showDatePicker = false
                }) {
                    Text("Elegir", color = extended.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = extended.textSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = extended.surface1)
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ─── Picker de HORA (Material 3) ───────────────────────────────────────
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = customStart.hour,
            initialMinute = customStart.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = extended.surface1,
            title = {
                Text(
                    text = "Elegir hora de llegada",
                    fontWeight = FontWeight.Bold,
                    color = extended.textPrimary
                )
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = extended.surface2,
                            selectorColor = extended.accent,
                            timeSelectorSelectedContainerColor = extended.accent.copy(alpha = 0.2f),
                            timeSelectorSelectedContentColor = extended.accent
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    customStart = customStart.toLocalDate()
                        .atTime(timePickerState.hour, timePickerState.minute)
                        .withSecond(0).withNano(0)
                    showTimePicker = false
                }) {
                    Text("Elegir", color = extended.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancelar", color = extended.textSecondary)
                }
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

@Composable
private fun SpaceChip(
    label: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val extended = ParkeoTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) extended.accent.copy(alpha = 0.14f) else extended.surface2,
        border = androidx.compose.foundation.BorderStroke(
            Dimens.borderHairline,
            if (isSelected) extended.accent else extended.borderSubtle
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = Typography.MonospaceTechnical,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) extended.accent else extended.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = extended.textTertiary
            )
        }
    }
}