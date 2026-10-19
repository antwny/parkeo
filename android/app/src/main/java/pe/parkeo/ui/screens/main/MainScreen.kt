/*
 * PARKeo · Android
 * Archivo: MainScreen.kt
 * Capa: presentación (contenedor autenticado y navegación por pestañas).
 * Propósito: mostrar una experiencia distinta según el rol del usuario (administrador,
 * operador o cliente) sin salir del destino HOME del NavHost.
 * Conceptos clave: Scaffold, ParkeoBottomBar, ParkeoBottomNavItem, estado recordado con
 * rememberSaveable, selección de pestaña, composición condicional por rol, BackHandler,
 * recolección de StateFlow y callbacks de navegación.
 * Flujo: ParkeoNavGraph crea los ViewModels y entrega este contenedor; MainScreen escucha
 * `userProfile`, calcula el rol, arma las pestañas y muestra el contenido de la pestaña
 * seleccionada. Las pestañas no cambian de ruta: el estado local `selectedIndex` decide el
 * contenido. Las acciones que sí salen de home (detalle, perfil, reserva a través del
 * detalle y logout) usan callbacks del grafo.
 * Relación con otros archivos: ParkeoNavGraph.kt define HOME y los callbacks; AuthViewModel
 * aporta el perfil; Home, Admin, Operator, Reservation, Vehicle y Profile son las pantallas
 * internas; ParkeoBottomBar muestra la barra inferior.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones de Compose, estado y roles).
 */
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

/**
 * Contenedor principal de la aplicación autenticada.
 *
 * Recibe todos los ViewModels que pueden necesitar sus pestañas. Esto no significa que
 * todos se usen a la vez: `ParkeoNavGraph` crea las instancias y este contenedor elige las
 * pantallas según el rol. La ventaja es que los ViewModels quedan asociados a la entrada
 * HOME y conservan su estado mientras el usuario cambia de pestaña.
 *
 * El perfil llega de `AuthViewModel.userProfile` como `StateFlow<UserProfile?>`. Mientras
 * sea `null` (lectura inicial de DataStore), se muestra una pantalla de carga y se evita
 * interpretar un rol vacío. Luego se normaliza el rol a mayúsculas y se decide la
 * experiencia. Las pestañas y el índice seleccionado son estado local: cambiar de pestaña
 * no crea otra entrada en el back stack.
 *
 * `onNavigateToReservationCreate` se declara en la firma para que el grafo pueda ofrecer la
 * creación de reserva, pero el cuerpo actual no lo reenvía a ninguna pantalla; el flujo
 * vigente llega a la reserva desde el detalle de cochera. Se documenta para no confundirlo
 * con una funcionalidad ya conectada.
 *
 * @param authViewModel perfil y cierre de sesión.
 * @param homeViewModel búsqueda y mapa de cocheras.
 * @param adminViewModel métricas, cocheras y usuarios del administrador.
 * @param operatorViewModel espacios y reservas del operador.
 * @param reservationViewModel listado y creación de reservas del cliente.
 * @param vehicleViewModel vehículos del cliente.
 * @param onNavigateToParkingDetail callback al grafo para abrir el detalle de una cochera.
 * @param onNavigateToReservationCreate callback al grafo para abrir el formulario de reserva.
 * @param onLogout callback al grafo para cerrar sesión y limpiar el back stack.
 */
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
    // Perfil observable: null mientras DataStore aún no entrega los datos.
    val userProfile by authViewModel.userProfile.collectAsState()
    val extended = ParkeoTheme.colors

    // Estado transitorio: sin perfil no se puede decidir el rol. Se muestra carga y se
    // corta la composición para no ejecutar las ramas de pestañas con datos incompletos.
    if (userProfile == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ParkeoLoadingView(message = "Cargando perfil...")
        }
        return
    }

    // Normalización del rol: el backend puede devolver mayúsculas o minúsculas. "OPERAT"
    // cubre variantes como OPERATOR y "OPERADOR" la forma en español.
    val roleUpper = (userProfile?.role ?: "").uppercase()
    val isAdmin = roleUpper.contains("ADMIN")
    val isOperator = !isAdmin && (roleUpper.contains("OPERAT") || roleUpper.contains("OPERADOR"))

    // Elementos de la barra inferior según el rol. `remember` evita reconstruir la lista
    // mientras el rol no cambie. La lista y el índice deben permanecer sincronizados.
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

    // Índice de la pestaña activa. `rememberSaveable` lo conserva si la Activity se recrea;
    // no se guarda en el NavController porque las pestañas no son destinos del grafo.
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    // Si el rol cambia y la nueva lista tiene menos pestañas, se vuelve a la primera para
    // evitar un índice fuera de rango.
    LaunchedEffect(items.size) {
        if (selectedIndex >= items.size) {
            selectedIndex = 0
        }
    }

    // Botón Atrás del sistema: si el usuario está en una pestaña secundaria, primero
    // vuelve a la primera pestaña; solo cuando ya está en ella el sistema decide salir.
    BackHandler(enabled = selectedIndex != 0) {
        selectedIndex = 0
    }

    // Navegación inferior: Scaffold del contenedor principal. Los insets se dejan en cero
    // porque cada pantalla interna gestiona su propia barra superior y el contenedor
    // compensa solo la barra inferior. La barra de navegación se dibuja como `bottomBar`.
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
        // Contenido de la pestaña activa. Solo se aplica el padding inferior que ocupa la
        // barra de navegación; el resto de insets lo resuelven las pantallas internas.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            when {
                // Experiencia ADMIN: métricas, cocheras, usuarios, exploración y perfil.
                // Los índices coinciden con el orden de `items`.
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
                            onNavigateToDashboard = { selectedIndex = 0 },
                            onNavigateToParkingLots = { selectedIndex = 1 },
                            onNavigateToUsers = { selectedIndex = 2 },
                            onNavigateToExplore = { selectedIndex = 3 },
                            onLogout = onLogout
                        )
                    }
                }
                // Experiencia OPERADOR: matriz de espacios, reservas, exploración y perfil.
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
                            onNavigateToSpaces = { selectedIndex = 0 },
                            onNavigateToReservations = { selectedIndex = 1 },
                            onNavigateToExplore = { selectedIndex = 2 },
                            onLogout = onLogout
                        )
                    }
                }
                else -> {
                    // Experiencia CLIENTE (cualquier rol distinto de admin/operador):
                    // explorar, reservas, vehículos y perfil.
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
                            onNavigateToExplore = { selectedIndex = 0 },
                            onNavigateToReservations = { selectedIndex = 1 },
                            onNavigateToVehicles = { selectedIndex = 2 },
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }
}
