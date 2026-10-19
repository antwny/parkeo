/*
 * PARKeo · Android
 * Archivo: ui/viewmodel/HomeViewModel.kt
 * Capa: presentación / estado (ViewModel).
 * Propósito: cargar el catálogo de estacionamientos, buscar por texto y consultar los
 * cercanos a una ubicación, publicando el resultado para el mapa y la lista del inicio.
 * Conceptos clave: ViewModel, StateFlow, MutableStateFlow, StateFlow.asStateFlow,
 *   viewModelScope, corrutinas, Result y actualización inmutable con `copy`.
 * Flujo: HomeScreen observa `uiState`; al crear el ViewModel se cargan todos los
 *   estacionamientos. La búsqueda y la ubicación del usuario vuelven a consultar
 *   ParkingRepository y reemplazan la lista. Los errores se muestran como texto.
 * Relación: ParkeoViewModelFactory lo construye con AppContainer.parkingRepository.
 *   Lo consume HomeScreen dentro de MainScreen y ParkeoNavGraph.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones 5, 6 y 14).
 */
package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.parkeo.data.remote.dto.ParkingLotDto
import pe.parkeo.domain.repository.ParkingRepository
import pe.parkeo.util.Result

/**
 * Estado de la pantalla de inicio.
 *
 * La lista es la misma sin importar si la fuente fue "todos", "cercanos" o "búsqueda";
 * por eso no se guarda un modo separado: la pantalla decide cómo representarla (mapa o
 * lista) con su propio estado de UI.
 *
 * @property isLoading indica que hay una consulta de estacionamientos en curso.
 * @property parkingLots estacionamientos visibles en el mapa/lista. Una lista vacía es un
 *   estado válido: puede significar que no hay resultados o que aún no llegó la respuesta.
 * @property error mensaje legible del último fallo; `null` si no hay error pendiente.
 * @property searchQuery último texto de búsqueda aplicado. Se conserva aquí para que el
 *   ViewModel pueda limpiar la búsqueda y volver a cargar todo.
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val parkingLots: List<ParkingLotDto> = emptyList(),
    val error: String? = null,
    val searchQuery: String = ""
)

/**
 * ViewModel del inicio.
 *
 * Mantiene un único `MutableStateFlow` y lo publica como `StateFlow` de solo lectura.
 * No guarda referencias a la pantalla ni al contexto de Android; toda la entrada/salida
 * pasa por `ParkingRepository`.
 *
 * @param parkingRepository contrato de dominio para listar, buscar y consultar
 *   estacionamientos cercanos.
 */
class HomeViewModel(
    private val parkingRepository: ParkingRepository
) : ViewModel() {

    /* Estado mutable privado: solo este ViewModel puede cambiarlo. */
    private val _uiState = MutableStateFlow(HomeUiState())

    /** Estado observable por HomeScreen; expone una vista inmutable del flujo anterior. */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /*
     * Bloque de inicialización: al crearse el ViewModel se dispara la carga inicial. La
     * llamada no bloquea el hilo principal porque `loadParkingLots` abre una corrutina en
     * `viewModelScope`; si la pantalla se recrea, Android reutiliza el ViewModel y no se
     * repite esta carga hasta que el propietario se destruya de verdad.
     */
    init {
        loadParkingLots()
    }

    /**
     * Recupera todos los estacionamientos publicados por el backend.
     *
     * Marca `isLoading` y limpia el error anterior antes de llamar al repositorio. Con
     * `Result.Success` reemplaza la lista; con `Result.Error` conserva la lista anterior y
     * publica el mensaje. Cada invocación lanza su propia corrutina: si se hacen varias
     * llamadas seguidas, la última respuesta que termine será la que actualice el estado,
     * sin cancelación manual de peticiones anteriores en este ViewModel.
     *
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
    fun loadParkingLots() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = parkingRepository.getParkingLots()) {
                /* Actualización inmutable: se crea un estado nuevo, no se muta la lista. */
                is Result.Success -> _uiState.update { it.copy(isLoading = false, parkingLots = result.data) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                /* `Result.Loading` no lo emiten los repositorios, pero el when debe ser exhaustivo. */
                else -> {}
            }
        }
    }

    /**
     * Consulta estacionamientos cercanos a una coordenada y reemplaza la lista actual.
     *
     * La pantalla obtiene la latitud/longitud con el permiso de ubicación y llama aquí.
     * Si la consulta falla, no se muestra directamente ese error: se intenta recuperar el
     * catálogo completo con `loadParkingLots`, de modo que el usuario no quede con una
     * pantalla vacía por un problema del filtro geográfico. Esa segunda llamada también
     * restablece `isLoading` y el error.
     *
     * @param lat latitud del usuario en grados decimales.
     * @param lon longitud del usuario en grados decimales.
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
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

    /**
     * Aplica una búsqueda por nombre o distrito.
     *
     * Guarda el texto en `searchQuery` para que el estado refleje la consulta vigente. Si
     * el texto está en blanco, restablece el catálogo completo y termina. En caso contrario
     * lanza la consulta al repositorio; no activa `isLoading` ni limpia el error previo, por
     * lo que la lista anterior permanece visible mientras llega la respuesta. Un fallo se
     * publica en `error` sin borrar los resultados que ya se mostraban.
     *
     * @param query texto escrito por el usuario; se envía tal como llega.
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            loadParkingLots()
            return
        }
        viewModelScope.launch {
            when (val result = parkingRepository.searchParkingLots(query)) {
                is Result.Success -> _uiState.update { it.copy(parkingLots = result.data) }
                is Result.Error -> _uiState.update { it.copy(error = result.message) }
                else -> {}
            }
        }
    }
}
