package pe.parkeo.ui.screens.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import pe.parkeo.data.remote.dto.ParkingLotDto
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.HomeViewModel

enum class HomeViewMode {
    MAP, LIST
}

enum class HomeFilter(val label: String) {
    ALL("Todos"),
    OPEN("Abiertos"),
    AVAILABLE("Con espacio"),
    HIGH_CAPACITY("Alta capacidad")
}

private val BottomSheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

/**
 * Explora: mapa de Google Maps a pantalla completa (full-bleed) con
 * búsqueda, filtros y accesos flotando encima. La lista de cocheras se
 * muestra como un bottom-sheet que deja ver el mapa atrás.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToParkingDetail: (Long) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToReservations: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    // Coordenadas iniciales: Miraflores, Lima (-12.1215, -77.0298)
    val defaultLocation = LatLng(-12.1215, -77.0298)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 13.5f)
    }

    var locationPermissionGranted by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    var viewMode by remember { mutableStateOf(HomeViewMode.MAP) }
    var selectedLot by remember { mutableStateOf<ParkingLotDto?>(null) }
    var activeFilter by remember { mutableStateOf(HomeFilter.ALL) }

    val displayedLots = remember(uiState.parkingLots, activeFilter) {
        when (activeFilter) {
            HomeFilter.ALL -> uiState.parkingLots
            HomeFilter.OPEN -> uiState.parkingLots.filter { it.isOpen }
            HomeFilter.AVAILABLE -> uiState.parkingLots.filter { it.availableSpaces > 0 }
            HomeFilter.HIGH_CAPACITY -> uiState.parkingLots.filter { it.availableSpaces >= 5 }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationPermissionGranted) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val userLatLng = LatLng(it.latitude, it.longitude)
                        coroutineScope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 14.5f))
                        }
                        viewModel.loadNearbyParkingLots(it.latitude, it.longitude)
                    }
                }
            } catch (_: SecurityException) { }
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(extended.background)) {

        // ── MAPA a pantalla completa (siempre compuesto, detrás de todo) ──
        GoogleMap(
            modifier = Modifier.matchParentSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = locationPermissionGranted),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                compassEnabled = true
            ),
            onMapClick = {
                focusManager.clearFocus()
                selectedLot = null
            }
        ) {
            displayedLots.forEach { parking ->
                val markerHue = when {
                    !parking.isOpen || parking.availableSpaces == 0 -> BitmapDescriptorFactory.HUE_RED
                    parking.availableSpaces <= 3 -> BitmapDescriptorFactory.HUE_ORANGE
                    else -> BitmapDescriptorFactory.HUE_GREEN
                }
                Marker(
                    state = MarkerState(LatLng(parking.latitude, parking.longitude)),
                    title = parking.name,
                    snippet = "${parking.availableSpaces} espacios disponibles",
                    icon = BitmapDescriptorFactory.defaultMarker(markerHue),
                    onClick = {
                        selectedLot = parking
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(parking.latitude, parking.longitude), 15.5f
                                )
                            )
                        }
                        true
                    }
                )
            }
        }

        // ── OVERLAY superior: búsqueda + accesos + filtros ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = Dimens.spacingMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Buscador compacto: una sola línea, alto fijo y copy breve
                // (antes ocupaba toda la barra y el placeholder se partía en 2 líneas).
                Surface(
                    onClick = { searchFocusRequester.requestFocus() },
                    modifier = Modifier
                        .widthIn(max = 228.dp)
                        .height(44.dp),
                    shape = ParkeoPillShape,
                    color = extended.surface1.copy(alpha = 0.94f),
                    border = BorderStroke(Dimens.borderHairline, extended.borderSubtle),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 14.dp, end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = extended.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                viewModel.search(it)
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = extended.textPrimary
                            ),
                            cursorBrush = SolidColor(extended.accent),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(searchFocusRequester),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Buscar cochera…",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = extended.textTertiary,
                                            maxLines = 1
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    viewModel.search("")
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    "Limpiar búsqueda",
                                    tint = extended.textTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Accesos rápidos
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Acceso a reservas
                    Surface(
                        onClick = onNavigateToReservations,
                        modifier = Modifier.size(40.dp),
                        shape = ParkeoPillShape,
                        color = extended.surface1.copy(alpha = 0.94f),
                        border = BorderStroke(Dimens.borderHairline, extended.borderSubtle),
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.BookmarkBorder,
                                contentDescription = "Mis reservas",
                                tint = extended.textSecondary,
                                modifier = Modifier.size(Dimens.iconSizeMedium)
                            )
                        }
                    }

                    // Acceso al perfil
                    Surface(
                        onClick = onNavigateToProfile,
                        modifier = Modifier.size(40.dp),
                        shape = ParkeoPillShape,
                        color = extended.surface1.copy(alpha = 0.94f),
                        border = BorderStroke(Dimens.borderHairline, extended.borderSubtle),
                        shadowElevation = 6.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = "Perfil",
                                tint = extended.textSecondary,
                                modifier = Modifier.size(Dimens.iconSizeMedium)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Dimens.spacingSm))

            // Filtros rápidos en pills horizontales
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
            ) {
                item {
                    Surface(
                        onClick = {
                            focusManager.clearFocus()
                            viewMode = if (viewMode == HomeViewMode.MAP) HomeViewMode.LIST else HomeViewMode.MAP
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = extended.accent,
                        border = BorderStroke(Dimens.borderHairline, extended.accent)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (viewMode == HomeViewMode.MAP) Icons.Filled.FormatListBulleted else Icons.Filled.Map,
                                contentDescription = null,
                                tint = extended.onAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (viewMode == HomeViewMode.MAP) "Lista (${displayedLots.size})" else "Mapa",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = extended.onAccent
                            )
                        }
                    }
                }
                items(HomeFilter.entries.toList()) { filter ->
                    val isSelected = activeFilter == filter
                    Surface(
                        onClick = { activeFilter = filter },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) extended.accent else extended.surface1.copy(alpha = 0.94f),
                        border = BorderStroke(
                            Dimens.borderHairline,
                            if (isSelected) extended.accent else extended.borderSubtle
                        )
                    ) {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) extended.onAccent else extended.textSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(Dimens.spacingSm))

            // ── Controles del mapa (esquina derecha, bajo el overlay) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                    MapControlButton(imageVector = Icons.Filled.ZoomOutMap, contentDescription = "Ver todas las cocheras") {
                        coroutineScope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(defaultLocation, 13.5f))
                        }
                    }
                    if (locationPermissionGranted) {
                        MapControlButton(imageVector = Icons.Filled.MyLocation, contentDescription = "Mi ubicación") {
                            try {
                                val fusedLocationClient =
                                    LocationServices.getFusedLocationProviderClient(context)
                                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                    location?.let {
                                        val userLatLng = LatLng(it.latitude, it.longitude)
                                        coroutineScope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(userLatLng, 15f)
                                            )
                                        }
                                    }
                                }
                            } catch (_: SecurityException) { }
                        }
                    }
                }
            }
        }

        // ── BOTTOM SHEET de lista (modo LISTA) ──
        androidx.compose.animation.AnimatedVisibility(
            visible = viewMode == HomeViewMode.LIST,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = BottomSheetShape,
                color = extended.surface1,
                border = BorderStroke(Dimens.borderHairline, extended.border),
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Grabber arrastrable estético
                    Box(
                        Modifier
                            .padding(top = 10.dp)
                            .align(Alignment.CenterHorizontally)
                            .size(width = 44.dp, height = 5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(extended.border)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COCHERAS DISPONIBLES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = extended.textTertiary
                        )
                        Text(
                            text = "${displayedLots.size} en total",
                            style = Typography.MonospaceTechnical,
                            fontSize = 12.sp,
                            color = extended.accent
                        )
                    }

                    when {
                        uiState.isLoading -> {
                            ParkeoSkeletonList(
                                itemCount = 4,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = Dimens.spacingMd)
                            )
                        }

                        uiState.error != null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                ParkeoErrorView(
                                    message = uiState.error ?: "Error al cargar cocheras",
                                    onRetry = { viewModel.loadParkingLots() }
                                )
                            }
                        }

                        displayedLots.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                ParkeoEmptyState(
                                    title = "No encontramos cocheras",
                                    subtitle = if (searchQuery.isNotBlank())
                                        "No hay resultados para \"$searchQuery\" con los filtros aplicados"
                                    else
                                        "No hay cocheras que coincidan con el filtro \"${activeFilter.label}\"",
                                    icon = Icons.Filled.SearchOff
                                )
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    horizontal = Dimens.spacingMd,
                                    vertical = Dimens.spacingSm
                                ),
                                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                            ) {
                                items(displayedLots, key = { it.id }) { parking ->
                                    ParkingCard(
                                        parking = parking,
                                        onClick = { onNavigateToParkingDetail(parking.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Preview card (modo MAPA) al seleccionar un marcador ──
        androidx.compose.animation.AnimatedVisibility(
            visible = selectedLot != null && viewMode == HomeViewMode.MAP,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(Dimens.spacingMd)
        ) {
            selectedLot?.let { lot ->
                val (badgeText, badgeVariant) = when {
                    !lot.isOpen -> "Cerrado" to BadgeVariant.Closed
                    lot.availableSpaces == 0 -> "Completo" to BadgeVariant.Occupied
                    lot.availableSpaces <= 3 -> "${lot.availableSpaces} libres" to BadgeVariant.Reserved
                    else -> "${lot.availableSpaces} libres" to BadgeVariant.Available
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = extended.surface1,
                    border = BorderStroke(Dimens.borderHairline, extended.border),
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Asa de hoja inferior (indica que se puede cerrar deslizando)
                        Box(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(width = 36.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(extended.border)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = lot.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = extended.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = lot.district ?: lot.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ParkeoBadge(text = badgeText, variant = badgeVariant)
                                IconButton(
                                    onClick = { selectedLot = null },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Cerrar",
                                        tint = extended.textTertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Metadatos: capacidad, distancia y estado abierto/cerrado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            MapInfoChip(
                                icon = Icons.Filled.LocalParking,
                                text = "${lot.totalCapacity} espacios"
                            )
                            lot.distance?.let { dist ->
                                MapInfoChip(
                                    icon = Icons.Filled.NearMe,
                                    text = "%.1f km".format(dist)
                                )
                            }
                            MapInfoChip(
                                icon = if (lot.isOpen) Icons.Filled.Schedule else Icons.Filled.Lock,
                                text = if (lot.isOpen) "Abierto" else "Cerrado",
                                highlight = lot.isOpen
                            )
                        }

                        // Acción principal a ancho completo (antes el texto se estrujaba
                        // en una fila angosta junto a la capacidad)
                        ParkeoButton(
                            text = "Ver detalles y reservar",
                            onClick = { onNavigateToParkingDetail(lot.id) },
                            trailingIcon = Icons.Filled.ChevronRight,
                            style = ParkeoButtonStyle.Primary,
                            compact = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/** Chip compacto de metadatos (capacidad, distancia, estado) en la card del marcador. */
@Composable
private fun MapInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    highlight: Boolean = false
) {
    val extended = ParkeoTheme.colors
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (highlight) extended.accent.copy(alpha = 0.12f) else extended.surface2,
        border = BorderStroke(
            Dimens.borderHairline,
            if (highlight) extended.accent.copy(alpha = 0.35f) else extended.borderSubtle
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlight) extended.accent else extended.textTertiary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (highlight) extended.textPrimary else extended.textSecondary,
                maxLines = 1
            )
        }
    }
}

/** Botón cuadrado flotante con estética Parkeo para los controles del mapa. */
@Composable
private fun MapControlButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val extended = ParkeoTheme.colors
    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        color = extended.surface1.copy(alpha = 0.92f),
        shape = ParkeoCardShape,
        border = BorderStroke(Dimens.borderHairline, extended.borderSubtle),
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = extended.accent,
                modifier = Modifier.size(Dimens.iconSizeMedium)
            )
        }
    }
}
