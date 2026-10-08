package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.parkeo.data.remote.dto.AdminStatisticsDto
import pe.parkeo.data.remote.dto.ParkingLotDto
import pe.parkeo.data.remote.dto.UserDto
import pe.parkeo.domain.repository.AdminRepository
import pe.parkeo.util.Result

data class AdminUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val statistics: AdminStatisticsDto = AdminStatisticsDto(),
    val parkingLots: List<ParkingLotDto> = emptyList(),
    val users: List<UserDto> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

class AdminViewModel(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    fun loadDashboardData(refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            // Load statistics
            when (val statsResult = adminRepository.getStatistics()) {
                is Result.Success -> {
                    _uiState.update { it.copy(statistics = statsResult.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = statsResult.message) }
                }
                else -> {}
            }

            // Load parking lots
            when (val lotsResult = adminRepository.getParkingLots()) {
                is Result.Success -> {
                    _uiState.update { it.copy(parkingLots = lotsResult.data) }
                }
                is Result.Error -> {
                    // non-fatal if stats already loaded
                }
                else -> {}
            }

            // Load users
            when (val usersResult = adminRepository.getUsers()) {
                is Result.Success -> {
                    _uiState.update { it.copy(users = usersResult.data) }
                }
                is Result.Error -> {
                    // non-fatal
                }
                else -> {}
            }

            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
        }
    }

    fun toggleParkingLotStatus(id: Long, isOpen: Boolean?, isActive: Boolean?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = adminRepository.updateParkingLotStatus(id, isOpen, isActive)) {
                is Result.Success -> {
                    val updatedLots = _uiState.value.parkingLots.map { lot ->
                        if (lot.id == id) result.data else lot
                    }
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            parkingLots = updatedLots,
                            successMessage = "Estado de cochera actualizado"
                        ) 
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun assignOperator(parkingLotId: Long, operatorId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = adminRepository.assignOperator(parkingLotId, operatorId)) {
                is Result.Success -> {
                    val updatedLots = _uiState.value.parkingLots.map { lot ->
                        if (lot.id == parkingLotId) result.data else lot
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            parkingLots = updatedLots,
                            successMessage = "Operador asignado exitosamente"
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun toggleUserStatus(id: Long, currentActive: Boolean) {
        viewModelScope.launch {
            val targetActive = !currentActive
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = adminRepository.updateUserStatus(id, targetActive)) {
                is Result.Success -> {
                    val updatedUsers = _uiState.value.users.map { user ->
                        if (user.id == id) result.data else user
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            users = updatedUsers,
                            successMessage = if (targetActive) "Usuario activado" else "Usuario desactivado"
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
