package pe.parkeo.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Eventos globales de sesión. Se usa desde el OkHttp Authenticator (fuera del
 * ciclo de Compose) para avisarle a la UI que la sesión expiró y navegar al login.
 */
object SessionEvents {

    data class SessionExpired(val id: Long, val message: String)

    private val _events = MutableStateFlow<SessionExpired?>(null)
    val events: StateFlow<SessionExpired?> = _events.asStateFlow()

    private var counter = 0L

    fun notifySessionExpired(message: String) {
        counter += 1
        _events.value = SessionExpired(counter, message)
    }

    fun consume() {
        _events.value = null
    }
}
