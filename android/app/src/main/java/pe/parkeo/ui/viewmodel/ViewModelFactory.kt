/*
 * PARKeo · Android
 * Archivo: ui/viewmodel/ViewModelFactory.kt
 * Capa: presentación / inyección de dependencias manual (patrón Factory).
 * Propósito: construir cada ViewModel con las dependencias que necesita, ya ensambladas
 * por `AppContainer`, sin que las pantallas conozcan Retrofit, DataStore ni repositorios.
 * Conceptos clave: ViewModelProvider.Factory, patrón Factory, `create(modelClass)`,
 *   inyección de dependencias manual, `AppContainer`, `isAssignableFrom`, conversión
 *   unchecked y ausencia de Hilt/Dagger.
 * Flujo: Android pide un ViewModel a esta fábrica (por ejemplo desde
 *   `viewModel(factory = ...)` en ParkeoNavGraph). La fábrica compara la clase solicitada y
 *   devuelve una instancia nueva con los repositorios/DataStore del contenedor.
 * Relación: `ParkeoApplication` crea `DefaultAppContainer`; ParkeoApp/MainActivity lo
 *   entregan a esta clase; `AppContainer` expone `sessionDataStore` y los seis repositorios.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones 5 y 14).
 */
package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import pe.parkeo.di.AppContainer

/**
 * Fábrica única de todos los ViewModels de PARKeo.
 *
 * El proyecto usa inyección de dependencias manual en lugar de Hilt: no hay anotaciones,
 * procesadores ni grafo generado. `AppContainer` actúa como contenedor de composición y
 * esta clase decide qué dependencias recibe cada ViewModel. Android conserva las instancias
 * en el `ViewModelStore` del propietario, así que `create` se invoca solo cuando hace falta
 * una instancia nueva (por ejemplo, al entrar por primera vez a una ruta).
 *
 * @param appContainer contenedor con el `SessionDataStore` y los repositorios de dominio.
 */
class ParkeoViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {

    /*
     * La firma de la interfaz devuelve `T` genérico, pero cada rama conoce el tipo concreto.
     * El compilador no puede demostrar esa correspondencia, de ahí la conversión `as T` y la
     * supresión de la advertencia. La comprobación con `isAssignableFrom` garantiza que la
     * clase solicitada sea compatible antes de convertir.
     */
    @Suppress("UNCHECKED_CAST")
    /**
     * Crea la instancia del ViewModel solicitado.
     *
     * Se compara con `isAssignableFrom` en lugar de igualdad para aceptar subclases o la
     * propia clase. Cada rama inyecta solo lo necesario: los ViewModels de sesión reciben
     * `SessionDataStore`; los de datos reciben el repositorio de su dominio. Si la clase no
     * está registrada se lanza una excepción de programación, porque indica que se olvidó
     * añadir la rama correspondiente.
     *
     * @param modelClass clase del ViewModel que Android quiere construir.
     * @return instancia nueva del ViewModel, con sus dependencias inyectadas.
     * @throws IllegalArgumentException si `modelClass` no corresponde a ningún ViewModel
     *   conocido por la fábrica.
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            /* Arranque: solo necesita leer la sesión y el onboarding guardados. */
            modelClass.isAssignableFrom(SplashViewModel::class.java) -> {
                SplashViewModel(appContainer.sessionDataStore) as T
            }
            /* Introducción: solo escribe la preferencia de onboarding. */
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> {
                OnboardingViewModel(appContainer.sessionDataStore) as T
            }
            /* Autenticación: repositorio de auth más almacenamiento de la sesión. */
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(appContainer.authRepository, appContainer.sessionDataStore) as T
            }
            /* Inicio: catálogo, búsqueda y cercanía de estacionamientos. */
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(appContainer.parkingRepository) as T
            }
            /* Detalle: comparte el repositorio de estacionamientos. */
            modelClass.isAssignableFrom(ParkingDetailViewModel::class.java) -> {
                ParkingDetailViewModel(appContainer.parkingRepository) as T
            }
            /* Vehículos: alta, baja y catálogo de tipos. */
            modelClass.isAssignableFrom(VehicleViewModel::class.java) -> {
                VehicleViewModel(appContainer.vehicleRepository) as T
            }
            /* Reservas: listado, creación y cancelación. */
            modelClass.isAssignableFrom(ReservationViewModel::class.java) -> {
                ReservationViewModel(appContainer.reservationRepository) as T
            }
            /* Administración: métricas, cocheras y usuarios. */
            modelClass.isAssignableFrom(AdminViewModel::class.java) -> {
                AdminViewModel(appContainer.adminRepository) as T
            }
            /* Operador: cocheras asignadas, espacios y check-in/check-out. */
            modelClass.isAssignableFrom(OperatorViewModel::class.java) -> {
                OperatorViewModel(appContainer.operatorRepository) as T
            }
            /* Clase no registrada: es un error de desarrollo, no una condición de usuario. */
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
