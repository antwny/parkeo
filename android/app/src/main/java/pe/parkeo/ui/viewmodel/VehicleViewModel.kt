// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. ViewModel de presentación; coordina acciones del usuario y publica estado observable para la interfaz.
package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.parkeo.data.remote.dto.VehicleDto
import pe.parkeo.data.remote.dto.VehicleTypeDto
import pe.parkeo.domain.repository.VehicleRepository
import pe.parkeo.util.Result

data class VehicleUiState(
    val isLoading: Boolean = false,
    val vehicles: List<VehicleDto> = emptyList(),
    val vehicleTypes: List<VehicleTypeDto> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

class VehicleViewModel(
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleUiState())
    val uiState: StateFlow<VehicleUiState> = _uiState.asStateFlow()

    init {
        loadVehicles()
        loadVehicleTypes()
    }

    /** Carga los datos requeridos: load vehicles. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    fun loadVehicles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = vehicleRepository.getMyVehicles()) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, vehicles = result.data) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    /** Carga los datos requeridos: load vehicle types. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    fun loadVehicleTypes() {
        viewModelScope.launch {
            when (val result = vehicleRepository.getVehicleTypes()) {
                is Result.Success -> _uiState.update { it.copy(vehicleTypes = result.data) }
                else -> {}
            }
        }
    }

    /** Ejecuta la operación indicada por el nombre: add vehicle. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    fun addVehicle(licensePlate: String, brand: String?, model: String?, color: String?, vehicleTypeId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = vehicleRepository.createVehicle(licensePlate, brand, model, color, vehicleTypeId)) {
                is Result.Success -> {
                    loadVehicles()
                    _uiState.update { it.copy(successMessage = "Vehículo agregado") }
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    /** Elimina o desactiva el recurso indicado: delete vehicle. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    fun deleteVehicle(id: Long) {
        viewModelScope.launch {
            when (vehicleRepository.deleteVehicle(id)) {
                is Result.Success -> loadVehicles()
                is Result.Error -> _uiState.update { it.copy(error = "Error al eliminar vehículo") }
                else -> {}
            }
        }
    }

    /** Limpia el estado asociado: clear messages. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
