package pe.parkeo.data.remote.interceptor

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import pe.parkeo.data.local.SessionDataStore
import pe.parkeo.data.remote.api.AuthApi
import pe.parkeo.data.remote.dto.RefreshTokenRequestDto
import pe.parkeo.util.SessionEvents

/**
 * Refresca el access token automáticamente ante un 401, con:
 *  - single-flight (un solo refresh concurrente gracias al lock),
 *  - detección de token desactualizado (otro thread ya refrescó),
 *  - máx. 1 reintento por request (header X-Auth-Retried),
 *  - cierre de sesión cuando el refresh token también está vencido.
 */
class TokenAuthenticator(
    private val sessionDataStore: SessionDataStore,
    private val authApi: AuthApi
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header(RETRY_HEADER) != null || response.code != 401) {
            return null
        }

        return synchronized(lock) {
            val currentToken = runBlocking { sessionDataStore.getAccessToken() }
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            // Otro thread ya refrescó el token: reintentar con el nuevo sin volver a llamar a refresh
            if (!currentToken.isNullOrBlank() && currentToken != requestToken) {
                return@synchronized newRequestWithToken(response.request, currentToken)
            }

            val refreshToken = runBlocking { sessionDataStore.getRefreshToken() }
            if (refreshToken.isNullOrBlank()) {
                return@synchronized null
            }

            try {
                val refreshResponse = runBlocking {
                    authApi.refresh(RefreshTokenRequestDto(refreshToken))
                }
                val auth = refreshResponse.takeIf { it.isSuccessful && it.body()?.success == true }?.body()?.data

                if (auth == null) {
                    expireSession()
                    return@synchronized null
                }

                runBlocking {
                    val roles = auth.user.roles
                    sessionDataStore.saveSession(
                        accessToken = auth.accessToken,
                        refreshToken = auth.refreshToken,
                        userId = auth.user.id,
                        email = auth.user.email,
                        name = "${auth.user.firstName} ${auth.user.lastName}".trim(),
                        role = if (roles.isNotEmpty()) roles.first() else "ROLE_USER"
                    )
                }
                newRequestWithToken(response.request, auth.accessToken)
            } catch (e: Exception) {
                expireSession()
                null
            }
        }
    }

    private fun expireSession() {
        runBlocking { sessionDataStore.clearSession() }
        SessionEvents.notifySessionExpired("Tu sesión expiró. Inicia sesión de nuevo.")
    }

    private fun newRequestWithToken(request: Request, token: String): Request =
        request.newBuilder()
            .header("Authorization", "Bearer $token")
            .header(RETRY_HEADER, "true")
            .build()

    private companion object {
        const val RETRY_HEADER = "X-Auth-Retried"
    }
}
