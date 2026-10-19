/*
 * PARKeo · Android
 * Archivo: ui/viewmodel/ReservationViewModel.kt
 * Capa: presentación / estado (ViewModel).
 * Propósito: listar las reservas del usuario por estado, crear una reserva y cancelarla,
 * dejando a la pantalla los mensajes y el identificador necesario para apagar recordatorios.
 * Conceptos clave: ViewModel, StateFlow, MutableStateFlow, StateFlow.asStateFlow,
 *   viewModelScope, corrutinas, Result, actualización inmutable con `copy` y estado
 *   privado no observable para recordar el último filtro.
 * Flujo: ReservationsScreen carga pestañas con `loadReservations`; ReservationCreateScreen
 *   llama a `createReservation` y, tras el éxito, programa recordatorios. Al cancelar, el
 *   ViewModel publica `cancelledReservationId`, la pantalla llama a ReminderScheduler y
 *   luego limpia los mensajes.
 * Relación: ParkeoViewModelFactory lo construye con AppContainer.reservationRepository.
 *   Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones 5, 11 y 14).
 */
package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.parkeo.data.remote.dto.ReservationDto
import pe.parkeo.domain.repository.ReservationRepository
import pe.parkeo.util.Result

/**
 * Estado de las pantallas de reservas.
 *
 * Convive con dos tipos de datos: una lista para las pestañas y una reserva "actual" para
 * el resultado de una creación. Los campos de mensaje y las banderas son eventos de un solo
 * uso que la pantalla consume y limpia.
 *
 * @property isLoading `true` mientras se listan, crean o cancelan reservas.
 * @property reservations reservas del filtro solicitado, tal como las devuelve el backend.
 * @property currentReservation reserva recién creada (o `null` si aún no hay ninguna); la
 *   pantalla la usa para programar recordatorios con sus horas de inicio y fin.
 * @property error mensaje legible del último fallo; `null` si no hay error pendiente.
 * @property successMessage mensaje de confirmación, por ejemplo "Reserva cancelada".
 * @property reservationCreated bandera de éxito de creación. La pantalla la observa para
 *   mostrar el diálogo y programar recordatorios; debe limpiarse después.
 * @property cancelledReservationId identificador de la última reserva cancelada con éxito.
 *   La pantalla lo usa para cancelar sus recordatorios en WorkManager.
 */
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

/**
 * ViewModel de reservas del cliente.
 *
 * Mantiene el estado observable y un campo privado `lastStatus` que no se expone a la UI:
 * sirve para recargar la misma pestaña después de cancelar, sin obligar a la pantalla a
 * repetir el filtro.
 *
 * @param reservationRepository contrato de dominio para listar, crear y cancelar reservas.
 */
class ReservationViewModel(
    private val reservationRepository: ReservationRepository
) : ViewModel() {

    /* Estado mutable privado: solo este ViewModel lo actualiza con `copy`. */
    private val _uiState = MutableStateFlow(ReservationUiState())

    /** Vista de solo lectura que observan ReservationsScreen y ReservationCreateScreen. */
    val uiState: StateFlow<ReservationUiState> = _uiState.asStateFlow()

    // Última pestaña consultada, para recargar la misma después de cancelar
    private var lastStatus: String? = null

    /**
     * Carga las reservas del usuario para un filtro de estado.
     *
     * Guarda el filtro en `lastStatus` antes de lanzar la corrutina, para poder repetir la
     * misma consulta tras una cancelación. El backend acepta valores como
     * `"PENDING,CONFIRMED"` o `null` para todas. Activa `isLoading`, limpia el error y
     * traduce `Result.Success`/`Result.Error` a la lista o al mensaje visible.
     *
     * @param status filtro de estados separado por comas, o `null` para no filtrar.
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
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
     * Crea una reserva y publica la reserva resultante.
     *
     * Reglas relevantes del contrato:
     * - `parkingSpaceId = null` significa que el servidor asigne un espacio libre del
     *   estacionamiento.
     * - `startTime` y `endTime` viajan en formato ISO `yyyy-MM-dd'T'HH:mm:ss` en hora de
     *   Lima; la pantalla es quien construye esos textos.
     *
     * El ViewModel no valida localmente duración, solapamientos ni disponibilidad: esas
     * reglas las vuelve a aplicar el backend dentro de una transacción. La validación local
     * del formulario pertenece a ReservationCreateScreen. Si el repositorio devuelve
     * `Result.Success`, guarda la reserva en `currentReservation` y activa
     * `reservationCreated`; si devuelve `Result.Error`, publica el mensaje y no activa la
     * bandera.
     *
     * @param parkingLotId estacionamiento donde se reserva.
     * @param parkingSpaceId espacio concreto o `null` para asignación automática.
     * @param vehicleId vehículo del usuario que ocupará el espacio.
     * @param startTime inicio en ISO local de Lima.
     * @param endTime fin en ISO local de Lima.
     * @param notes observación opcional para el operador.
     * @return no devuelve un valor; el resultado se observa en `uiState`.
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

    /**
     * Cancela una reserva y prepara la recarga de la lista actual.
     *
     * No activa `isLoading`: la pantalla ya muestra su propio diálogo de confirmación y
     * bloquea la acción. Si el repositorio devuelve `Result.Success`, publica un mensaje y
     * el id cancelado; la pantalla reacciona apagando los recordatorios y después llama a
     * `clearMessages`. Luego se recarga `lastStatus` para reflejar el nuevo estado. Si falla,
     * solo se publica el error y `cancelledReservationId` permanece sin cambios.
     *
     * @param id identificador de la reserva a cancelar.
     * @param reason motivo opcional enviado al backend.
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
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

    /**
     * Limpia todos los eventos de un solo uso del estado: error, mensaje de éxito, bandera
     * de creación e id de cancelación. Se llama después de que la pantalla consume cada
     * evento para evitar diálogos o avisos repetidos. No modifica la lista de reservas ni
     * la reserva actual.
     *
     * @return no devuelve un valor.
     */
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
