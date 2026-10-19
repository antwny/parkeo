/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/repository/ReservationRepositoryImpl.kt
 * Capa: datos (implementación de repositorio).
 * Propósito: implementar `ReservationRepository` con llamadas a
 * `ReservationApi`, validar el envoltorio `ApiResponseDto` del backend y
 * convertir errores HTTP/red en mensajes comprensibles para el usuario.
 * Conceptos clave:
 *   - Implementación de contrato: los ViewModels dependen de la interfaz;
 *     `DefaultAppContainer` construye esta clase con `ReservationApi`.
 *   - `Response`: se exige `isSuccessful` (HTTP 2xx) y `body().success`.
 *   - `body()` / `data`: la lista viene dentro de `PageDto.content`; el
 *     detalle y las acciones devuelven un único `ReservationDto`.
 *   - `errorBody`: en 400/409 el backend explica el conflicto real en el JSON
 *     de error, no en `body()`; se parsea con Kotlin Serialization.
 *   - `Result`: `Success` con DTO o `Error` con mensaje (sin código HTTP en
 *     esta implementación).
 *   - DTO frente a modelo: se devuelve `ReservationDto` con `status: String`;
 *     el modelo `Reservation` con `ReservationStatus` no se usa aquí, por lo
 *     que el mapeo DTO -> dominio queda pendiente.
 * Flujo: pantalla -> `ReservationViewModel` -> contrato -> esta clase ->
 * Retrofit -> backend Spring -> MySQL. El backend valida solapamientos,
 * calcula precio y aplica transiciones de estado.
 * Relación: `ReservationViewModel` consume la interfaz; `AppContainer` inyecta
 * `ReservationApi`; el backend Spring aplica las reglas transaccionales.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.repository

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import pe.parkeo.data.remote.api.ReservationApi
import pe.parkeo.data.remote.dto.*
import pe.parkeo.domain.repository.ReservationRepository
import pe.parkeo.util.Result
import retrofit2.Response

/**
 * Implementación concreta de `ReservationRepository`.
 *
 * No mantiene caché: cada operación consulta o modifica el backend. Los
 * errores de negocio se conservan como texto para que la UI pueda explicar,
 * por ejemplo, que el espacio ya está ocupado.
 *
 * @param reservationApi Cliente Retrofit con las rutas de reservas del usuario.
 */
class ReservationRepositoryImpl(
    private val reservationApi: ReservationApi
) : ReservationRepository {

    /**
     * Parser tolerante usado solo para leer el `errorBody`. `ignoreUnknownKeys`
     * permite que el backend agregue campos sin romper la extracción del
     * mensaje.
     */
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Lista las reservas del usuario autenticado.
     *
     * Llama a GET `api/reservations` con el filtro `status` (puede ser `null`)
     * y la paginación por defecto (página 0, tamaño 20). Si la respuesta es
     * válida, devuelve `data.content` o lista vacía. Si no, usa
     * `errorMessage` para extraer el mensaje del `errorBody` o aplicar un
     * mensaje según el código HTTP. Los fallos de red se capturan y se
     * devuelven como `Error` con "Error de conexión" si no hay mensaje.
     *
     * @param status Filtro por estado técnico o `null` para todos.
     * @return `Result.Success` con las reservas o `Result.Error` con un
     *   mensaje legible.
     */
    override suspend fun getMyReservations(status: String?): Result<List<ReservationDto>> {
        return try {
            val response = reservationApi.getMyReservations(status)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data?.content ?: emptyList())
            } else Result.Error(errorMessage(response, "Error al cargar reservas"))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Obtiene el detalle de una reserva.
     *
     * Llama a GET `api/reservations/{id}`. Si HTTP es exitoso y
     * `success = true`, usa `data!!`: si el backend omite los datos, el
     * `NullPointerException` cae en el `catch` y se convierte en `Error`. Si
     * la respuesta no es válida, delega en `errorMessage`.
     *
     * @param id Identificador de la reserva.
     * @return `Result.Success` con `ReservationDto` o `Result.Error`.
     */
    override suspend fun getReservationDetail(id: Long): Result<ReservationDto> {
        return try {
            val response = reservationApi.getReservationDetail(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data!!)
            } else Result.Error(errorMessage(response, "Error al cargar la reserva"))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Crea una reserva en el backend.
     *
     * Construye `CreateReservationRequestDto` con todos los parámetros (los
     * opcionales viajan como `null`) y llama a POST `api/reservations`. El
     * backend valida horario, vehículo, solapamientos, estado del
     * estacionamiento, asigna espacio si `parkingSpaceId` es `null` y calcula
     * el precio. Si la respuesta es válida, devuelve el `ReservationDto`
     * creado; si no, usa `errorMessage` para conservar el motivo real (por
     * ejemplo un 409 por espacio ocupado). Los fallos de red se capturan.
     *
     * @param parkingLotId Estacionamiento donde se reserva.
     * @param parkingSpaceId Espacio concreto o `null` para asignación
     *   automática.
     * @param vehicleId Vehículo del usuario.
     * @param startTime Inicio en ISO-8601 local.
     * @param endTime Fin en ISO-8601 local.
     * @param notes Nota opcional.
     * @return `Result.Success` con la reserva creada o `Result.Error` con el
     *   mensaje del backend o de conexión.
     */
    override suspend fun createReservation(
        parkingLotId: Long,
        parkingSpaceId: Long?,
        vehicleId: Long,
        startTime: String,
        endTime: String,
        notes: String?
    ): Result<ReservationDto> {
        return try {
            val response = reservationApi.createReservation(
                CreateReservationRequestDto(
                    parkingLotId = parkingLotId,
                    parkingSpaceId = parkingSpaceId,
                    vehicleId = vehicleId,
                    startTime = startTime,
                    endTime = endTime,
                    notes = notes
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data!!)
            } else Result.Error(errorMessage(response, "Error al crear reserva"))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Cancela una reserva del usuario.
     *
     * Llama a PATCH `api/reservations/{id}/cancel` con
     * `CancelReservationRequestDto(reason)` (el motivo puede ser `null`). El
     * backend verifica propiedad y estado, marca la reserva como `CANCELLED`
     * y libera el espacio. Si la respuesta es válida, devuelve el DTO
     * actualizado; si no, aplica `errorMessage` para explicar el rechazo. Los
     * errores de red se capturan.
     *
     * @param id Identificador de la reserva.
     * @param reason Motivo opcional de cancelación.
     * @return `Result.Success` con la reserva actualizada o `Result.Error`.
     */
    override suspend fun cancelReservation(id: Long, reason: String?): Result<ReservationDto> {
        return try {
            val response = reservationApi.cancelReservation(id, CancelReservationRequestDto(reason))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data!!)
            } else Result.Error(errorMessage(response, "Error al cancelar reserva"))
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Extrae el mensaje de error más útil de una respuesta fallida.
     *
     * Cuando el backend responde 400/409/etc., el detalle suele venir en el
     * `errorBody` (por ejemplo "El espacio ya está reservado para el período
     * solicitado"), no en `body()`, que en errores HTTP suele ser `null`.
     *
     * Secuencia:
     * 1. Intenta leer `errorBody().string()`; si falla, continúa con `null`.
     *    El cuerpo es de un solo uso: una segunda lectura devolvería vacío.
     * 2. Si hay texto, lo interpreta como JSON y toma la propiedad `message`
     *    de nivel superior. Si no existe o está vacía, ignora el resultado.
     * 3. Si no obtuvo mensaje, aplica un texto según `response.code()`:
     *    401 sesión expirada, 403 sin permiso, 404 no encontrado, 409
     *    conflicto de espacio/horario; para cualquier otro código usa
     *    `fallback`.
     *
     * Así la UI recibe una explicación comprensible incluso si el backend
     * cambia el formato del error.
     *
     * @param response Respuesta Retrofit fallida; se usan `errorBody` y
     *   `code`.
     * @param fallback Mensaje por defecto del método que hizo la llamada.
     * @return Mensaje extraído del JSON, texto específico por código HTTP o
     *   `fallback`; nunca `null`.
     */
    private fun errorMessage(response: Response<*>, fallback: String): String {
        val raw = try {
            response.errorBody()?.string()
        } catch (e: Exception) {
            null
        }

        if (!raw.isNullOrBlank()) {
            val message = try {
                json.parseToJsonElement(raw).jsonObject["message"]?.jsonPrimitive?.contentOrNull
            } catch (e: Exception) {
                null
            }
            if (!message.isNullOrBlank()) return message
        }

        return when (response.code()) {
            401 -> "Tu sesión expiró. Inicia sesión de nuevo"
            403 -> "No tienes permiso para esta acción"
            404 -> "No se encontró lo que buscas"
            409 -> "Ese espacio u horario ya está ocupado"
            else -> fallback
        }
    }
}