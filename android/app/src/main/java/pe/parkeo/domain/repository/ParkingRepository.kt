/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/domain/repository/ParkingRepository.kt
 * Capa: dominio (contrato de repositorio).
 * Propósito: declarar las consultas de estacionamientos que necesitan la
 * pantalla de inicio, la búsqueda, el mapa, el detalle y el flujo de reserva:
 * listado, cercanía, búsqueda por texto, disponibilidad y espacios libres.
 * Conceptos clave:
 *   - Contrato: los ViewModels dependen de esta interfaz y no de `ParkingApi`.
 *     La implementación real es `ParkingRepositoryImpl`; en pruebas podría
 *     sustituirse por una falsa.
 *   - `suspend`: las operaciones de red no bloquean el hilo principal y deben
 *     ejecutarse desde un `viewModelScope` u otra corrutina.
 *   - `Result`: encapsula éxito o error con un mensaje apto para la UI.
 *   - DTO frente a modelo: este contrato devuelve DTO de red (`ParkingLotDto`,
 *     `ParkingLotDetailDto`, etc.) en lugar de los modelos de dominio
 *     `ParkingLot`. Es una decisión pragmática del código actual y un punto de
 *     acoplamiento que conviene reconocer al estudiar la arquitectura.
 * Flujo: pantalla -> ViewModel -> `ParkingRepository` ->
 * `ParkingRepositoryImpl` -> `ParkingApi` -> backend Spring. El backend
 * consulta MySQL, aplica reglas y responde JSON; Retrofit lo deserializa.
 * Relación: `AppContainer` inyecta la implementación; el backend expone rutas
 * bajo `/api/parking`. Los DTO devueltos alimentan `HomeViewModel` y las
 * pantallas de detalle/reserva.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.*
import pe.parkeo.util.Result

/**
 * Contrato de acceso a estacionamientos.
 *
 * La implementación concreta es `ParkingRepositoryImpl`. La UI no debería
 * importar Retrofit: si cambia la ruta, la paginación o la forma del JSON,
 * los cambios se concentran en la implementación y el contrato se mantiene.
 */
interface ParkingRepository {
    /**
     * Obtiene el listado general de estacionamientos.
     *
     * La implementación actual llama a `GET api/parking` sin parámetros, por
     * lo que el backend aplica su paginación por defecto (página 0, tamaño 20)
     * y solo se devuelve la primera página. No existe en este contrato un
     * parámetro para pedir páginas siguientes.
     *
     * @return `Result.Success` con la lista de `ParkingLotDto`; lista vacía si
     *   el backend responde sin contenido. `Result.Error` si la respuesta no
     *   es exitosa o si falla la red/serialización.
     */
    suspend fun getParkingLots(): Result<List<ParkingLotDto>>

    /**
     * Obtiene el detalle completo de un estacionamiento.
     *
     * El detalle incluye servicios, horarios y tarifas que no aparecen en el
     * listado, por eso se consulta aparte.
     *
     * @param id Identificador del estacionamiento.
     * @return `Result.Success` con `ParkingLotDetailDto`, o `Result.Error` con
     *   "Estacionamiento no encontrado" si el backend no devuelve datos, o con
     *   el mensaje de error correspondiente.
     */
    suspend fun getParkingLotDetail(id: Long): Result<ParkingLotDetailDto>

    /**
     * Busca estacionamientos cercanos a una coordenada.
     *
     * La distancia y el orden dependen del cálculo del backend. Si no hay
     * resultados dentro del radio, se devuelve una lista vacía.
     *
     * @param lat Latitud de la ubicación del usuario en grados decimales.
     * @param lon Longitud de la ubicación del usuario en grados decimales.
     * @param radius Radio máximo de búsqueda en kilómetros. Vale 5.0 por
     *   defecto; el backend también asume 5.0 si no se envía.
     * @return `Result.Success` con la lista cercana o `Result.Error` si falla
     *   la consulta; no se propaga una excepción a la UI.
     */
    suspend fun getNearbyParkingLots(lat: Double, lon: Double, radius: Double = 5.0): Result<List<ParkingLotDto>>

    /**
     * Busca estacionamientos por texto libre (nombre, dirección, distrito).
     *
     * La implementación no expone paginación y usa los valores por defecto
     * del backend, de modo que solo entrega la primera página de resultados.
     *
     * @param query Texto de búsqueda ingresado por el usuario. Debe viajar
     *   como query string; el backend decide qué campos compara.
     * @return `Result.Success` con las coincidencias o lista vacía;
     *   `Result.Error` si la petición falla.
     */
    suspend fun searchParkingLots(query: String): Result<List<ParkingLotDto>>

    /**
     * Calcula la disponibilidad resumida de un estacionamiento.
     *
     * En la implementación actual se consulta la lista de espacios y se cuenta
     * cuántos están disponibles; `occupiedSpaces` es el resto y
     * `reservedSpaces` se informa como 0. El backend no recibe una fecha en
     * esta llamada, por lo que el parámetro `date` no se usa: la foto
     * corresponde al estado actual, no a una fecha futura. Además, la ruta del
     * backend devuelve únicamente espacios disponibles, así que el
     * `totalCapacity` calculado es el tamaño de esa lista y no la capacidad
     * física total del estacionamiento; los contadores deben interpretarse con
     * esa limitación.
     *
     * @param id Identificador del estacionamiento.
     * @param date Fecha ISO sugerida para una consulta futura. Se conserva en
     *   el contrato por compatibilidad, pero actualmente se ignora.
     * @return `Result.Success` con `AvailabilityDto` calculado localmente a
     *   partir de los espacios, o `Result.Error` si falla la consulta.
     */
    suspend fun getAvailability(id: Long, date: String?): Result<AvailabilityDto>

    /**
     * Obtiene los espacios reservables de un estacionamiento.
     *
     * La implementación envía solo el identificador del estacionamiento y el
     * tipo de vehículo; `startTime` y `endTime` no llegan al backend porque la
     * ruta actual no los acepta. La validación definitiva de conflictos de
     * horario ocurre al crear la reserva.
     *
     * @param id Identificador del estacionamiento.
     * @param startTime Inicio deseado en formato ISO-8601. Se mantiene en el
     *   contrato, pero la implementación actual no lo envía.
     * @param endTime Fin deseado en formato ISO-8601. Tampoco se envía hoy.
     * @param vehicleTypeId Tipo de vehículo para filtrar espacios compatibles;
     *   `null` significa no filtrar por tipo.
     * @return `Result.Success` con los espacios disponibles o `Result.Error`
     *   si la petición falla.
     */
    suspend fun getAvailableSpaces(id: Long, startTime: String, endTime: String, vehicleTypeId: Long?): Result<List<ParkingSpaceDto>>
}
