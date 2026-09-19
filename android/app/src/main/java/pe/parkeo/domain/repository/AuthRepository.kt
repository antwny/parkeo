package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.AuthResponseDto
import pe.parkeo.util.Result

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthResponseDto>
    suspend fun register(email: String, password: String, firstName: String, lastName: String, phone: String?): Result<AuthResponseDto>
    suspend fun logout()
    suspend fun refreshToken(): Result<AuthResponseDto>
    suspend fun isLoggedIn(): Boolean
}
