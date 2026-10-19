/*
 * PARKeo · Android
 * Archivo: ui/viewmodel/OperatorViewModel.kt
 * Capa: presentación / estado (ViewModel).
 * Propósito: dar al operador la lista de sus cocheras, los espacios y las reservas del
 * estacionamiento elegido, además de permitir cambiar el estado de un espacio y registrar
 * ingresos y salidas.
 * Conceptos clave: ViewModel, StateFlow, MutableStateFlow, StateFlow.asStateFlow,
 *   viewModelScope, corrutinas, Result, actualización inmutable con `copy` y estado de
 *   selección compartido entre pantallas.
 * Flujo: OperatorSpacesScreen y OperatorReservationsScreen observan el mismo ViewModel.
 *   Al cargar las cocheras se elige una y se consultan espacios y reservas; las acciones
 *   actualizan la lista local y, en check-in/check-out, se recargan los espacios.
 * Relación: ParkeoViewModelFactory lo construye con AppContainer.operatorRepository. El
 *   backend valida que el operador tenga asignada la cochera.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones 5, 9 y 14).
 */
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

/**
 * Estado compartido por las pantallas del operador.
 *
 * @property isLoading carga principal o acción en curso; si ya hay datos, la pantalla puede
 *   mantenerlos visibles y mostrar solo un indicador pequeño.
 * @property isRefreshing refresco manual que no debe tapar el contenido existente.
 * @property parkingLots cocheras asignadas al operador autenticado.
 * @property selectedLot cochera actualmente elegida; `null` mientras no haya ninguna.
 * @property spaces espacios de la cochera seleccionada, con su estado de ocupación.
 * @property reservations reservas de la cochera seleccionada.
 * @property searchQuery texto del filtro local de espacios; el filtrado ocurre en la UI.
 * @property error mensaje legible del último fallo; `null` si no hay error pendiente.
 * @property successMessage confirmación de una acción sobre espacios o reservas.
 */
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

/**
 * ViewModel del módulo operador.
 *
 * Mantiene la selección de cochera y los datos derivados (espacios y reservas) en un solo
 * estado, de modo que ambas pantallas del módulo compartan la misma visión. El servidor
 * sigue siendo la autoridad sobre qué cocheras puede ver y modificar el operador.
 *
 * @param operatorRepository contrato de dominio con las consultas y acciones del operador.
 */
class OperatorViewModel(
    private val operatorRepository: OperatorRepository
) : ViewModel() {

    /* Estado mutable privado; la UI recibe una vista inmutable. */
    private val _uiState = MutableStateFlow(OperatorUiState())

    /** Vista de solo lectura compartida por las pantallas del operador. */
    val uiState: StateFlow<OperatorUiState> = _uiState.asStateFlow()

    /**
     * Carga las cocheras asignadas al operador y selecciona una.
     *
     * Si `refresh` es `true`, usa `isRefreshing` para no ocultar el contenido; en caso
     * contrario usa `isLoading`. La selección se conserva si la cochera sigue existiendo en
     * la nueva lista; si desapareció, se elige la primera. Después de publicar la lista se
     * llama a `loadSpacesAndReservations` para traer los datos de la cochera elegida.
     *
     * Nota: OperatorReservationsScreen también observa `selectedLot` y puede solicitar la
     * misma carga; ese comportamiento duplicado pertenece a la pantalla y no se corrige
     * aquí para no alterar la lógica existente.
     *
     * @param refresh `true` para refresco manual; `false` para carga inicial.
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
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
                    /* Se conserva la cochera elegida si sigue disponible; si no, la primera. */
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

                    /* Solo se consultan espacios/reservas si realmente hay una cochera. */
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

    /**
     * Cambia la cochera activa y solicita sus espacios y reservas.
     *
     * La actualización de `selectedLot` es inmediata e inmutable; la consulta se ejecuta en
     * la corrutina que abre `loadSpacesAndReservations`. La pantalla puede reaccionar a la
     * nueva selección para actualizar títulos y resúmenes.
     *
     * @param lot cochera elegida por el operador.
     * @return no devuelve un valor.
     */
    fun selectParkingLot(lot: ParkingLotDto) {
        _uiState.update { it.copy(selectedLot = lot) }
        loadSpacesAndReservations(lot.id)
    }

    /**
     * Consulta en secuencia los espacios y las reservas de una cochera.
     *
     * Activa `isLoading` y limpia el error. Un fallo al cargar espacios se publica en
     * `error`; un fallo al cargar reservas se ignora de forma deliberada, de modo que un
     * problema en una de las dos listas no oculta la otra. Al final se apaga `isLoading`.
     * Las dos llamadas son suspendidas y comparten la misma corrutina.
     *
     * @param lotId identificador de la cochera cuyos datos se quieren cargar.
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
    fun loadSpacesAndReservations(lotId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Espacios: su error sí se publica porque condiciona la operación del operador.
            when (val spacesResult = operatorRepository.getSpaces(lotId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(spaces = spacesResult.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = spacesResult.message) }
                }
                else -> {}
            }

            // Reservas: fallo no fatal; se conserva la lista anterior.
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

    /**
     * Cambia el estado de un espacio (por ejemplo `AVAILABLE`, `OCCUPIED` o `RESERVED`).
     *
     * El repositorio devuelve el espacio actualizado; se reemplaza solo ese elemento en la
     * lista mediante `map` y `copy`, sin recargar toda la cochera. El mensaje de éxito
     * incluye el estado nuevo. Un fallo apaga la carga y publica el mensaje del backend.
     *
     * @param spaceId identificador del espacio.
     * @param newStatus estado destino que espera el backend.
     * @param notes observación opcional asociada al cambio.
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
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

    /**
     * Registra el ingreso de una reserva (check-in).
     *
     * Actualiza la reserva devuelta dentro de la lista y publica un mensaje. Después vuelve
     * a cargar espacios y reservas de la cochera seleccionada, porque el ingreso cambia la
     * ocupación y el estado real del espacio. La recarga se ejecuta en otra corrutina
     * lanzada por `loadSpacesAndReservations`.
     *
     * @param reservationId reserva que llega al estacionamiento.
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
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

    /**
     * Registra la salida de una reserva (check-out).
     *
     * Sigue el mismo patrón que `checkIn`: reemplaza la reserva devuelta, publica el mensaje
     * y vuelve a consultar espacios y reservas para reflejar el espacio liberado y el nuevo
     * estado de la reserva.
     *
     * @param reservationId reserva que abandona el estacionamiento.
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
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

    /**
     * Actualiza el texto de búsqueda local de espacios. No realiza una petición: la
     * pantalla filtra la lista ya cargada usando `searchQuery`.
     *
     * @param query texto ingresado por el operador.
     * @return no devuelve un valor.
     */
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    /**
     * Limpia los mensajes de error y éxito después de que la pantalla los presenta. No
     * modifica cocheras, espacios ni reservas.
     *
     * @return no devuelve un valor.
     */
    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
