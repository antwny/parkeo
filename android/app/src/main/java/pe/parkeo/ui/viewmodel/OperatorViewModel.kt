package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.parkeo.data.remote.dto.ParkingLotDto
import pe.parkeo.data.remote.dto.ParkingSpaceDto
import pe.parkeo.data.remote.dto.ReservationDto
import pe.parkeo.domain.repository.OperatorRepository
import pe.parkeo.util.Result

data class OperatorUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val parkingLots: List<ParkingLotDto> = emptyList(),
    val selectedLot: ParkingLotDto? = null,
    val spaces: List<ParkingSpaceDto> = emptyList(),
    val reservations: List<ReservationDto> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null,
    val successMessage: String? = null
)

class OperatorViewModel(
    private val operatorRepository: OperatorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OperatorUiState())
    val uiState: StateFlow<OperatorUiState> = _uiState.asStateFlow()

    fun loadMyParkingLots(refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            when (val result = operatorRepository.getMyParkingLots()) {
                is Result.Success -> {
                    val lots = result.data
                    val currentSelected = _uiState.value.selectedLot
                    val selected = if (currentSelected != null && lots.any { it.id == currentSelected.id }) {
                        lots.first { it.id == currentSelected.id }
                    } else {
                        lots.firstOrNull()
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            parkingLots = lots,
                            selectedLot = selected
                        )
                    }

                    if (selected != null) {
                        loadSpacesAndReservations(selected.id)
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, isRefreshing = false, error = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun selectParkingLot(lot: ParkingLotDto) {
        _uiState.update { it.copy(selectedLot = lot) }
        loadSpacesAndReservations(lot.id)
    }

    fun loadSpacesAndReservations(lotId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load spaces
            when (val spacesResult = operatorRepository.getSpaces(lotId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(spaces = spacesResult.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = spacesResult.message) }
                }
                else -> {}
            }

            // Load reservations
            when (val reservationsResult = operatorRepository.getReservations(lotId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(reservations = reservationsResult.data) }
                }
                is Result.Error -> {}
                else -> {}
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun updateSpaceStatus(spaceId: Long, newStatus: String, notes: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = operatorRepository.updateSpaceStatus(spaceId, newStatus, notes)) {
                is Result.Success -> {
                    val updatedSpaces = _uiState.value.spaces.map { space ->
                        if (space.id == spaceId) result.data else space
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            spaces = updatedSpaces,
                            successMessage = "Espacio actualizado a $newStatus"
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

    fun checkIn(reservationId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = operatorRepository.checkIn(reservationId)) {
                is Result.Success -> {
                    val updatedList = _uiState.value.reservations.map {
                        if (it.id == reservationId) result.data else it
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            reservations = updatedList,
                            successMessage = "¡Ingreso registrado exitosamente!"
                        )
                    }
                    // Actualizar espacios en vivo
                    _uiState.value.selectedLot?.id?.let { loadSpacesAndReservations(it) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun checkOut(reservationId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = operatorRepository.checkOut(reservationId)) {
                is Result.Success -> {
                    val updatedList = _uiState.value.reservations.map {
                        if (it.id == reservationId) result.data else it
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            reservations = updatedList,
                            successMessage = "¡Salida registrada exitosamente!"
                        )
                    }
                    // Actualizar espacios en vivo
                    _uiState.value.selectedLot?.id?.let { loadSpacesAndReservations(it) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
