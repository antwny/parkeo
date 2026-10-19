/*
 * PARKeo · Android
 * Archivo: ParkeoNavGraph.kt
 * Capa: navegación (definición del grafo de destinos y transiciones).
 * Propósito: declarar cada pantalla del flujo, obtener sus ViewModels desde la factory y
 * conectar los callbacks de la UI con el NavController.
 * Conceptos clave: NavHost, NavHostController, composable, back stack, rutas, argumentos,
 * NavType.LongType, viewModel(factory = ...), ViewModel scoped a un NavBackStackEntry,
 * popUpTo, popUpTo inclusivo, navegación de limpieza y ausencia de launchSingleTop.
 * Flujo general: splash decide onboarding o home; onboarding termina en login; login puede
 * ir a registro; el éxito de autenticación entra a home; desde home se abre el detalle de
 * cochera y desde allí la creación de reserva; las reservas, vehículos y perfil se abren
 * como destinos secundarios. El cierre de sesión limpia el back stack y vuelve a login.
 * Relación con otros archivos: NavRoutes.kt aporta las rutas; MainActivity/ParkeoApp crean
 * el NavController y la factory; los ViewModels y pantallas están en ui/viewmodel y
 * ui/screens; AppContainer suministra los repositorios que usa la factory.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones de Compose, navegación y
 * ViewModel).
 */
package pe.parkeo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pe.parkeo.ui.screens.auth.LoginScreen
import pe.parkeo.ui.screens.auth.RegisterScreen
import pe.parkeo.ui.screens.home.HomeScreen
import pe.parkeo.ui.screens.main.MainScreen
import pe.parkeo.ui.screens.onboarding.OnboardingScreen
import pe.parkeo.ui.screens.parking.ParkingDetailScreen
import pe.parkeo.ui.screens.profile.ProfileScreen
import pe.parkeo.ui.screens.reservation.ReservationCreateScreen
import pe.parkeo.ui.screens.reservation.ReservationsScreen
import pe.parkeo.ui.screens.splash.SplashScreen
import pe.parkeo.ui.screens.vehicles.VehiclesScreen
import pe.parkeo.ui.viewmodel.*

/**
 * Construye el grafo de navegación completo de PARKeo.
 *
 * `NavHost` es el contenedor que asocia cada ruta con un bloque `composable` y mantiene el
 * back stack. Al cambiar de destino, el host reemplaza el contenido visible; las entradas
 * anteriores permanecen en la pila hasta que se hace `popBackStack` o se limpian con
 * `popUpTo`. La navegación no crea Activities nuevas: todo ocurre dentro de la composición.
 *
 * Cada bloque obtiene su ViewModel con `viewModel(factory = viewModelFactory)`. Por defecto,
 * ese ViewModel queda asociado al `NavBackStackEntry` del destino: sobrevive a
 * recomposiciones mientras la entrada esté en la pila y se libera cuando sale de ella. La
 * factory construye instancias con los repositorios del `AppContainer`; no se usa Hilt.
 *
 * Los callbacks de navegación se declaran como lambdas para que las pantallas no conozcan
 * al NavController ni las rutas. Así la UI queda desacoplada y el grafo concentra las
 * decisiones de transición.
 *
 * En este grafo no se usa `launchSingleTop`. Por tanto, si una acción navegara dos veces
 * seguidas al mismo destino antes de que la primera entrada se consuma, se podrían apilar
 * dos copias. Los flujos actuales evitan ese caso porque cada callback se ejecuta desde una
 * acción puntual y las transiciones de autenticación limpian el back stack con `popUpTo`.
 *
 * @param viewModelFactory factory que crea `SplashViewModel`, `AuthViewModel`, etc., con
 * sus dependencias. Debe provenir de `ParkeoApp`.
 * @param navController controlador del back stack. Se recibe para poder compartirlo y para
 * facilitar pruebas; si no se pasa, se recuerda uno nuevo dentro del grafo.
 * @param startDestination ruta inicial. Por defecto es splash; se puede sustituir en
 * pruebas o vistas previas para arrancar directamente en otro destino.
 */
@Composable
fun ParkeoNavGraph(
    viewModelFactory: ParkeoViewModelFactory,
    navController: NavHostController = rememberNavController(),
    startDestination: String = NavRoutes.SPLASH
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Destino inicial: splash. El ViewModel observa la sesión y el onboarding;
        // la pantalla anima la marca y, tras una espera, avisa al grafo a dónde ir.
        composable(NavRoutes.SPLASH) {
            val splashViewModel: SplashViewModel = viewModel(factory = viewModelFactory)
            SplashScreen(
                viewModel = splashViewModel,
                onNavigateToOnboarding = {
                    // `popUpTo(SPLASH) { inclusive = true }` saca el splash del back stack:
                    // al llegar al onboarding, Atrás no debe volver a la pantalla de marca.
                    navController.navigate(NavRoutes.ONBOARDING) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    // Si ya hay sesión, se entra directo a home y se elimina splash.
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // Onboarding: el ViewModel persiste que ya se vio la introducción. Al terminar,
        // se navega a login eliminando onboarding para que no reaparezca con Atrás.
        composable(NavRoutes.ONBOARDING) {
            val onboardingViewModel: OnboardingViewModel = viewModel(factory = viewModelFactory)
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onFinish = {
                    // Se elimina onboarding del back stack antes de mostrar login.
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // Login: comparte AuthViewModel con el registro, pero cada entrada del back stack
        // obtiene su propia instancia porque el ViewModel está asociado al destino.
        composable(NavRoutes.LOGIN) {
            val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    // Se consume la señal de éxito para que no vuelva a dispararse si la
                    // pantalla se recompone, y se entra a home limpiando el login.
                    authViewModel.resetSuccess()
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                },
                // Ir a registro no limpia login: el usuario puede volver con Atrás.
                onNavigateToRegister = { navController.navigate(NavRoutes.REGISTER) }
            )
        }

        // Registro: al completarse, `resetSuccess` evita repetir la navegación y se limpia
        // el flujo de autenticación antes de entrar a home.
        composable(NavRoutes.REGISTER) {
            val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    authViewModel.resetSuccess()
                    // Se declaran dos `popUpTo` para expresar que el registro actual y el
                    // login previo deben quedar fuera del back stack. El resultado buscado
                    // es que home sea la única entrada visible al volver con Atrás.
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.REGISTER) { inclusive = true }
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                },
                // La flecha de la barra superior vuelve al login sin crear otro destino.
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Home: contenedor autenticado. Se crean varias instancias de ViewModel porque
        // MainScreen reúne pestañas de cliente, operador y administrador. Cada ViewModel
        // queda limitado a esta entrada del back stack.
        composable(NavRoutes.HOME) {
            val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
            val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
            val adminViewModel: AdminViewModel = viewModel(factory = viewModelFactory)
            val operatorViewModel: OperatorViewModel = viewModel(factory = viewModelFactory)
            val reservationViewModel: ReservationViewModel = viewModel(factory = viewModelFactory)
            val vehicleViewModel: VehicleViewModel = viewModel(factory = viewModelFactory)

            MainScreen(
                authViewModel = authViewModel,
                homeViewModel = homeViewModel,
                adminViewModel = adminViewModel,
                operatorViewModel = operatorViewModel,
                reservationViewModel = reservationViewModel,
                vehicleViewModel = vehicleViewModel,
                onNavigateToParkingDetail = { id ->
                    // La función constructora sustituye `{parkingId}` por el Long real.
                    navController.navigate(NavRoutes.parkingDetail(id))
                },
                onNavigateToReservationCreate = { id ->
                    // Destino independiente para reservar; conserva home en el back stack.
                    navController.navigate(NavRoutes.reservationCreate(id))
                },
                onLogout = {
                    // `popUpTo(0)` con inclusive vacía todo el back stack y luego abre login:
                    // después de cerrar sesión no se puede volver a home con Atrás.
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Detalle de cochera: recibe `parkingId` como Long. Si el argumento faltara, el
        // bloque termina sin componer nada (salida defensiva) en lugar de usar un valor 0.
        composable(
            route = NavRoutes.PARKING_DETAIL,
            arguments = listOf(navArgument("parkingId") { type = NavType.LongType })
        ) { backStackEntry ->
            val parkingId = backStackEntry.arguments?.getLong("parkingId") ?: return@composable
            val detailViewModel: ParkingDetailViewModel = viewModel(factory = viewModelFactory)
            ParkingDetailScreen(
                parkingId = parkingId,
                viewModel = detailViewModel,
                // Volver al destino anterior sin apilar una copia del detalle.
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReservation = { id ->
                    // El detalle delega en el grafo la construcción de la ruta de reserva.
                    navController.navigate(NavRoutes.reservationCreate(id))
                }
            )
        }

        // Creación de reserva: el mismo `parkingId` de la ruta alimenta tres ViewModels:
        // detalle de la cochera, vehículos disponibles y creación de la reserva.
        composable(
            route = NavRoutes.RESERVATION_CREATE,
            arguments = listOf(navArgument("parkingId") { type = NavType.LongType })
        ) { backStackEntry ->
            val parkingId = backStackEntry.arguments?.getLong("parkingId") ?: return@composable
            val detailViewModel: ParkingDetailViewModel = viewModel(factory = viewModelFactory)
            val vehicleViewModel: VehicleViewModel = viewModel(factory = viewModelFactory)
            val reservationViewModel: ReservationViewModel = viewModel(factory = viewModelFactory)
            ReservationCreateScreen(
                parkingId = parkingId,
                parkingViewModel = detailViewModel,
                vehicleViewModel = vehicleViewModel,
                reservationViewModel = reservationViewModel,
                onNavigateBack = { navController.popBackStack() },
                onReservationCreated = {
                    // `popUpTo(HOME)` sin `inclusive` retira creación y detalle, pero conserva
                    // home debajo; luego se apila el listado de reservas.
                    navController.navigate(NavRoutes.RESERVATIONS) {
                        popUpTo(NavRoutes.HOME)
                    }
                }
            )
        }

        // Listado de reservas: destino secundario al que también se llega desde el perfil.
        composable(NavRoutes.RESERVATIONS) {
            val reservationViewModel: ReservationViewModel = viewModel(factory = viewModelFactory)
            ReservationsScreen(
                viewModel = reservationViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Vehículos del cliente: se abre desde home o desde el perfil.
        composable(NavRoutes.VEHICLES) {
            val vehicleViewModel: VehicleViewModel = viewModel(factory = viewModelFactory)
            VehiclesScreen(
                viewModel = vehicleViewModel,
                // Volver al listado anterior; no se duplica la entrada.
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Perfil independiente: el mismo composable también se usa como pestaña dentro de
        // MainScreen. Este destino permite abrirlo fuera de las pestañas y conecta sus
        // accesos con rutas reales del grafo.
        composable(NavRoutes.PROFILE) {
            val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
            ProfileScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVehicles = { navController.navigate(NavRoutes.VEHICLES) },
                onNavigateToReservations = { navController.navigate(NavRoutes.RESERVATIONS) },
                onLogout = {
                    // Igual que en home: se vacía el back stack antes de abrir login.
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
