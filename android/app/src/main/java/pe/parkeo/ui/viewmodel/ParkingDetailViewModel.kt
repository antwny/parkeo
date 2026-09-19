package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.parkeo.data.remote.dto.ParkingLotDetailDto
import pe.parkeo.domain.repository.ParkingRepository
import pe.parkeo.util.Result

data class ParkingDetailUiState(
    val isLoading: Boolean = false,
    val parkingLot: ParkingLotDetailDto? = null,
    val error: String? = null
)

class ParkingDetailViewModel(
    private val parkingRepository: ParkingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ParkingDetailUiState())
    val uiState: StateFlow<ParkingDetailUiState> = _uiState.asStateFlow()

    fun loadParkingLot(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = parkingRepository.getParkingLotDetail(id)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, parkingLot = result.data) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }
}
