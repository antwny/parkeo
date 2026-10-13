// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Contrato o modelo del dominio; expresa conceptos/reglas de Parkeo sin acoplarse a HTTP ni a Android.
package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.AuthResponseDto
import pe.parkeo.util.Result

interface AuthRepository {
    /** Autentica las credenciales y actualiza el estado de sesión: login. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun login(email: String, password: String): Result<AuthResponseDto>
    suspend fun register(email: String, password: String, firstName: String, lastName: String, phone: String?): Result<AuthResponseDto>
    suspend fun logout()
    /** Ejecuta la operación indicada por el nombre: refresh token. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun refreshToken(): Result<AuthResponseDto>
    suspend fun isLoggedIn(): Boolean
}
