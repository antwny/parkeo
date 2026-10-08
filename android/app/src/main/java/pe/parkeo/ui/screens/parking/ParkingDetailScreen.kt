package pe.parkeo.ui.screens.parking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.ParkingDetailViewModel
import pe.parkeo.util.DirectionsButton

@Composable
fun ParkingDetailScreen(
    parkingId: Long,
    viewModel: ParkingDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReservation: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors

    LaunchedEffect(parkingId) {
        viewModel.loadParkingLot(parkingId)
    }

    Scaffold(
        containerColor = extended.background,
        topBar = {
            ParkeoTopBar(
                title = uiState.parkingLot?.name ?: "Detalle de cochera",
                onNavigationClick = onNavigateBack
            )
        },
        bottomBar = {
            uiState.parkingLot?.let { parking ->
                Surface(
                    color = extended.surface1,
                    border = androidx.compose.foundation.BorderStroke(
                        Dimens.borderHairline,
                        extended.borderSubtle
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(Dimens.spacingMd),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ParkeoButton(
                            text = "Regresar",
                            onClick = onNavigateBack,
                            leadingIcon = Icons.Filled.ArrowBack,
                            style = ParkeoButtonStyle.Secondary,
                            modifier = Modifier
                                .weight(0.9f)
                                .height(Dimens.buttonHeightDefault)
                        )

                        ParkeoButton(
                            text = "Reservar espacio",
                            onClick = { onNavigateToReservation(parking.id) },
                            leadingIcon = Icons.Filled.BookmarkAdd,
                            enabled = parking.isOpen && parking.availableSpaces > 0,
                            style = ParkeoButtonStyle.Primary,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(Dimens.buttonHeightDefault)
                        )
                    }
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    ParkeoLoadingView(message = "Cargando detalles...")
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    ParkeoErrorView(
                        message = uiState.error ?: "Error al cargar la información",
                        onRetry = { viewModel.loadParkingLot(parkingId) }
                    )
                }
            }

            uiState.parkingLot != null -> {
                val parking = uiState.parkingLot!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(Dimens.spacingMd),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                ) {
                    // Availability Hero Card
                    ParkeoCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentBorder = parking.availableSpaces > 0
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimens.spacingLg),
                            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "DISPONIBILIDAD EN TIEMPO REAL",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = extended.textTertiary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "${parking.availableSpaces}",
                                            style = Typography.MonospaceTechnical,
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (parking.availableSpaces > 0) extended.accent else extended.signalRed
                                        )
                                        Text(
                                            text = " / ${parking.totalCapacity} espacios",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = extended.textSecondary,
                                            modifier = Modifier.padding(bottom = 6.dp, start = 6.dp)
                                        )
                                    }
                                }

                                ParkeoBadge(
                                    status = if (!parking.isOpen)
                                        ParkeoBadgeStatus.Closed
                                    else if (parking.availableSpaces > 0)
                                        ParkeoBadgeStatus.Available
                                    else
                                        ParkeoBadgeStatus.Occupied
                                )
                            }

                            // Linear progress indicator
                            val occupancyRatio = if (parking.totalCapacity > 0) {
                                1f - (parking.availableSpaces.toFloat() / parking.totalCapacity)
                            } else 0f

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LinearProgressIndicator(
                                    progress = { occupancyRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = if (parking.availableSpaces > 0) extended.accent else extended.signalRed,
                                    trackColor = extended.surface3
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Ocupación: ${(occupancyRatio * 100).toInt()}%",
                                        style = Typography.MonospaceTechnical,
                                        fontSize = 11.sp,
                                        color = extended.textTertiary
                                    )
                                    Text(
                                        text = if (parking.isOpen) "Abierto" else "Cerrado temporalmente",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (parking.isOpen) extended.signalGreen else extended.signalRed
                                    )
                                }
                            }
                        }
                    }

                    // Location & Contact Information Card
                    ParkeoCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(Dimens.spacingLg),
                            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                        ) {
                            Text(
                                text = "UBICACIÓN Y CONTACTO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = extended.textTertiary
                            )

                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = extended.accent,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = parking.address,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = extended.textPrimary
                                    )
                                    parking.district?.let {
                                        Text(
                                            text = "$it, Lima",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = extended.textSecondary
                                        )
                                    }
                                }
                            }

                            // Ruta hasta la cochera (Google Maps)
                            val lat = parking.latitude
                            val lng = parking.longitude
                            if (lat != null && lng != null) {
                                DirectionsButton(latitude = lat, longitude = lng)
                            }

                            parking.phone?.let { phone ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Phone,
                                        contentDescription = null,
                                        tint = extended.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = phone,
                                        style = Typography.MonospaceTechnical,
                                        fontSize = 13.sp,
                                        color = extended.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Tariffs Table Card
                    if (parking.tariffs.isNotEmpty()) {
                        ParkeoCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(Dimens.spacingLg),
                                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                            ) {
                                Text(
                                    text = "TABLA DE TARIFAS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = extended.textTertiary
                                )

                                Spacer(Modifier.height(4.dp))

                                parking.tariffs.forEach { tariff ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = when {
                                                    tariff.vehicleType.contains("MOTO", ignoreCase = true) -> Icons.Filled.TwoWheeler
                                                    tariff.vehicleType.contains("CAMION", ignoreCase = true) -> Icons.Filled.LocalShipping
                                                    else -> Icons.Filled.DirectionsCar
                                                },
                                                contentDescription = null,
                                                tint = extended.textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "${tariff.vehicleType} • ${tariff.tariffType}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = extended.textPrimary
                                            )
                                        }

                                        Text(
                                            text = "S/ ${String.format("%.2f", tariff.price)}",
                                            style = Typography.MonospaceTechnical,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = extended.accent
                                        )
                                    }
                                    HorizontalDivider(
                                        color = extended.borderSubtle,
                                        thickness = Dimens.borderHairline
                                    )
                                }
                            }
                        }
                    }

                    // Services Included Card
                    if (parking.services.isNotEmpty()) {
                        ParkeoCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(Dimens.spacingLg),
                                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                            ) {
                                Text(
                                    text = "SERVICIOS INCLUIDOS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = extended.textTertiary
                                )

                                Spacer(Modifier.height(4.dp))

                                parking.services.forEach { service ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = extended.accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = service.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = extended.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}