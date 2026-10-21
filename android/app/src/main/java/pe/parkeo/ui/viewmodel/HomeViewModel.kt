package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.parkeo.data.remote.dto.ParkingLotDto
import pe.parkeo.domain.repository.ParkingRepository
import pe.parkeo.util.Result

data class HomeUiState(
    val isLoading: Boolean = false,
    val parkingLots: List<ParkingLotDto> = emptyList(),
    val error: String? = null,
    val searchQuery: String = ""
)

class HomeViewModel(
    private val parkingRepository: ParkingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Fuente de cambios de búsqueda: debounce + cancelación (collectLatest). */
    private val searchQueries = MutableSharedFlow<String>(extraBufferCapacity = 8)

    init {
        loadParkingLots()
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            searchQueries
                .debounce(300)
                .collectLatest { query ->
                    if (query.isBlank()) {
                        val result = parkingRepository.getParkingLots()
                        if (result is Result.Success) {
                            _uiState.update { it.copy(parkingLots = result.data, error = null, searchQuery = "") }
                        }
                    } else {
                        when (val result = parkingRepository.searchParkingLots(query)) {
                            is Result.Success -> _uiState.update { it.copy(parkingLots = result.data, error = null) }
                            is Result.Error -> _uiState.update { it.copy(error = result.message) }
                            else -> {}
                        }
                    }
                }
        }
    }

    fun loadParkingLots() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = parkingRepository.getParkingLots()) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, parkingLots = result.data) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    fun loadNearbyParkingLots(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = parkingRepository.getNearbyParkingLots(lat, lon)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, parkingLots = result.data) }
                is Result.Error -> loadParkingLots()
                else -> {}
            }
        }
    }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchQueries.tryEmit(query)
    }
}
