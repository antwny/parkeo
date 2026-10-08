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
    val reservationCreated: Boolean = false,
    // Id de la última reserva cancelada con éxito (la pantalla lo usa para cancelar sus recordatorios)
    val cancelledReservationId: Long? = null
)

class ReservationViewModel(
    private val reservationRepository: ReservationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReservationUiState())
    val uiState: StateFlow<ReservationUiState> = _uiState.asStateFlow()

    // Última pestaña consultada, para recargar la misma después de cancelar
    private var lastStatus: String? = null

    fun loadReservations(status: String? = null) {
        lastStatus = status
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = reservationRepository.getMyReservations(status)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, reservations = result.data) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    /**
     * parkingSpaceId = null -> el servidor asigna un espacio libre del estacionamiento.
     * startTime / endTime en formato ISO yyyy-MM-dd'T'HH:mm:ss (hora de Lima).
     */
    fun createReservation(
        parkingLotId: Long,
        parkingSpaceId: Long? = null,
        vehicleId: Long,
        startTime: String,
        endTime: String,
        notes: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = reservationRepository.createReservation(
                parkingLotId = parkingLotId,
                parkingSpaceId = parkingSpaceId,
                vehicleId = vehicleId,
                startTime = startTime,
                endTime = endTime,
                notes = notes
            )
            when (result) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, currentReservation = result.data, reservationCreated = true)
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    fun cancelReservation(id: Long, reason: String? = null) {
        viewModelScope.launch {
            when (val result = reservationRepository.cancelReservation(id, reason)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(successMessage = "Reserva cancelada", cancelledReservationId = id)
                    }
                    loadReservations(lastStatus)
                }
                is Result.Error -> _uiState.update { it.copy(error = result.message) }
                else -> {}
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                error = null,
                successMessage = null,
                reservationCreated = false,
                cancelledReservationId = null
            )
        }
    }
}