/*
 * PARKeo · Android
 * Archivo: ui/viewmodel/AuthViewModel.kt
 * Capa: presentación / estado (ViewModel).
 * Propósito: coordinar el inicio de sesión, el registro, el cierre de sesión y la
 * exposición del perfil guardado, sin que las pantallas conozcan Retrofit ni DataStore.
 * Conceptos clave: ViewModel, StateFlow, MutableStateFlow, StateFlow.asStateFlow,
 *   viewModelScope, corrutinas, Result, StateFlow.stateIn y SharingStarted.WhileSubscribed.
 * Flujo: LoginScreen o RegisterScreen invocan login/register; el ViewModel lanza una
 *   corrutina, llama a AuthRepository y publica carga, error o éxito. SessionDataStore
 *   conserva la sesión y emite isLoggedIn/userProfile, que ProfileScreen y la navegación
 *   observan. Logout limpia la sesión y el estado publicado.
 * Relación: ParkeoViewModelFactory lo construye con AppContainer.authRepository y
 *   AppContainer.sessionDataStore. Las pantallas LoginScreen, RegisterScreen y
 *   ProfileScreen lo consumen; ParkeoNavGraph decide la ruta según el éxito.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones 5, 9 y 14).
 */
package pe.parkeo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.parkeo.data.local.SessionDataStore
import pe.parkeo.domain.repository.AuthRepository
import pe.parkeo.util.Result

/**
 * Estado efímero de las pantallas de autenticación.
 *
 * Distingue dos ideas que suelen confundirse: `isLoggedIn` refleja el resultado de la
 * última acción de esta pantalla, mientras que la sesión persistida se observa aparte con
 * `AuthViewModel.isLoggedIn`. Los campos de error y éxito son eventos de un solo uso: la
 * pantalla los consume y luego llama a `clearError` o `resetSuccess`.
 *
 * @property isLoading `true` mientras hay una petición de login o registro en curso; sirve
 *   para deshabilitar botones y mostrar progreso.
 * @property isLoggedIn resultado de la acción actual: pasa a `true` tras autenticar. No es
 *   la fuente de verdad persistente, que vive en `SessionDataStore`.
 * @property error mensaje legible para el usuario cuando la operación falla; `null` cuando
 *   no hay error pendiente. No contiene trazas técnicas.
 * @property isSuccess bandera de éxito usada por la pantalla para navegar una sola vez;
 *   debe restablecerse con `resetSuccess` después de navegar.
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

/**
 * Proyección de los datos mínimos del usuario guardados en DataStore.
 *
 * No es el DTO completo de autenticación ni una entidad de base de datos: solo reúne lo que
 * la interfaz necesita para saludar y decidir qué opciones mostrar.
 *
 * @property email correo del usuario autenticado; cadena vacía si aún no se conoce.
 * @property name nombre visible. El ViewModel sustituye los valores en blanco por
 *   "Usuario Parkeo" para evitar encabezados vacíos.
 * @property role rol textual informado por el backend (por ejemplo `CLIENTE`, `ADMIN` u
 *   `OPERADOR`). La interfaz lo compara en mayúsculas para decidir accesos visuales; la
 *   autorización real siempre la aplica el servidor.
 */
data class UserProfile(
    val email: String = "",
    val name: String = "",
    val role: String = "CLIENTE"
)

/**
 * ViewModel compartido por las pantallas de autenticación y perfil.
 *
 * Conserva un `MutableStateFlow` privado para los eventos de autenticación y expone su
 * versión de solo lectura. Además transforma los flujos de `SessionDataStore` en estado
 * observable con `stateIn`, de modo que la UI no tenga que leer DataStore directamente.
 *
 * @param authRepository contrato de dominio que ejecuta login, registro y logout contra el
 *   backend. Se recibe como interfaz para poder sustituirlo en pruebas.
 * @param sessionDataStore almacenamiento local de la sesión (tokens, correo, nombre y rol).
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    /*
     * Fuente mutable del estado de la pantalla. El guion bajo es una convención de Kotlin:
     * solo el ViewModel modifica este flujo; las pantallas reciben la vista inmutable.
     */
    private val _uiState = MutableStateFlow(AuthUiState())

    /** Vista de solo lectura del estado de autenticación que observan las pantallas. */
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /*
     * `SessionDataStore.isLoggedIn` es un Flow que depende del token guardado. `stateIn`
     * lo convierte en StateFlow con un valor inicial (`false`) para que Compose siempre
     * tenga algo que mostrar. `WhileSubscribed(5000)` mantiene la lectura activa durante
     * cinco segundos después de que la última pantalla deja de observar, evitando cancelar
     * y recrear el flujo ante cambios rápidos de configuración.
     */
    val isLoggedIn: StateFlow<Boolean> = sessionDataStore.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /*
     * DataStore entrega un Triple(email, name, role) o null. `map` lo transforma en el
     * modelo de presentación `UserProfile`; si el nombre está en blanco se usa un texto
     * amigable. El valor inicial `null` representa "todavía no se leyó la sesión" y permite
     * que la pantalla muestre un estado neutro mientras llega la primera emisión.
     */
    val userProfile: StateFlow<UserProfile?> = sessionDataStore.getUserInfo()
        .map { triple ->
            triple?.let { (email, name, role) ->
                UserProfile(
                    email = email,
                    name = name.ifBlank { "Usuario Parkeo" },
                    role = role
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Autentica al usuario con correo y contraseña.
     *
     * La pantalla valida el formato del correo y que los campos no estén vacíos antes de
     * llamar aquí; este ViewModel no repite esas reglas de formulario. La operación se
     * ejecuta en `viewModelScope`, por lo que se cancela automáticamente si el ViewModel
     * deja de existir. Primero publica `isLoading = true` y limpia el error anterior; luego
     * traduce `Result.Success` a éxito y `Result.Error` a un mensaje visible. La rama
     * `else` cubre `Result.Loading`, que los repositorios de este proyecto no emiten, pero
     * mantiene el `when` exhaustivo.
     *
     * @param email correo ya recortado por la pantalla.
     * @param password contraseña en texto plano solo durante la petición; el backend la
     *   compara con su hash y no se persiste en la app.
     * @return no devuelve un valor; el resultado se observa en `uiState`.
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.login(email, password)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, isLoggedIn = true, isSuccess = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    /**
     * Registra una cuenta nueva y, si el backend responde con sesión, deja al usuario
     * autenticado. El repositorio es quien guarda los tokens en `SessionDataStore`; este
     * ViewModel solo refleja el resultado en la pantalla.
     *
     * Se aplica el mismo patrón que en `login`: estado de carga, limpieza del error previo,
     * actualización inmutable con `copy` y traducción de `Result.Success`/`Result.Error`.
     *
     * @param email correo de la cuenta nueva.
     * @param password contraseña elegida por el usuario.
     * @param firstName nombres del usuario.
     * @param lastName apellidos del usuario.
     * @param phone teléfono opcional; puede ser `null` si el formulario no lo exige.
     * @return no devuelve un valor; el resultado se publica en `uiState`.
     */
    fun register(email: String, password: String, firstName: String, lastName: String, phone: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.register(email, password, firstName, lastName, phone)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, isLoggedIn = true, isSuccess = true) }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    /**
     * Cierra la sesión local y remota.
     *
     * `AuthRepository.logout` intenta avisar al backend y, en cualquier caso, limpia la
     * sesión local; por eso el ViewModel no espera un `Result`. Al terminar, apaga las
     * banderas de la pantalla para que un nuevo inicio de sesión empiece limpio. La
     * navegación posterior la decide `ProfileScreen`/`ParkeoNavGraph`.
     *
     * @return no devuelve un valor.
     */
    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(isLoggedIn = false, isSuccess = false) }
        }
    }

    /**
     * Elimina el mensaje de error pendiente para que no se vuelva a mostrar al recomponer
     * la pantalla. Se usa después de presentar un aviso o al corregir el formulario.
     *
     * @return no devuelve un valor.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Consume la bandera de éxito. La pantalla navega cuando `isSuccess` es `true` y luego
     * llama aquí; así evita navegar repetidamente si Compose vuelve a observar el mismo
     * estado. No toca la sesión persistida.
     *
     * @return no devuelve un valor.
     */
    fun resetSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }
}
