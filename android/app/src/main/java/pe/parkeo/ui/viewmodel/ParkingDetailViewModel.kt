/*
 * PARKeo · Android
 * Archivo: ui/viewmodel/ParkingDetailViewModel.kt
 * Capa: presentación / estado (ViewModel).
 * Propósito: cargar la ficha completa de un estacionamiento (datos, tarifas,
 * disponibilidad, coordenadas) para la pantalla de detalle y el formulario de reserva.
 * Conceptos clave: ViewModel, StateFlow, MutableStateFlow, StateFlow.asStateFlow,
 *   viewModelScope, corrutinas, Result y actualización inmutable con `copy`.
 * Flujo: ParkingDetailScreen y ReservationCreateScreen reciben el id de la navegación,
 *   llaman a `loadParkingLot(id)` y observan `uiState`. El repositorio consulta el backend
 *   y el ViewModel publica carga, detalle o error.
 * Relación: ParkeoViewModelFactory lo construye con AppContainer.parkingRepository.
 *   Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones 5 y 6).
 */
package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.parkeo.data.remote.dto.ParkingLotDetailDto
import pe.parkeo.domain.repository.ParkingRepository
import pe.parkeo.util.Result

/**
 * Estado de la pantalla de detalle de un estacionamiento.
 *
 * @property isLoading `true` mientras se consulta la ficha; la pantalla puede mostrar un
 *   indicador sin borrar el detalle anterior.
 * @property parkingLot ficha completa recibida del backend o `null` si todavía no se cargó
 *   o si la consulta falló.
 * @property error mensaje legible del último fallo; `null` si no hay error pendiente.
 */
data class ParkingDetailUiState(
    val isLoading: Boolean = false,
    val parkingLot: ParkingLotDetailDto? = null,
    val error: String? = null
)

/**
 * ViewModel de la ficha de un estacionamiento.
 *
 * No conoce la navegación ni el id de la ruta: la pantalla se lo entrega a
 * `loadParkingLot`. Conserva el último detalle cargado para que una recomposición no
 * obligue a repetir la petición.
 *
 * @param parkingRepository contrato de dominio que obtiene el detalle por identificador.
 */
class ParkingDetailViewModel(
    private val parkingRepository: ParkingRepository
) : ViewModel() {

    /* Estado mutable privado; las pantallas nunca lo modifican directamente. */
    private val _uiState = MutableStateFlow(ParkingDetailUiState())

    /** Vista de solo lectura del estado que observan las pantallas de detalle y reserva. */
    val uiState: StateFlow<ParkingDetailUiState> = _uiState.asStateFlow()

    /**
     * Consulta y publica la ficha del estacionamiento indicado.
     *
     * Antes de la llamada suspendida activa `isLoading` y limpia el error previo. Un
     * `Result.Success` reemplaza el detalle; un `Result.Error` conserva el detalle anterior
     * (si existía) y publica el mensaje. La corrutina vive en `viewModelScope`, así que se
     * cancela si el ViewModel desaparece, por ejemplo al salir de la pantalla.
     *
     * @param id identificador del estacionamiento recibido como argumento de navegación.
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
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
