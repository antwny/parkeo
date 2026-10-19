/*
 * PARKeo · Android
 * Archivo: ui/viewmodel/VehicleViewModel.kt
 * Capa: presentación / estado (ViewModel).
 * Propósito: gestionar la lista de vehículos del usuario, los tipos de vehículo
 * disponibles y las operaciones de alta y baja.
 * Conceptos clave: ViewModel, StateFlow, MutableStateFlow, StateFlow.asStateFlow,
 *   viewModelScope, corrutinas, Result, actualización inmutable con `copy` y cargas
 *   paralelas lanzadas desde `init`.
 * Flujo: VehiclesScreen y ReservationCreateScreen observan `uiState`. Al crear el
 *   ViewModel se cargan vehículos y tipos; agregar un vehículo vuelve a cargar la lista;
 *   eliminar también la refresca. Los mensajes se consumen y luego se limpian.
 * Relación: ParkeoViewModelFactory lo construye con AppContainer.vehicleRepository.
 *   Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones 5 y 6).
 */
package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.parkeo.data.remote.dto.VehicleDto
import pe.parkeo.data.remote.dto.VehicleTypeDto
import pe.parkeo.domain.repository.VehicleRepository
import pe.parkeo.util.Result

/**
 * Estado de las pantallas que muestran o eligen vehículos.
 *
 * @property isLoading `true` durante la carga de vehículos o durante un alta/baja; no se
 *   activa al cargar los tipos, que se consideran datos auxiliares.
 * @property vehicles vehículos del usuario. Una lista vacía es válida y la pantalla suele
 *   ofrecer registrar el primero.
 * @property vehicleTypes catálogo de tipos (auto, moto, camioneta, etc.) usado por el
 *   formulario de alta y por la reserva.
 * @property error mensaje legible del último fallo; `null` si no hay error pendiente.
 * @property successMessage confirmación de una operación, por ejemplo "Vehículo agregado".
 */
data class VehicleUiState(
    val isLoading: Boolean = false,
    val vehicles: List<VehicleDto> = emptyList(),
    val vehicleTypes: List<VehicleTypeDto> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

/**
 * ViewModel de vehículos.
 *
 * Encapsula las consultas y mutaciones del repositorio y mantiene el estado observable.
 * La validación de placa, marca o tipo pertenece al formulario; el backend vuelve a
 * validar antes de persistir.
 *
 * @param vehicleRepository contrato de dominio para listar, crear y eliminar vehículos y
 *   consultar sus tipos.
 */
class VehicleViewModel(
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    /* Estado mutable privado; la UI solo recibe la versión de solo lectura. */
    private val _uiState = MutableStateFlow(VehicleUiState())

    /** Vista de solo lectura que observan las pantallas. */
    val uiState: StateFlow<VehicleUiState> = _uiState.asStateFlow()

    /*
     * Carga inicial. Se lanzan dos corrutinas independientes: una para vehículos y otra
     * para tipos. `loadVehicles` activa `isLoading`, pero `loadVehicleTypes` no, por lo que
     * el indicador refleja únicamente la carga principal. Si ambas terminan en distinto
     * orden, cada una actualiza solo su propia lista mediante `copy`.
     */
    init {
        loadVehicles()
        loadVehicleTypes()
    }

    /**
     * Recarga los vehículos del usuario.
     *
     * Activa `isLoading`, llama al repositorio y, según el `Result`, reemplaza la lista o
     * publica el error. No limpia un error anterior al empezar; el error se sustituye solo
     * si la nueva llamada falla. Se usa tanto al iniciar como después de crear o eliminar.
     *
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
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

    /**
     * Recarga el catálogo de tipos de vehículo.
     *
     * Es una carga auxiliar: no toca `isLoading` ni borra el error existente. Si falla, el
     * `Result.Error` se ignora de forma deliberada para no bloquear la pantalla de vehículos;
     * el formulario simplemente se quedará sin opciones hasta que el catálogo esté
     * disponible. Un `Result.Success` actualiza la lista de tipos.
     *
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
    fun loadVehicleTypes() {
        viewModelScope.launch {
            when (val result = vehicleRepository.getVehicleTypes()) {
                is Result.Success -> _uiState.update { it.copy(vehicleTypes = result.data) }
                else -> {}
            }
        }
    }

    /**
     * Registra un vehículo para el usuario actual.
     *
     * Activa `isLoading` y delega en el repositorio. Si el alta es exitosa, recarga la lista
     * con `loadVehicles` (que volverá a marcar carga y luego la apagará) y publica un
     * mensaje de éxito. Si falla, apaga la carga y publica el mensaje del backend. Los
     * campos opcionales se envían tal cual; la placa y el tipo son obligatorios por firma.
     *
     * @param licensePlate placa del vehículo.
     * @param brand marca opcional.
     * @param model modelo opcional.
     * @param color color opcional.
     * @param vehicleTypeId identificador del tipo de vehículo elegido.
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
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

    /**
     * Elimina o desactiva el vehículo indicado y refresca la lista.
     *
     * No activa `isLoading`, por lo que la pantalla debe manejar su propia confirmación o
     * bloqueo. Si el repositorio falla, se publica un mensaje genérico en lugar del mensaje
     * del backend; si tiene éxito, la recarga de `loadVehicles` es la que actualiza la lista.
     *
     * @param id identificador del vehículo a eliminar.
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
    fun deleteVehicle(id: Long) {
        viewModelScope.launch {
            when (vehicleRepository.deleteVehicle(id)) {
                is Result.Success -> loadVehicles()
                is Result.Error -> _uiState.update { it.copy(error = "Error al eliminar vehículo") }
                else -> {}
            }
        }
    }

    /**
     * Limpia el error y el mensaje de éxito pendientes para que la pantalla no los repita.
     * No altera las listas de vehículos ni de tipos.
     *
     * @return no devuelve un valor.
     */
    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
