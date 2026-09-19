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
        composable(NavRoutes.SPLASH) {
            val splashViewModel: SplashViewModel = viewModel(factory = viewModelFactory)
            SplashScreen(
                viewModel = splashViewModel,
                onNavigateToOnboarding = {
                    navController.navigate(NavRoutes.ONBOARDING) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.ONBOARDING) {
            val onboardingViewModel: OnboardingViewModel = viewModel(factory = viewModelFactory)
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onFinish = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.LOGIN) {
            val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    authViewModel.resetSuccess()
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(NavRoutes.REGISTER) }
            )
        }

        composable(NavRoutes.REGISTER) {
            val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    authViewModel.resetSuccess()
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.REGISTER) { inclusive = true }
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

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
                    navController.navigate(NavRoutes.parkingDetail(id))
                },
                onNavigateToReservationCreate = { id ->
                    navController.navigate(NavRoutes.reservationCreate(id))
                },
                onLogout = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = NavRoutes.PARKING_DETAIL,
            arguments = listOf(navArgument("parkingId") { type = NavType.LongType })
        ) { backStackEntry ->
            val parkingId = backStackEntry.arguments?.getLong("parkingId") ?: return@composable
            val detailViewModel: ParkingDetailViewModel = viewModel(factory = viewModelFactory)
            ParkingDetailScreen(
                parkingId = parkingId,
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReservation = { id ->
                    navController.navigate(NavRoutes.reservationCreate(id))
                }
            )
        }

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
                    navController.navigate(NavRoutes.RESERVATIONS) {
                        popUpTo(NavRoutes.HOME)
                    }
                }
            )
        }

        composable(NavRoutes.RESERVATIONS) {
            val reservationViewModel: ReservationViewModel = viewModel(factory = viewModelFactory)
            ReservationsScreen(
                viewModel = reservationViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.VEHICLES) {
            val vehicleViewModel: VehicleViewModel = viewModel(factory = viewModelFactory)
            VehiclesScreen(
                viewModel = vehicleViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.PROFILE) {
            val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
            ProfileScreen(
                viewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToVehicles = { navController.navigate(NavRoutes.VEHICLES) },
                onNavigateToReservations = { navController.navigate(NavRoutes.RESERVATIONS) },
                onLogout = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
