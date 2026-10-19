/*
 * PARKeo · Android
 * Archivo: ui/viewmodel/AdminViewModel.kt
 * Capa: presentación / estado (ViewModel).
 * Propósito: reunir las métricas, los estacionamientos y los usuarios que necesita el
 * panel administrativo, además de abrir/cerrar cocheras, asignar operadores y activar o
 * desactivar cuentas.
 * Conceptos clave: ViewModel, StateFlow, MutableStateFlow, StateFlow.asStateFlow,
 *   viewModelScope, corrutinas, Result, actualización inmutable con `copy`, carga
 *   secuencial de varias fuentes y refresco diferenciado.
 * Flujo: AdminDashboardScreen, AdminParkingLotsScreen y AdminUsersScreen observan el mismo
 *   ViewModel. `loadDashboardData` consulta estadísticas, cocheras y usuarios en orden;
 *   cada acción actualiza la lista local con el DTO devuelto y publica un mensaje.
 * Relación: ParkeoViewModelFactory lo construye con AppContainer.adminRepository. El rol
 *   ADMIN se decide en la UI y, sobre todo, en el backend.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones 5, 9 y 14).
 */
package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.parkeo.data.remote.dto.AdminStatisticsDto
import pe.parkeo.data.remote.dto.ParkingLotDto
import pe.parkeo.data.remote.dto.UserDto
import pe.parkeo.domain.repository.AdminRepository
import pe.parkeo.util.Result

/**
 * Estado compartido por las pantallas del módulo administrador.
 *
 * @property isLoading carga principal o acción en curso; las pantallas suelen mostrar un
 *   indicador de página completa solo cuando además no hay datos.
 * @property isRefreshing refresco manual iniciado por el usuario (gesto o botón). Se
 *   distingue de `isLoading` para no ocultar el contenido ya cargado mientras se actualiza.
 * @property statistics métricas agregadas del panel. Se inicializa con una instancia vacía
 *   para que la UI pueda leer contadores sin comprobar `null`.
 * @property parkingLots estacionamientos administrables de la red.
 * @property users cuentas registradas (administradores, operadores y clientes).
 * @property error mensaje legible del último fallo; `null` si no hay error pendiente.
 * @property successMessage confirmación de una acción administrativa.
 */
data class AdminUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val statistics: AdminStatisticsDto = AdminStatisticsDto(),
    val parkingLots: List<ParkingLotDto> = emptyList(),
    val users: List<UserDto> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

/**
 * ViewModel del panel de administración.
 *
 * Mantiene una sola fuente de verdad para las tres pantallas administrativas. No aplica
 * reglas de autorización: el backend valida que la cuenta tenga rol ADMIN y que la acción
 * sea legal; ocultar una pantalla no protege un endpoint.
 *
 * @param adminRepository contrato de dominio con las consultas y mutaciones del panel.
 */
class AdminViewModel(
    private val adminRepository: AdminRepository
) : ViewModel() {

    /* Estado mutable privado; las pantallas solo observan la versión de solo lectura. */
    private val _uiState = MutableStateFlow(AdminUiState())

    /** Vista de solo lectura que consumen las pantallas administrativas. */
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    /**
     * Carga (o refresca) los tres bloques del panel: estadísticas, cocheras y usuarios.
     *
     * El trabajo ocurre en una sola corrutina, por lo que las solicitudes son secuenciales.
     * Si `refresh` es `true`, se activa `isRefreshing` para conservar el contenido visible;
     * en caso contrario se activa `isLoading` para el estado inicial. El error de
     * estadísticas es el único que se publica; los fallos de cocheras y usuarios se ignoran
     * a propósito para no ocultar datos que sí pudieron cargarse. Al final se apagan ambas
     * banderas, incluso si hubo un error parcial.
     *
     * @param refresh `true` para un refresco manual que no tape la pantalla; `false` para
     *   la carga inicial.
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
    fun loadDashboardData(refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) {
                _uiState.update { it.copy(isRefreshing = true, error = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, error = null) }
            }

            // Estadísticas: es el bloque principal; su error sí se muestra al usuario.
            when (val statsResult = adminRepository.getStatistics()) {
                is Result.Success -> {
                    _uiState.update { it.copy(statistics = statsResult.data) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(error = statsResult.message) }
                }
                else -> {}
            }

            // Cocheras: un fallo aquí no es fatal si las estadísticas ya se cargaron.
            when (val lotsResult = adminRepository.getParkingLots()) {
                is Result.Success -> {
                    _uiState.update { it.copy(parkingLots = lotsResult.data) }
                }
                is Result.Error -> {
                    // Fallo no fatal: se conserva la lista anterior y no se sobrescribe el error.
                }
                else -> {}
            }

            // Usuarios: se aplica la misma política de fallo no fatal que en cocheras.
            when (val usersResult = adminRepository.getUsers()) {
                is Result.Success -> {
                    _uiState.update { it.copy(users = usersResult.data) }
                }
                is Result.Error -> {
                    // Fallo no fatal: se conserva la lista anterior.
                }
                else -> {}
            }

            // Pase lo que pase con los bloques, se cierran los indicadores de carga.
            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
        }
    }

    /**
     * Abre/cierra o activa/desactiva un estacionamiento.
     *
     * Los dos parámetros son opcionales en el contrato: `null` significa "no cambiar ese
     * aspecto". El repositorio devuelve el DTO actualizado; el ViewModel reemplaza solo el
     * elemento correspondiente dentro de la lista, sin recargar todo el panel. Es una
     * actualización inmutable porque `map` produce una lista nueva y `copy` un estado nuevo.
     *
     * @param id identificador del estacionamiento.
     * @param isOpen nuevo estado de apertura, o `null` para no modificarlo.
     * @param isActive nuevo estado de activación, o `null` para no modificarlo.
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
    fun toggleParkingLotStatus(id: Long, isOpen: Boolean?, isActive: Boolean?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = adminRepository.updateParkingLotStatus(id, isOpen, isActive)) {
                is Result.Success -> {
                    val updatedLots = _uiState.value.parkingLots.map { lot ->
                        if (lot.id == id) result.data else lot
                    }
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            parkingLots = updatedLots,
                            successMessage = "Estado de cochera actualizado"
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
     * Asigna un operador a un estacionamiento.
     *
     * El repositorio devuelve la cochera actualizada; se sustituye en la lista local para
     * que la pantalla refleje el cambio sin una recarga completa. La autorización y la
     * existencia del operador las valida el servidor.
     *
     * @param parkingLotId estacionamiento destino.
     * @param operatorId usuario con rol de operador que se asignará.
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
    fun assignOperator(parkingLotId: Long, operatorId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = adminRepository.assignOperator(parkingLotId, operatorId)) {
                is Result.Success -> {
                    val updatedLots = _uiState.value.parkingLots.map { lot ->
                        if (lot.id == parkingLotId) result.data else lot
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            parkingLots = updatedLots,
                            successMessage = "Operador asignado exitosamente"
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
     * Activa o desactiva una cuenta de usuario.
     *
     * El estado objetivo se calcula como negación del valor actual que envió la pantalla;
     * el backend devuelve el usuario actualizado y se reemplaza solo ese elemento. El
     * mensaje de éxito se elige según el estado objetivo. Desactivar una cuenta no debe
     * confundirse con borrarla: es una baja lógica que el backend decide cómo aplicar.
     *
     * @param id identificador del usuario.
     * @param currentActive estado actual mostrado por la UI, usado para invertirlo.
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
    fun toggleUserStatus(id: Long, currentActive: Boolean) {
        viewModelScope.launch {
            val targetActive = !currentActive
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = adminRepository.updateUserStatus(id, targetActive)) {
                is Result.Success -> {
                    val updatedUsers = _uiState.value.users.map { user ->
                        if (user.id == id) result.data else user
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            users = updatedUsers,
                            successMessage = if (targetActive) "Usuario activado" else "Usuario desactivado"
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
     * Limpia los mensajes de error y éxito después de que la pantalla los presenta. Las
     * listas y estadísticas se conservan.
     *
     * @return no devuelve un valor.
     */
    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
