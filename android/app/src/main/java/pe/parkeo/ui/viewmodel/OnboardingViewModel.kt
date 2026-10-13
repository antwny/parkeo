// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. ViewModel de presentación; coordina acciones del usuario y publica estado observable para la interfaz.
package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pe.parkeo.data.local.SessionDataStore

class OnboardingViewModel(
    private val sessionDataStore: SessionDataStore
) : ViewModel() {
    /** Ejecuta la operación indicada por el nombre: mark onboarding shown. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    fun markOnboardingShown() {
        viewModelScope.launch {
            sessionDataStore.setOnboardingShown()
        }
    }
}
