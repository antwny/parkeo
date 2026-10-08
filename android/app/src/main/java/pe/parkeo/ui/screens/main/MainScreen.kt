package pe.parkeo.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Scaffold
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import pe.parkeo.ui.components.ParkeoBottomBar
import pe.parkeo.ui.components.ParkeoBottomNavItem
import pe.parkeo.ui.components.ParkeoLoadingView
import pe.parkeo.ui.screens.admin.AdminDashboardScreen
import pe.parkeo.ui.screens.admin.AdminParkingLotsScreen
import pe.parkeo.ui.screens.admin.AdminUsersScreen
import pe.parkeo.ui.screens.home.HomeScreen
import pe.parkeo.ui.screens.operator.OperatorReservationsScreen
import pe.parkeo.ui.screens.operator.OperatorSpacesScreen
import pe.parkeo.ui.screens.profile.ProfileScreen
import pe.parkeo.ui.screens.reservation.ReservationsScreen
import pe.parkeo.ui.screens.vehicles.VehiclesScreen
import pe.parkeo.ui.theme.ParkeoTheme
import pe.parkeo.ui.viewmodel.*

@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    adminViewModel: AdminViewModel,
    operatorViewModel: OperatorViewModel,
    reservationViewModel: ReservationViewModel,
    vehicleViewModel: VehicleViewModel,
    onNavigateToParkingDetail: (Long) -> Unit,
    onNavigateToReservationCreate: (Long) -> Unit,
    onLogout: () -> Unit
) {
    val userProfile by authViewModel.userProfile.collectAsState()
    val extended = ParkeoTheme.colors

    if (userProfile == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ParkeoLoadingView(message = "Cargando perfil...")
        }
        return
    }

    val roleUpper = (userProfile?.role ?: "").uppercase()
    val isAdmin = roleUpper.contains("ADMIN")
    val isOperator = !isAdmin && (roleUpper.contains("OPERAT") || roleUpper.contains("OPERADOR"))

    // Define navigation items based on role
    val items = remember(isAdmin, isOperator) {
        when {
            isAdmin -> listOf(
                ParkeoBottomNavItem("Métricas", Icons.Filled.Dashboard),
                ParkeoBottomNavItem("Cocheras", Icons.Filled.LocalParking),
                ParkeoBottomNavItem("Usuarios", Icons.Filled.Group),
                ParkeoBottomNavItem("Explorar", Icons.Filled.Map),
                ParkeoBottomNavItem("Perfil", Icons.Filled.Person)
            )
            isOperator -> listOf(
                ParkeoBottomNavItem("Espacios", Icons.Filled.GridView),
                ParkeoBottomNavItem("Reservas", Icons.Filled.BookmarkBorder),
                ParkeoBottomNavItem("Explorar", Icons.Filled.Map),
                ParkeoBottomNavItem("Perfil", Icons.Filled.Person)
            )
            else -> listOf(
                ParkeoBottomNavItem("Explorar", Icons.Filled.Map),
                ParkeoBottomNavItem("Reservas", Icons.Filled.BookmarkBorder),
                ParkeoBottomNavItem("Vehículos", Icons.Filled.DirectionsCar),
                ParkeoBottomNavItem("Perfil", Icons.Filled.Person)
            )
        }
    }

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    // If role changes, clamp index
    LaunchedEffect(items.size) {
        if (selectedIndex >= items.size) {
            selectedIndex = 0
        }
    }

    // Hardware back button returns to the first tab before exiting
    BackHandler(enabled = selectedIndex != 0) {
        selectedIndex = 0
    }

    Scaffold(
        containerColor = extended.background,
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            ParkeoBottomBar(
                items = items,
                selectedIndex = selectedIndex,
                onItemSelected = { selectedIndex = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            when {
                isAdmin -> {
                    when (selectedIndex) {
                        0 -> AdminDashboardScreen(
                            viewModel = adminViewModel,
                            onNavigateToParkingLots = { selectedIndex = 1 },
                            onNavigateToUsers = { selectedIndex = 2 }
                        )
                        1 -> AdminParkingLotsScreen(
                            viewModel = adminViewModel,
                            onNavigateBack = { selectedIndex = 0 },
                            onSelectLotDetail = onNavigateToParkingDetail
                        )
                        2 -> AdminUsersScreen(
                            viewModel = adminViewModel,
                            onNavigateBack = { selectedIndex = 0 }
                        )
                        3 -> HomeScreen(
                            viewModel = homeViewModel,
                            onNavigateToParkingDetail = onNavigateToParkingDetail,
                            onNavigateToProfile = { selectedIndex = 4 },
                            onNavigateToReservations = { selectedIndex = 0 }
                        )
                        4 -> ProfileScreen(
                            viewModel = authViewModel,
                            onNavigateBack = { selectedIndex = 0 },
                            onNavigateToVehicles = { /* Admin profile options */ },
                            onNavigateToReservations = { selectedIndex = 0 },
                            onLogout = onLogout
                        )
                    }
                }
                isOperator -> {
                    when (selectedIndex) {
                        0 -> OperatorSpacesScreen(
                            viewModel = operatorViewModel,
                            onNavigateToReservations = { selectedIndex = 1 }
                        )
                        1 -> OperatorReservationsScreen(
                            viewModel = operatorViewModel,
                            onNavigateBack = { selectedIndex = 0 }
                        )
                        2 -> HomeScreen(
                            viewModel = homeViewModel,
                            onNavigateToParkingDetail = onNavigateToParkingDetail,
                            onNavigateToProfile = { selectedIndex = 3 },
                            onNavigateToReservations = { selectedIndex = 1 }
                        )
                        3 -> ProfileScreen(
                            viewModel = authViewModel,
                            onNavigateBack = { selectedIndex = 0 },
                            onNavigateToVehicles = { },
                            onNavigateToReservations = { selectedIndex = 1 },
                            onLogout = onLogout
                        )
                    }
                }
                else -> {
                    // Cliente
                    when (selectedIndex) {
                        0 -> HomeScreen(
                            viewModel = homeViewModel,
                            onNavigateToParkingDetail = onNavigateToParkingDetail,
                            onNavigateToProfile = { selectedIndex = 3 },
                            onNavigateToReservations = { selectedIndex = 1 }
                        )
                        1 -> ReservationsScreen(
                            viewModel = reservationViewModel,
                            onNavigateBack = { selectedIndex = 0 }
                        )
                        2 -> VehiclesScreen(
                            viewModel = vehicleViewModel,
                            onNavigateBack = { selectedIndex = 0 }
                        )
                        3 -> ProfileScreen(
                            viewModel = authViewModel,
                            onNavigateBack = { selectedIndex = 0 },
                            onNavigateToVehicles = { selectedIndex = 2 },
                            onNavigateToReservations = { selectedIndex = 1 },
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }
}
