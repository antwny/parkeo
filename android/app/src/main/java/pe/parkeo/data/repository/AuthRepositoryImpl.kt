package pe.parkeo.data.repository

import pe.parkeo.data.local.SessionDataStore
import pe.parkeo.data.remote.api.AuthApi
import pe.parkeo.data.remote.dto.*
import pe.parkeo.domain.repository.AuthRepository
import pe.parkeo.util.Result

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val sessionDataStore: SessionDataStore
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<AuthResponseDto> {
        return try {
            val response = authApi.login(LoginRequestDto(email, password))
            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()!!.data!!
                sessionDataStore.saveSession(
                    accessToken = authData.accessToken,
                    refreshToken = authData.refreshToken,
                    userId = authData.user.id,
                    email = authData.user.email,
                    name = "${authData.user.firstName} ${authData.user.lastName}",
                    role = authData.user.role
                )
                Result.Success(authData)
            } else {
                Result.Error(response.body()?.message ?: "Error de autenticación", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String?
    ): Result<AuthResponseDto> {
        return try {
            val response = authApi.register(
                RegisterRequestDto(email, password, firstName, lastName, phone)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()!!.data!!
                sessionDataStore.saveSession(
                    accessToken = authData.accessToken,
                    refreshToken = authData.refreshToken,
                    userId = authData.user.id,
                    email = authData.user.email,
                    name = "${authData.user.firstName} ${authData.user.lastName}",
                    role = authData.user.role
                )
                Result.Success(authData)
            } else {
                Result.Error(response.body()?.message ?: "Error al registrarse", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    override suspend fun logout() {
        try {
            val refreshToken = sessionDataStore.getRefreshToken()
            if (!refreshToken.isNullOrBlank()) {
                authApi.logout(RefreshTokenRequestDto(refreshToken))
            }
        } catch (e: Exception) {
            // Ignorar errores de logout en red, siempre limpiar sesión local
        } finally {
            sessionDataStore.clearSession()
        }
    }

    override suspend fun refreshToken(): Result<AuthResponseDto> {
        return try {
            val refreshToken = sessionDataStore.getRefreshToken()
                ?: return Result.Error("No hay refresh token")
            val response = authApi.refresh(RefreshTokenRequestDto(refreshToken))
            if (response.isSuccessful && response.body()?.success == true) {
                val authData = response.body()!!.data!!
                sessionDataStore.saveSession(
                    accessToken = authData.accessToken,
                    refreshToken = authData.refreshToken,
                    userId = authData.user.id,
                    email = authData.user.email,
                    name = "${authData.user.firstName} ${authData.user.lastName}",
                    role = authData.user.role
                )
                Result.Success(authData)
            } else {
                sessionDataStore.clearSession()
                Result.Error("Sesión expirada")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return !sessionDataStore.getAccessToken().isNullOrBlank()
    }
}
