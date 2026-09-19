package pe.parkeo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    @SerialName("firstName") val firstName: String,
    @SerialName("lastName") val lastName: String,
    val phone: String? = null
)

@Serializable
data class RefreshTokenRequestDto(
    @SerialName("refreshToken") val refreshToken: String
)

@Serializable
data class AuthResponseDto(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String,
    val user: UserDto
)

@Serializable
data class UserDto(
    val id: Long,
    val email: String,
    @SerialName("firstName") val firstName: String,
    @SerialName("lastName") val lastName: String,
    val phone: String? = null,
    val role: String = "ROLE_USER",
    val roles: List<String> = emptyList(),
    @SerialName("avatarUrl") val avatarUrl: String? = null,
    @SerialName("isActive") val isActive: Boolean = true
)

@Serializable
data class ApiResponseDto<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)
