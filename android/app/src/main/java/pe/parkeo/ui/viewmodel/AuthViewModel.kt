// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. ViewModel de presentación; coordina acciones del usuario y publica estado observable para la interfaz.
package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.parkeo.data.local.SessionDataStore
import pe.parkeo.domain.repository.AuthRepository
import pe.parkeo.util.Result

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

data class UserProfile(
    val email: String = "",
    val name: String = "",
    val role: String = "CLIENTE"
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isLoggedIn: StateFlow<Boolean> = sessionDataStore.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userProfile: StateFlow<UserProfile?> = sessionDataStore.getUserInfo()
        .map { triple ->
            triple?.let { (email, name, role) ->
                UserProfile(
                    email = email,
                    name = name.ifBlank { "Usuario Parkeo" },
                    role = role
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Autentica las credenciales y actualiza el estado de sesión: login. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.login(email, password)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, isLoggedIn = true, isSuccess = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    /** Registra y devuelve: register. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    fun register(email: String, password: String, firstName: String, lastName: String, phone: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.register(email, password, firstName, lastName, phone)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, isLoggedIn = true, isSuccess = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    /** Cierra la sesión y limpia el estado local/remoto correspondiente: logout. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(isLoggedIn = false, isSuccess = false) }
        }
    }

    /** Limpia el estado asociado: clear error. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /** Restablece el estado inicial: reset success. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    fun resetSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }
}
