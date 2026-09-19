package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pe.parkeo.di.AppContainer

class ParkeoViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SplashViewModel::class.java) -> {
                SplashViewModel(appContainer.sessionDataStore) as T
            }
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> {
                OnboardingViewModel(appContainer.sessionDataStore) as T
            }
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(appContainer.authRepository, appContainer.sessionDataStore) as T
            }
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(appContainer.parkingRepository) as T
            }
            modelClass.isAssignableFrom(ParkingDetailViewModel::class.java) -> {
                ParkingDetailViewModel(appContainer.parkingRepository) as T
            }
            modelClass.isAssignableFrom(VehicleViewModel::class.java) -> {
                VehicleViewModel(appContainer.vehicleRepository) as T
            }
            modelClass.isAssignableFrom(ReservationViewModel::class.java) -> {
                ReservationViewModel(appContainer.reservationRepository) as T
            }
            modelClass.isAssignableFrom(AdminViewModel::class.java) -> {
                AdminViewModel(appContainer.adminRepository) as T
            }
            modelClass.isAssignableFrom(OperatorViewModel::class.java) -> {
                OperatorViewModel(appContainer.operatorRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
