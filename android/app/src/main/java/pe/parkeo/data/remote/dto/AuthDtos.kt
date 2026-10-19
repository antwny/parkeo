/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/remote/dto/AuthDtos.kt
 * Capa: datos/red · contrato (DTO de autenticación).
 * Propósito: modelar el JSON de login, registro, refresh, logout y del usuario que
 * devuelve el backend, además del sobre común `ApiResponseDto<T>` usado por todas las
 * APIs.
 * Contrato JSON: cuerpos como `{"email":"...","password":"..."}` y respuestas como
 * `{"success":true,"message":"...","data":{"accessToken":"...","refreshToken":"...",
 * "user":{...}}}`. Las fechas y campos extra no declarados se ignoran.
 * Endpoints: `POST /api/auth/login`, `POST /api/auth/register`,
 * `POST /api/auth/refresh`, `POST /api/auth/logout`.
 * Conceptos clave: Kotlin Serialization, `@Serializable`, `@SerialName`, DTO de
 * petición/respuesta, `ignoreUnknownKeys`, DTO genérico, valores por defecto,
 * nulabilidad, tokens JWT, roles.
 * Flujo resumido: Retrofit serializa el DTO de petición a JSON y deserializa la
 * respuesta en `ApiResponseDto<AuthResponseDto>`; AuthRepositoryImpl valida el sobre
 * y guarda token, identidad y rol en SessionDataStore.
 * Relación: lo usa AuthApi y, a través de ella, AuthRepositoryImpl. `UserDto` también
 * aparece en AdminApi. El backend puede enviar campos adicionales; el `Json` de
 * AppContainer configura `ignoreUnknownKeys = true` para no fallar por ellos.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cuerpo de `POST /api/auth/login`.
 *
 * Viaja como JSON y no se persiste. El backend valida formato y credenciales; un
 * error de validación produce `400` y credenciales incorrectas `401`.
 *
 * @property email correo de la cuenta; obligatorio y con formato de correo.
 * @property password contraseña en texto plano solo durante la petición; nunca debe
 *   registrarse en logs ni guardarse en DataStore. El backend exige entre 8 y 100
 *   caracteres para el registro.
 */
@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

/**
 * Cuerpo de `POST /api/auth/register`.
 *
 * El backend crea la cuenta con rol inicial `ROLE_USER` y devuelve una sesión. El
 * campo `documentNumber` que acepta el backend no está modelado aquí; se omite del
 * JSON. Los campos opcionales con valor por defecto no se envían si conservan `null`.
 *
 * @property email correo único de la cuenta; si ya existe, el backend responde `409`.
 * @property password contraseña; el backend exige entre 8 y 100 caracteres.
 * @property firstName nombre; `@SerialName` mantiene el nombre exacto del contrato.
 * @property lastName apellido; `@SerialName` mantiene el nombre exacto del contrato.
 * @property phone teléfono opcional; `null` omite el campo o lo envía como nulo según
 *   la configuración de serialización.
 */
@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    @SerialName("firstName") val firstName: String,
    @SerialName("lastName") val lastName: String,
    val phone: String? = null
)

/**
 * Cuerpo de `POST /api/auth/refresh` y de `POST /api/auth/logout`.
 *
 * Solo contiene el refresh token. El backend lo busca en su tabla de tokens, verifica
 * que no esté revocado ni expirado y, en el refresh, lo rota: revoca el anterior y
 * emite uno nuevo.
 *
 * @property refreshToken token de larga duración; es una credencial sensible y no
 *   debe registrarse en logs.
 */
@Serializable
data class RefreshTokenRequestDto(
    @SerialName("refreshToken") val refreshToken: String
)

/**
 * Respuesta de login, registro y refresh.
 *
 * El backend también devuelve `tokenType` y `expiresIn`; como no están declarados y
 * `ignoreUnknownKeys` está activo, se descartan sin error. El cliente usa el token
 * directamente con el esquema `Bearer` y no necesita la expiración para renovar:
 * la renovación es explícita mediante `AuthRepository.refreshToken()`.
 *
 * @property accessToken JWT de corta duración para `Authorization: Bearer`.
 * @property refreshToken token de larga duración y revocable.
 * @property user identidad y rol asociados a la sesión.
 */
@Serializable
data class AuthResponseDto(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String,
    val user: UserDto
)

/**
 * Usuario devuelto por autenticación y por el panel de administración.
 *
 * El backend expone `roles` (lista) y un getter `role` que devuelve el primero; ambos
 * llegan en el JSON. Los valores esperados son `ROLE_USER`, `ROLE_OPERATOR` y
 * `ROLE_ADMIN`; la UI los compara con tolerancia a mayúsculas y a las variantes
 * “CLIENTE”, “OPERADOR” y “ADMIN”. `avatarUrl` no siempre viene del backend y
 * `isActive` permite reflejar cuentas deshabilitadas.
 *
 * @property id identificador interno (`BIGINT`) del usuario.
 * @property email correo de la cuenta.
 * @property firstName nombre; `@SerialName` fija el nombre del contrato.
 * @property lastName apellido; `@SerialName` fija el nombre del contrato.
 * @property phone teléfono opcional; puede ser `null`.
 * @property role rol principal como texto; por defecto `ROLE_USER` si el campo falta.
 * @property roles lista completa de roles; por defecto vacía.
 * @property avatarUrl URL opcional de la foto de perfil.
 * @property isActive indica si la cuenta está habilitada; por defecto `true`.
 */
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

/**
 * Sobre común de todas las respuestas del backend.
 *
 * El backend añade `timestamp`, que este DTO ignora. `success` es la verdad lógica del
 * negocio y es independiente del código HTTP: un `200` puede traer `success = false`.
 * `data` es `null` en errores y en operaciones sin contenido (`Unit`/`Void`, como
 * logout o borrados). La deserialización usa `ignoreUnknownKeys = true` y
 * `coerceInputValues = true`, de modo que campos nuevos o nulos inesperados no rompen
 * la app. Para errores 4xx/5xx Retrofit entrega `errorBody()`, que el repositorio
 * interpreta con `NetworkErrorParser`.
 *
 * @param T tipo del contenido cuando la operación es exitosa.
 * @property success indicador lógico de éxito definido por el backend.
 * @property message mensaje legible para el usuario o para depuración.
 * @property data contenido tipado; `null` si no hay datos o hubo error.
 */
@Serializable
data class ApiResponseDto<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)
