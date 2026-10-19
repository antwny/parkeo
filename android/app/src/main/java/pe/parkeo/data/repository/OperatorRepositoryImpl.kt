/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/repository/OperatorRepositoryImpl.kt
 * Capa: datos (implementación de repositorio).
 * Propósito: implementar `OperatorRepository` con `OperatorApi`: consultar
 * estacionamientos asignados, espacios y reservas, cambiar estados de espacio
 * y registrar ingresos/salidas. Incluye extracción de mensajes de error del
 * `errorBody` y un helper `safeApiCall`.
 * Conceptos clave:
 *   - Implementación de contrato: `OperatorViewModel` usa la interfaz;
 *     `DefaultAppContainer` construye esta clase.
 *   - `Response`: se exige HTTP exitoso y `body().success == true`.
 *   - `errorBody`: se parsea con Kotlin Serialization para obtener el campo
 *     `message`; si no se puede, se usa el mensaje HTTP o un texto por defecto.
 *   - `safeApiCall`: centraliza `try/catch` y convierte excepciones en
 *     `Result.Error`.
 *   - DTO frente a modelo: se devuelven DTO de red; no hay mapeo a los modelos
 *     de dominio en esta versión.
 *   - Autorización horizontal: el backend verifica que el estacionamiento
 *     pertenezca al operador; el cliente no debe asumirlo.
 * Flujo: pantalla de operador -> ViewModel -> contrato -> esta clase ->
 * Retrofit -> backend Spring -> MySQL.
 * Relación: `OperatorViewModel` consume la interfaz; `AppContainer` inyecta
 * `OperatorApi`; el backend Spring valida la asignación del operador y guarda
 * los cambios en MySQL.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.repository

import pe.parkeo.data.remote.api.OperatorApi
import pe.parkeo.data.remote.dto.*
import pe.parkeo.domain.repository.OperatorRepository
import pe.parkeo.util.Result

/**
 * Implementación concreta de `OperatorRepository`.
 *
 * Las rutas de operador requieren un JWT válido; el backend además comprueba
 * que el usuario sea el operador asignado al estacionamiento solicitado.
 *
 * @param operatorApi Cliente Retrofit con las rutas de operador.
 */
class OperatorRepositoryImpl(
    private val operatorApi: OperatorApi
) : OperatorRepository {

    /**
     * Lista los estacionamientos asignados al operador.
     *
     * Llama a GET `api/operator/parking-lots`. Devuelve `data` o lista vacía;
     * si falla, lanza una excepción con el mensaje del cuerpo o "Error al
     * obtener estacionamientos", que `safeApiCall` convierte en `Error`.
     *
     * @return `Result.Success` con la lista o `Result.Error`.
     */
    override suspend fun getMyParkingLots(): Result<List<ParkingLotDto>> = safeApiCall {
        val response = operatorApi.getMyParkingLots()
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error al obtener estacionamientos")
    }

    /**
     * Obtiene los espacios de un estacionamiento.
     *
     * Llama a GET `api/operator/parking-lots/{id}/spaces`. Devuelve `data` o
     * lista vacía; si falla, lanza una excepción con el mensaje del cuerpo o
     * "Error al obtener espacios".
     *
     * @param lotId Identificador del estacionamiento.
     * @return `Result.Success` con los espacios o `Result.Error`.
     */
    override suspend fun getSpaces(lotId: Long): Result<List<ParkingSpaceDto>> = safeApiCall {
        val response = operatorApi.getSpaces(lotId)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error al obtener espacios")
    }

    /**
     * Cambia el estado de un espacio.
     *
     * Llama a PATCH `api/operator/spaces/{spaceId}/status` con
     * `UpdateSpaceStatusRequestDto(status, notes)`. El estado debe ser uno de
     * los valores técnicos (`AVAILABLE`, `OCCUPIED`, `RESERVED`,
     * `MAINTENANCE`); el backend valida la transición y la pertenencia. Si la
     * respuesta es válida devuelve `data!!`; si no, lanza una excepción con el
     * mensaje del cuerpo o "Error al actualizar estado del espacio".
     *
     * @param spaceId Identificador del espacio.
     * @param status Nuevo estado técnico.
     * @param notes Nota opcional; puede ser `null`.
     * @return `Result.Success` con el espacio actualizado o `Result.Error`.
     */
    override suspend fun updateSpaceStatus(
        spaceId: Long,
        status: String,
        notes: String?
    ): Result<ParkingSpaceDto> = safeApiCall {
        val response = operatorApi.updateSpaceStatus(spaceId, UpdateSpaceStatusRequestDto(status, notes))
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(response.body()?.message ?: "Error al actualizar estado del espacio")
    }

    /**
     * Lista las reservas de un estacionamiento.
     *
     * Llama a GET `api/operator/reservations` con `parkingLotId`, filtro
     * `status` y paginación por defecto (página 0, tamaño 50). Si la respuesta
     * es válida devuelve `data.content`; si no, lanza una excepción con
     * `extractError(response, "Error al obtener reservas")`, que intenta leer
     * el mensaje del `errorBody` y cae en el mensaje por defecto.
     *
     * @param lotId Identificador del estacionamiento.
     * @param status Filtro por estado técnico o `null`.
     * @return `Result.Success` con las reservas o `Result.Error`.
     */
    override suspend fun getReservations(lotId: Long, status: String?): Result<List<ReservationDto>> = safeApiCall {
        val response = operatorApi.getReservations(lotId, status)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data?.content ?: emptyList()
        } else throw Exception(extractError(response, "Error al obtener reservas"))
    }

    /**
     * Registra el ingreso de un vehículo.
     *
     * Llama a PATCH `api/operator/reservations/{id}/check-in`. El backend
     * valida que el operador administre el estacionamiento y que la reserva
     * esté `PENDING` o `CONFIRMED`; luego la pasa a `ACTIVE`, marca el espacio
     * como `OCCUPIED` y ajusta la disponibilidad. Devuelve el DTO actualizado
     * o lanza una excepción con `extractError` y "Error al registrar ingreso".
     *
     * @param reservationId Identificador de la reserva.
     * @return `Result.Success` con la reserva actualizada o `Result.Error`.
     */
    override suspend fun checkIn(reservationId: Long): Result<ReservationDto> = safeApiCall {
        val response = operatorApi.checkInReservation(reservationId)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(extractError(response, "Error al registrar ingreso"))
    }

    /**
     * Registra la salida de un vehículo.
     *
     * Llama a PATCH `api/operator/reservations/{id}/check-out`. El backend
     * exige que la reserva esté `ACTIVE`, la marca como `COMPLETED`, libera el
     * espacio a `AVAILABLE` y ajusta la disponibilidad. Devuelve el DTO
     * actualizado o lanza una excepción con `extractError` y "Error al
     * registrar salida".
     *
     * @param reservationId Identificador de la reserva.
     * @return `Result.Success` con la reserva actualizada o `Result.Error`.
     */
    override suspend fun checkOut(reservationId: Long): Result<ReservationDto> = safeApiCall {
        val response = operatorApi.checkOutReservation(reservationId)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(extractError(response, "Error al registrar salida"))
    }

    /**
     * Extrae el mensaje del cuerpo de error de una respuesta de Retrofit.
     *
     * Secuencia:
     * 1. Lee `errorBody().string()` (un solo uso).
     * 2. Si el texto no está vacío, lo parsea como `JsonObject` con
     *    `ignoreUnknownKeys` y busca la propiedad `message` de tipo primitivo.
     * 3. Devuelve ese mensaje si es válido; en caso contrario, `defaultMsg`.
     * 4. Si la lectura o el parseo fallan, usa `response.message()` (la frase
     *    HTTP, por ejemplo "Conflict"); si está vacía, `defaultMsg`.
     *
     * Es una versión específica de esta clase; `ReservationRepositoryImpl` y
     * `NetworkErrorParser` tienen estrategias equivalentes.
     *
     * @param response Respuesta fallida de Retrofit.
     * @param defaultMsg Mensaje de respaldo del método llamador.
     * @return Mensaje del backend, frase HTTP o `defaultMsg`; nunca `null`.
     */
    private fun extractError(response: retrofit2.Response<*>, defaultMsg: String): String {
        return try {
            val raw = response.errorBody()?.string()
            if (!raw.isNullOrBlank()) {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val obj = json.parseToJsonElement(raw)
                val msg = obj.let {
                    (it as? kotlinx.serialization.json.JsonObject)?.get("message")?.let { elem ->
                        (elem as? kotlinx.serialization.json.JsonPrimitive)?.content
                    }
                }
                msg ?: defaultMsg
            } else defaultMsg
        } catch (e: Exception) {
            response.message().ifBlank { defaultMsg }
        }
    }

    /**
     * Ejecuta una operación suspensible y normaliza éxito/error.
     *
     * Si la lambda termina, devuelve `Result.Success` con su valor; si lanza
     * cualquier `Exception`, devuelve `Result.Error` con el mensaje o "Error
     * de conexión". No conserva el código HTTP. El `catch (Exception)` también
     * captura cancelaciones de corrutina y errores de programación; es una
     * simplificación didáctica del proyecto.
     *
     * @param call Lambda suspendida que realiza la petición y valida la
     *   respuesta.
     * @return `Result.Success` con el valor o `Result.Error` con mensaje.
     */
    private suspend fun <T> safeApiCall(call: suspend () -> T): Result<T> {
        return try {
            Result.Success(call())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }
}
