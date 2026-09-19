package pe.parkeo.ui.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import pe.parkeo.ui.screens.admin.AdminDashboardScreen
import pe.parkeo.ui.screens.admin.AdminParkingLotsScreen
import pe.parkeo.ui.screens.admin.AdminUsersScreen
import pe.parkeo.ui.screens.home.HomeScreen
import pe.parkeo.ui.screens.operator.OperatorSpacesScreen
import pe.parkeo.ui.screens.profile.ProfileScreen
import pe.parkeo.ui.screens.reservation.ReservationsScreen
import pe.parkeo.ui.screens.vehicles.VehiclesScreen
import pe.parkeo.ui.viewmodel.*

private data class NavItem(
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

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

    if (userProfile == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
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
                NavItem("Métricas", Icons.Filled.Dashboard),
                NavItem("Cocheras", Icons.Filled.LocalParking),
                NavItem("Usuarios", Icons.Filled.Group),
                NavItem("Explorar", Icons.Filled.Map),
                NavItem("Perfil", Icons.Filled.Person)
            )
            isOperator -> listOf(
                NavItem("Espacios", Icons.Filled.GridView),
                NavItem("Reservas", Icons.Filled.BookmarkBorder),
                NavItem("Explorar", Icons.Filled.Map),
                NavItem("Perfil", Icons.Filled.Person)
            )
            else -> listOf(
                NavItem("Explorar", Icons.Filled.Map),
                NavItem("Reservas", Icons.Filled.BookmarkBorder),
                NavItem("Vehículos", Icons.Filled.DirectionsCar),
                NavItem("Perfil", Icons.Filled.Person)
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
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index }
                    )
                }
            }
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
                        1 -> ReservationsScreen(
                            viewModel = reservationViewModel,
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
