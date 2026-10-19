/*
 * PARKeo · Android
 * Archivo: ui/viewmodel/SplashViewModel.kt
 * Capa: presentación / estado (ViewModel).
 * Propósito: exponer, como estado observable, si hay sesión iniciada y si el onboarding ya
 * se mostró, para que la pantalla de arranque decida la primera ruta.
 * Conceptos clave: ViewModel, StateFlow, Flow.map, StateFlow.stateIn,
 *   SharingStarted.WhileSubscribed(5000), valor inicial `null` como "todavía desconocido" y
 *   DataStore de preferencias.
 * Flujo: SplashScreen recoge ambos flujos; mientras alguno sea `null` espera. Cuando ambos
 *   tienen valor, navega a inicio si hay sesión o a onboarding en caso contrario.
 * Relación: ParkeoViewModelFactory lo construye con AppContainer.sessionDataStore.
 *   Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones 3, 5 y 14).
 */
package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import pe.parkeo.data.local.SessionDataStore

/**
 * ViewModel de la pantalla de arranque.
 *
 * No necesita un `MutableStateFlow` porque no recibe acciones del usuario: solo transforma
 * flujos de solo lectura de DataStore. El tipo nullable es intencional y representa tres
 * situaciones: `null` = aún no se leyó, `true`/`false` = preferencia conocida.
 *
 * @param sessionDataStore fuente local de la sesión y de la preferencia de onboarding.
 */
class SplashViewModel(
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    /*
     * `SessionDataStore.isLoggedIn` emite Boolean. Se convierte a Boolean? para que el valor
     * inicial de `stateIn` pueda ser `null` y la pantalla distinga "no hay sesión" de
     * "todavía no terminó la lectura". `WhileSubscribed(5000)` mantiene el flujo activo
     * cinco segundos tras la última suscripción para sobrevivir a recreaciones breves.
     */
    val isLoggedIn: StateFlow<Boolean?> = sessionDataStore.isLoggedIn
        .map { it as Boolean? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /*
     * Mismo patrón para la preferencia de onboarding. La pantalla espera a que ambos flujos
     * tengan un valor antes de navegar; así evita mandar al onboarding a un usuario que sí
     * tenía sesión, o viceversa, por una lectura aún pendiente.
     */
    val onboardingShown: StateFlow<Boolean?> = sessionDataStore.onboardingShown
        .map { it as Boolean? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
