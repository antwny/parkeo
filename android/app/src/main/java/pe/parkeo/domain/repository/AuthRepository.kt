/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/domain/repository/AuthRepository.kt
 * Capa: dominio (contrato de repositorio).
 * Propósito: declarar las operaciones de autenticación que necesita la app:
 * iniciar sesión, registrarse, renovar el token, cerrar sesión y saber si hay
 * una sesión local. La interfaz describe el "qué"; no dice cómo se ejecuta la
 * red ni dónde se guardan los tokens.
 * Conceptos clave:
 *   - Contrato: la UI y los ViewModels dependen de esta interfaz, no de
 *     `AuthApi` ni de Retrofit. Así se puede cambiar la implementación o usar
 *     una falsa en pruebas sin tocar las pantallas.
 *   - `suspend`: todas las operaciones pueden tardar (red o DataStore) y deben
 *     llamarse desde una corrutina; no bloquean el hilo principal.
 *   - `Result`: el proyecto usa `pe.parkeo.util.Result` con `Success` y `Error`
 *     para representar fallos esperables sin propagar excepciones a la UI.
 *   - DTO frente a modelo: por decisión pragmática actual, este contrato expone
 *     `AuthResponseDto` (un tipo de la capa de datos). Lo ideal en una
 *     arquitectura estricta sería devolver un modelo de dominio de sesión y
 *     mapear el DTO dentro de `AuthRepositoryImpl`; el comentario se mantiene
 *     para que el estudiante identifique ese acoplamiento.
 * Flujo: `AuthViewModel` -> `AuthRepository` -> `AuthRepositoryImpl` ->
 * `AuthApi` (Retrofit) -> backend Spring. La implementación además persiste la
 * sesión en `SessionDataStore`.
 * Relación: `AppContainer` construye `AuthRepositoryImpl` con `AuthApi` y
 * `SessionDataStore`; el backend valida credenciales, emite JWT y aplica RBAC.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.AuthResponseDto
import pe.parkeo.util.Result

/**
 * Contrato de autenticación del dominio.
 *
 * La implementación concreta es `AuthRepositoryImpl`. Ningún ViewModel debería
 * construir `AuthApi` ni manipular DataStore directamente: al depender de esta
 * interfaz, la lógica de presentación queda desacoplada de Retrofit y del
 * almacenamiento local.
 */
interface AuthRepository {
    /**
     * Autentica un usuario con correo y contraseña.
     *
     * La implementación envía las credenciales al endpoint de login, valida la
     * respuesta del backend y, si todo sale bien, guarda access token, refresh
     * token y datos básicos del usuario en la sesión local.
     *
     * @param email Correo electrónico registrado. Se envía tal cual al backend.
     * @param password Contraseña en texto plano solo durante la petición; el
     *   backend la compara con su hash BCrypt y nunca devuelve la contraseña.
     * @return `Result.Success` con `AuthResponseDto` (tokens y usuario) si el
     *   backend confirma; `Result.Error` con un mensaje legible y el código
     *   HTTP cuando la respuesta no es exitosa. En fallos de red o
     *   deserialización el mensaje llega sin código HTTP.
     */
    suspend fun login(email: String, password: String): Result<AuthResponseDto>

    /**
     * Registra un usuario nuevo y, si el backend lo permite, inicia sesión.
     *
     * Al igual que `login`, la implementación guarda la sesión recibida para
     * que el usuario no tenga que autenticarse otra vez.
     *
     * @param email Correo del nuevo usuario; debe ser único en el backend.
     * @param password Contraseña elegida por el usuario.
     * @param firstName Nombres del usuario.
     * @param lastName Apellidos del usuario.
     * @param phone Teléfono de contacto opcional; puede ser `null`.
     * @return `Result.Success` con los tokens y el usuario creado, o
     *   `Result.Error` con el mensaje del backend (por ejemplo correo
     *   duplicado) o de conexión.
     */
    suspend fun register(email: String, password: String, firstName: String, lastName: String, phone: String?): Result<AuthResponseDto>

    /**
     * Cierra la sesión local y solicita al backend revocar el refresh token.
     *
     * No devuelve un `Result` porque el cierre local debe completarse aunque
     * la red falle: la implementación ignora errores remotos y limpia siempre
     * los datos de sesión. La UI puede asumir que, al terminar, no hay sesión.
     */
    suspend fun logout()

    /**
     * Renueva el access token usando el refresh token guardado.
     *
     * La implementación lee el refresh token local, llama al backend y, si la
     * respuesta es válida, reemplaza la sesión completa (tokens y datos del
     * usuario). Si no hay token o el backend lo rechaza, limpia la sesión.
     *
     * @return `Result.Success` con la nueva sesión, o `Result.Error` con
     *   "No hay refresh token", "Sesión expirada" o un mensaje de conexión.
     */
    suspend fun refreshToken(): Result<AuthResponseDto>

    /**
     * Comprueba si existe un access token guardado localmente.
     *
     * Es una comprobación rápida y offline: no valida la firma, el vencimiento
     * ni el estado de la cuenta en el backend. Un token vencido puede hacer
     * que este método devuelva `true`; la siguiente llamada autenticada fallará
     * con 401 y el flujo de refresh/expiración deberá reaccionar.
     *
     * @return `true` si hay un access token no vacío en DataStore; `false` en
     *   caso contrario.
     */
    suspend fun isLoggedIn(): Boolean
}
