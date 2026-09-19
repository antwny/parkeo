package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import pe.parkeo.data.local.SessionDataStore

class SplashViewModel(
    private val sessionDataStore: SessionDataStore
) : ViewModel() {
    val isLoggedIn: StateFlow<Boolean?> = sessionDataStore.isLoggedIn
        .map { it as Boolean? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val onboardingShown: StateFlow<Boolean?> = sessionDataStore.onboardingShown
        .map { it as Boolean? }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
