package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.parkeo.data.remote.dto.ReservationDto
import pe.parkeo.domain.repository.ReservationRepository
import pe.parkeo.util.Result

data class ReservationUiState(
    val isLoading: Boolean = false,
    val reservations: List<ReservationDto> = emptyList(),
    val currentReservation: ReservationDto? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val reservationCreated: Boolean = false
)

class ReservationViewModel(
    private val reservationRepository: ReservationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReservationUiState())
    val uiState: StateFlow<ReservationUiState> = _uiState.asStateFlow()

    fun loadReservations(status: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = reservationRepository.getMyReservations(status)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, reservations = result.data) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    fun createReservation(parkingSpaceId: Long, vehicleId: Long, startTime: String, endTime: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = reservationRepository.createReservation(parkingSpaceId, vehicleId, startTime, endTime)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, currentReservation = result.data, reservationCreated = true)
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    fun cancelReservation(id: Long) {
        viewModelScope.launch {
            when (val result = reservationRepository.cancelReservation(id)) {
                is Result.Success -> loadReservations()
                is Result.Error -> _uiState.update { it.copy(error = result.message) }
                else -> {}
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null, reservationCreated = false) }
    }
}
