package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pe.parkeo.data.local.SessionDataStore

class OnboardingViewModel(
    private val sessionDataStore: SessionDataStore
) : ViewModel() {
    fun markOnboardingShown() {
        viewModelScope.launch {
            sessionDataStore.setOnboardingShown()
        }
    }
}
