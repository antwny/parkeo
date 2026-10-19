/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/repository/ParkingRepositoryImpl.kt
 * Capa: datos (implementación de repositorio).
 * Propósito: implementar `ParkingRepository` llamando a `ParkingApi` y
 * traduciendo cada `Response` de Retrofit a `Result`. Incluye un helper
 * genérico `safeApiCall` que centraliza el `try/catch`.
 * Conceptos clave:
 *   - Implementación de contrato: la UI depende de `ParkingRepository`, no de
 *     esta clase ni de Retrofit.
 *   - `Response`: `isSuccessful` cubre 2xx; además se exige `body().success`
 *     porque el backend envuelve la respuesta en `ApiResponseDto`.
 *   - `body()` y `data`: el contenido útil puede ser `null` o una página
 *     (`PageDto`) cuyo `content` es la lista real.
 *   - `safeApiCall`: recibe una lambda suspendida, devuelve `Success` con su
 *     resultado o `Error` con el mensaje de la excepción.
 *   - DTO frente a modelo: los métodos devuelven DTO de red sin mapearlos al
 *     modelo `ParkingLot`. El mapeo DTO -> dominio no está implementado en
 *     esta versión; por eso la UI consume `ParkingLotDto`.
 * Flujo: ViewModel -> contrato -> esta clase -> `ParkingApi` -> backend
 * Spring -> MySQL. `DefaultAppContainer` inyecta `ParkingApi`.
 * Relación: los ViewModels dependen de `ParkingRepository`; `AppContainer`
 * construye esta implementación; `ParkingApi` y el backend Spring resuelven la
 * parte remota.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.repository

import pe.parkeo.data.remote.api.ParkingApi
import pe.parkeo.data.remote.dto.*
import pe.parkeo.domain.repository.ParkingRepository
import pe.parkeo.util.Result

/**
 * Implementación concreta de `ParkingRepository`.
 *
 * No guarda estado ni caché: cada llamada consulta el backend. La
 * disponibilidad, por tanto, es una foto del momento de la petición.
 *
 * @param parkingApi Cliente Retrofit con las rutas públicas de estacionamientos.
 */
class ParkingRepositoryImpl(
    private val parkingApi: ParkingApi
) : ParkingRepository {

    /**
     * Obtiene la primera página del listado de estacionamientos.
     *
     * Secuencia: llama a `getParkingLots()` (GET `api/parking` con página 0 y
     * tamaño 20 por defecto); si HTTP es exitoso y `success = true`, devuelve
     * `data.content` o una lista vacía; si no, lanza una excepción con el
     * mensaje del cuerpo o "Error al obtener estacionamientos", que
     * `safeApiCall` convierte en `Error`.
     *
     * @return `Result.Success` con la lista o `Result.Error` con el mensaje.
     */
    override suspend fun getParkingLots(): Result<List<ParkingLotDto>> = safeApiCall {
        val response = parkingApi.getParkingLots()
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data?.content ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error al obtener estacionamientos")
    }

    /**
     * Obtiene el detalle de un estacionamiento.
     *
     * Llama a GET `api/parking/{id}`. Si la respuesta es válida pero `data` es
     * `null`, lanza "Estacionamiento no encontrado"; si el HTTP no es exitoso,
     * usa el mensaje del cuerpo o "Error". `safeApiCall` traduce la excepción.
     *
     * @param id Identificador del estacionamiento.
     * @return `Result.Success` con `ParkingLotDetailDto` o `Result.Error`.
     */
    override suspend fun getParkingLotDetail(id: Long): Result<ParkingLotDetailDto> = safeApiCall {
        val response = parkingApi.getParkingLotDetail(id)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: throw Exception("Estacionamiento no encontrado")
        } else throw Exception(response.body()?.message ?: "Error")
    }

    /**
     * Busca estacionamientos dentro de un radio de una coordenada.
     *
     * Llama a GET `api/parking/nearby` con latitud, longitud y radio en
     * kilómetros. El backend calcula la distancia y ordena; aquí solo se
     * devuelve `data` o una lista vacía. Si falla, lanza una excepción que
     * `safeApiCall` convierte en `Error`.
     *
     * @param lat Latitud en grados decimales.
     * @param lon Longitud en grados decimales.
     * @param radius Radio en kilómetros; el valor por defecto de la interfaz
     *   es 5.0.
     * @return `Result.Success` con los estacionamientos cercanos o
     *   `Result.Error`.
     */
    override suspend fun getNearbyParkingLots(lat: Double, lon: Double, radius: Double): Result<List<ParkingLotDto>> = safeApiCall {
        val response = parkingApi.getNearbyParkingLots(lat, lon, radius)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error")
    }

    /**
     * Busca estacionamientos por texto.
     *
     * Llama a GET `api/parking/search` con `q = query` y la paginación por
     * defecto (página 0, tamaño 20). Devuelve `data.content` o lista vacía. El
     * mensaje de error de respaldo es "Error en búsqueda".
     *
     * @param query Texto ingresado por el usuario.
     * @return `Result.Success` con las coincidencias o `Result.Error`.
     */
    override suspend fun searchParkingLots(query: String): Result<List<ParkingLotDto>> = safeApiCall {
        val response = parkingApi.searchParkingLots(query)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data?.content ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error en búsqueda")
    }

    /**
     * Calcula la disponibilidad de un estacionamiento a partir de sus espacios.
     *
     * Llama a GET `api/parking/{id}/availability` sin enviar `date` ni tipo de
     * vehículo. En el backend actual esa ruta devuelve solo los espacios
     * disponibles, no todos los espacios físicos. Aquí se cuenta cuántos
     * cumplen `isAvailable = true` y se construye un `AvailabilityDto` local:
     * total = tamaño de la lista, disponibles = contador, ocupados = resto y
     * reservados = 0. Como la lista ya viene filtrada, el total calculado es la
     * cantidad de espacios devueltos (no la capacidad física total) y
     * `occupiedSpaces` normalmente queda en 0. La fecha solicitada tampoco
     * influye en el resultado; son limitaciones reales de esta implementación.
     *
     * @param id Identificador del estacionamiento.
     * @param date Fecha ISO que el contrato conserva, pero que la
     *   implementación ignora.
     * @return `Result.Success` con el resumen calculado o `Result.Error`.
     */
    override suspend fun getAvailability(id: Long, date: String?): Result<AvailabilityDto> = safeApiCall {
        val response = parkingApi.getAvailability(id)
        if (response.isSuccessful && response.body()?.success == true) {
            val spaces = response.body()!!.data ?: emptyList()
            val available = spaces.count { it.isAvailable }
            AvailabilityDto(
                parkingLotId = id,
                totalCapacity = spaces.size,
                availableSpaces = available,
                occupiedSpaces = spaces.size - available,
                reservedSpaces = 0
            )
        } else throw Exception(response.body()?.message ?: "Error")
    }

    /**
     * Obtiene los espacios disponibles de un estacionamiento.
     *
     * Llama a GET `api/parking/{id}/spaces/available` enviando solo
     * `vehicleTypeId` (puede ser `null`). `startTime` y `endTime` no forman
     * parte de la ruta y no se envían; la reserva definitiva validará el
     * horario. Devuelve `data` o lista vacía, y traduce fallos a `Error`.
     *
     * @param id Identificador del estacionamiento.
     * @param startTime Inicio deseado; conservado por contrato, ignorado hoy.
     * @param endTime Fin deseado; conservado por contrato, ignorado hoy.
     * @param vehicleTypeId Tipo de vehículo para filtrar, o `null`.
     * @return `Result.Success` con los espacios o `Result.Error`.
     */
    override suspend fun getAvailableSpaces(
        id: Long,
        startTime: String,
        endTime: String,
        vehicleTypeId: Long?
    ): Result<List<ParkingSpaceDto>> = safeApiCall {
        val response = parkingApi.getAvailableSpaces(id, vehicleTypeId)
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error")
    }

    /**
     * Ejecuta una operación suspensible y normaliza el resultado.
     *
     * Es un helper privado para no repetir `try/catch` en cada método. Si la
     * lambda termina bien, envuelve el valor en `Result.Success`; si lanza
     * cualquier `Exception`, devuelve `Result.Error` con `message` o
     * "Error desconocido".
     *
     * Limitaciones didácticas: el `catch (Exception)` también captura la
     * cancelación de corrutinas y convierte fallos de programación (por
     * ejemplo un `NullPointerException` por `!!`) en errores de UI; además no
     * conserva el código HTTP. Los métodos de esta clase lanzan excepciones a
     * propósito para que este helper las traduzca.
     *
     * @param apiCall Lambda suspendida que realiza la llamada y valida la
     *   respuesta; su tipo de retorno es `T`.
     * @return `Result.Success` con el valor de `apiCall` o `Result.Error` con
     *   un mensaje legible.
     */
    private suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
        return try {
            Result.Success(apiCall())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error desconocido")
        }
    }
}
