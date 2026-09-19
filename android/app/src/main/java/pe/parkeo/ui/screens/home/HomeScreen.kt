package pe.parkeo.ui.screens.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.HomeViewModel

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

    // Coordenadas iniciales: Miraflores, Lima (-12.1215, -77.0298)
    val defaultLocation = LatLng(-12.1215, -77.0298)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 13f)
    }

    var locationPermissionGranted by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

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
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(userLatLng, 14f))
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
            // Search Bar Input
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSm)
            ) {
                ParkeoTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.search(it)
                    },
                    placeholder = "Buscar cochera por nombre o distrito...",
                    leadingIcon = Icons.Filled.Search,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Interactive Map View Vessel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(horizontal = Dimens.spacingMd)
                    .clip(ParkeoCardShape)
                    .border(Dimens.borderHairline, extended.borderSubtle, ParkeoCardShape)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = locationPermissionGranted
                    ),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = locationPermissionGranted,
                        zoomControlsEnabled = false,
                        compassEnabled = true
                    )
                ) {
                    uiState.parkingLots.forEach { parking ->
                        Marker(
                            state = MarkerState(LatLng(parking.latitude, parking.longitude)),
                            title = parking.name,
                            snippet = "${parking.availableSpaces} espacios disponibles",
                            onClick = {
                                onNavigateToParkingDetail(parking.id)
                                false
                            }
                        )
                    }
                }
            }

            // Parking Lots Section
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        ParkeoLoadingView(message = "Buscando cocheras disponibles...")
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
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
                        modifier = Modifier.weight(1f),
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
