/*
 * PARKeo · Android
 * Archivo: ui/viewmodel/OnboardingViewModel.kt
 * Capa: presentación / estado (ViewModel).
 * Propósito: recordar que el usuario ya vio la introducción para no volver a mostrarla en
 * cada arranque de la aplicación.
 * Conceptos clave: ViewModel, viewModelScope, corrutinas, DataStore de preferencias y
 *   operación de escritura sin estado observable propio.
 * Flujo: OnboardingScreen llama a `markOnboardingShown` al terminar; la escritura se hace
 *   en segundo plano. SplashViewModel lee el mismo indicador para decidir si entra al
 *   onboarding o al inicio.
 * Relación: ParkeoViewModelFactory lo construye con AppContainer.sessionDataStore.
 *   Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones 3, 5 y 14).
 */
package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pe.parkeo.data.local.SessionDataStore

/**
 * ViewModel mínimo para la pantalla de introducción.
 *
 * No publica un `UiState`: la pantalla no necesita observar progreso ni errores, solo
 * disparar una escritura. Aun así se usa un ViewModel para no retener el contexto de la
 * Activity ni ejecutar DataStore en el hilo principal.
 *
 * @param sessionDataStore almacenamiento local donde se guarda la preferencia
 *   `onboarding_shown`.
 */
class OnboardingViewModel(
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    /**
     * Persiste que la introducción ya fue vista.
     *
     * La escritura de DataStore es suspendida y se ejecuta en `viewModelScope`, por lo que
     * no bloquea la UI y se cancela si el ViewModel desaparece. No se espera confirmación
     * ni se navega desde aquí: la pantalla avanza inmediatamente y SplashViewModel leerá el
     * valor en el próximo arranque. Si la escritura fallara, no hay manejo de error visible
     * en este nivel; la consecuencia sería volver a mostrar el onboarding.
     *
     * @return no devuelve un valor.
     */
    fun markOnboardingShown() {
        viewModelScope.launch {
            sessionDataStore.setOnboardingShown()
        }
    }
}
