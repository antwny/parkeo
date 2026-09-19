package pe.parkeo.ui.screens.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    var viewMode by remember { mutableStateOf(HomeViewMode.MAP) }
    var selectedLot by remember { mutableStateOf<ParkingLotDto?>(null) }

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

    Scaffold(
        containerColor = extended.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            ParkeoTopBar(
                title = "Parkeo",
                isBrandTitle = true,
                actions = {
                    IconButton(onClick = onNavigateToReservations) {
                        Icon(
                            imageVector = Icons.Filled.BookmarkBorder,
                            contentDescription = "Mis reservas",
                            tint = extended.textSecondary
                        )
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Perfil",
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
            // Floating Filter & Search Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(extended.surface1)
                    .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSm),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
            ) {
                ParkeoTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.search(it)
                    },
                    placeholder = "Buscar cochera por nombre o distrito...",
                    leadingIcon = Icons.Filled.Search,
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = {
                                searchQuery = ""
                                viewModel.search("")
                            }) {
                                Icon(Icons.Filled.Close, "Limpiar búsqueda", tint = extended.textTertiary)
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                // View Mode Toggle Segmented Control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isMapSelected = viewMode == HomeViewMode.MAP
                    Surface(
                        onClick = {
                            focusManager.clearFocus()
                            viewMode = HomeViewMode.MAP
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        color = if (isMapSelected) extended.accent.copy(alpha = 0.15f) else extended.surface2,
                        shape = ParkeoPillShape,
                        border = BorderStroke(
                            Dimens.borderHairline,
                            if (isMapSelected) extended.accent else extended.borderSubtle
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Map,
                                contentDescription = null,
                                tint = if (isMapSelected) extended.accent else extended.textTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Mapa interactivo",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isMapSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isMapSelected) extended.accent else extended.textSecondary
                            )
                        }
                    }

                    val isListSelected = viewMode == HomeViewMode.LIST
                    Surface(
                        onClick = {
                            focusManager.clearFocus()
                            viewMode = HomeViewMode.LIST
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        color = if (isListSelected) extended.accent.copy(alpha = 0.15f) else extended.surface2,
                        shape = ParkeoPillShape,
                        border = BorderStroke(
                            Dimens.borderHairline,
                            if (isListSelected) extended.accent else extended.borderSubtle
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FormatListBulleted,
                                contentDescription = null,
                                tint = if (isListSelected) extended.accent else extended.textTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Lista (${uiState.parkingLots.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isListSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isListSelected) extended.accent else extended.textSecondary
                            )
                        }
                    }
                }
            }

            // Main Content: Full-Bleed Map or List View
            when (viewMode) {
                HomeViewMode.MAP -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(
                                isMyLocationEnabled = locationPermissionGranted
                            ),
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
                            uiState.parkingLots.forEach { parking ->
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
                                                    LatLng(parking.latitude, parking.longitude),
                                                    15.5f
                                                )
                                            )
                                        }
                                        true
                                    }
                                )
                            }
                        }

                        // Floating Map Controls (Re-center & My Location)
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(Dimens.spacingMd),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = {
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(defaultLocation, 13.5f)
                                        )
                                    }
                                },
                                modifier = Modifier.size(42.dp),
                                color = extended.surface1.copy(alpha = 0.92f),
                                shape = ParkeoCardShape,
                                border = BorderStroke(Dimens.borderHairline, extended.borderSubtle),
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.ZoomOutMap,
                                        contentDescription = "Ver todas las cocheras",
                                        tint = extended.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (locationPermissionGranted) {
                                Surface(
                                    onClick = {
                                        try {
                                            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                                location?.let {
                                                    val userLatLng = LatLng(it.latitude, it.longitude)
                                                    coroutineScope.launch {
                                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                                                    }
                                                }
                                            }
                                        } catch (_: SecurityException) { }
                                    },
                                    modifier = Modifier.size(42.dp),
                                    color = extended.surface1.copy(alpha = 0.92f),
                                    shape = ParkeoCardShape,
                                    border = BorderStroke(Dimens.borderHairline, extended.borderSubtle),
                                    shadowElevation = 4.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.MyLocation,
                                            contentDescription = "Mi ubicación",
                                            tint = extended.accent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Preview Card when a parking lot is selected
                        androidx.compose.animation.AnimatedVisibility(
                            visible = selectedLot != null,
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
                                    shape = ParkeoCardShape,
                                    color = extended.surface1,
                                    border = BorderStroke(Dimens.borderHairline, extended.border),
                                    shadowElevation = 8.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                Text(
                                                    text = lot.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = extended.textPrimary
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = lot.district ?: lot.address,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = extended.textSecondary
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                ParkeoBadge(text = badgeText, variant = badgeVariant)
                                                Spacer(Modifier.width(8.dp))
                                                IconButton(
                                                    onClick = { selectedLot = null },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Close,
                                                        contentDescription = "Cerrar",
                                                        tint = extended.textTertiary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Filled.LocalParking,
                                                        contentDescription = null,
                                                        tint = extended.accent,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = "${lot.totalCapacity} capacidad",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = extended.textSecondary,
                                                        fontSize = 12.sp
                                                    )
                                                }

                                                lot.distance?.let { dist ->
                                                    Text(
                                                        text = "•  %.1f km".format(dist),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = extended.textSecondary,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }

                                            ParkeoButton(
                                                text = "Ver detalles y reservar",
                                                onClick = { onNavigateToParkingDetail(lot.id) },
                                                style = ParkeoButtonStyle.Primary,
                                                modifier = Modifier.height(38.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HomeViewMode.LIST -> {
                    when {
                        uiState.isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                ParkeoLoadingView(message = "Buscando cocheras disponibles...")
                            }
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

                        uiState.parkingLots.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                ParkeoEmptyState(
                                    title = "No encontramos cocheras",
                                    subtitle = if (searchQuery.isNotBlank())
                                        "No hay resultados para \"$searchQuery\" en esta zona"
                                    else
                                        "No hay cocheras disponibles en este momento",
                                    icon = Icons.Filled.SearchOff
                                )
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(Dimens.spacingMd),
                                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = Dimens.spacingXs),
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
                                            text = "${uiState.parkingLots.size} en total",
                                            style = Typography.MonospaceTechnical,
                                            fontSize = 12.sp,
                                            color = extended.accent
                                        )
                                    }
                                }

                                items(uiState.parkingLots, key = { it.id }) { parking ->
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
    }
}
