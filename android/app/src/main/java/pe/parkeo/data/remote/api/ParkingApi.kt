/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/remote/api/ParkingApi.kt
 * Capa: datos/red (contrato Retrofit de estacionamientos).
 * Propósito: declarar la consulta pública de estacionamientos, búsqueda, cercanía,
 * detalle y disponibilidad de espacios.
 * Conceptos clave: Retrofit, `@GET`, `@Path`, `@Query`, paginación de Spring,
 * `PageDto<T>`, `Response<T>`, coordenadas, disponibilidad.
 * Flujo resumido: ParkingRepositoryImpl llama a estos métodos y obtiene el contenido
 * de `ApiResponseDto.data`; si `success` es verdadero, la UI muestra la lista o el
 * detalle. Estas rutas no requieren token según Spring Security, aunque el
 * AuthInterceptor añadirá `Authorization` si hay sesión.
 * Relación: la crea AppContainer y la consume ParkingRepositoryImpl. Todos los
 * endpoints cuelgan de `/api/parking` sobre `BuildConfig.BASE_URL`.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.remote.api

import pe.parkeo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Contrato REST de consulta de estacionamientos.
 *
 * Todos los métodos son `GET`, públicos y `suspend`. La respuesta usa el sobre
 * `ApiResponseDto<T>`: un HTTP 200 puede traer `success = false` si el backend
 * decidió reportar un error lógico. `Response<T>` permite inspeccionar además el
 * código HTTP (`200`, `400`, `404`, `500`) y el cuerpo de error.
 */
interface ParkingApi {
    /**
     * Lista estacionamientos activos de forma paginada.
     *
     * `GET /api/parking?page=0&size=20`. La paginación de Spring es base 0: `page = 0`
     * es la primera página. Responde `200 OK` con `PageDto<ParkingLotDto>`; un valor
     * negativo o un tamaño inválido puede producir `400`. No incluye estacionamientos
     * inactivos en la vista pública.
     *
     * @param page índice de página, base 0; por defecto 0.
     * @param size cantidad de elementos por página; por defecto 20.
     * @return sobre con la página de resúmenes de estacionamiento.
     */
    @GET("api/parking")
    suspend fun getParkingLots(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponseDto<PageDto<ParkingLotDto>>>

    /**
     * Obtiene el detalle de un estacionamiento.
     *
     * `GET /api/parking/{id}`. Responde `200 OK` con `ParkingLotDetailDto` (servicios,
     * horarios, tarifas, calificaciones). Devuelve `404` si el identificador no existe
     * y `400` si el `id` no tiene formato numérico. Es una ruta pública.
     *
     * @param id identificador del estacionamiento.
     * @return sobre con el detalle completo.
     */
    @GET("api/parking/{id}")
    suspend fun getParkingLotDetail(@Path("id") id: Long): Response<ApiResponseDto<ParkingLotDetailDto>>

    /**
     * Busca estacionamientos cercanos a una coordenada.
     *
     * `GET /api/parking/nearby?lat=...&lon=...&radius=...`. `radius` se expresa en
     * kilómetros y por defecto es `5.0`; el backend ordena por distancia y rellena
     * `distanceKm`. Responde `200 OK` con una lista plana (sin paginación); `400` si
     * las coordenadas o el radio no son válidos.
     *
     * @param lat latitud en grados decimales (WGS-84), rango esperado -90 a 90.
     * @param lon longitud en grados decimales (WGS-84), rango esperado -180 a 180.
     * @param radius radio de búsqueda en kilómetros; por defecto 5.0.
     * @return sobre con la lista de estacionamientos cercanos.
     */
    @GET("api/parking/nearby")
    suspend fun getNearbyParkingLots(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("radius") radius: Double = 5.0
    ): Response<ApiResponseDto<List<ParkingLotDto>>>

    /**
     * Busca estacionamientos por texto libre.
     *
     * `GET /api/parking/search?q=...&page=0&size=20`. El backend busca coincidencias
     * en nombre o dirección y devuelve una página. Responde `200 OK` con
     * `PageDto<ParkingLotDto>` y `400` si falta el parámetro `q`. La paginación es
     * base 0.
     *
     * @param query texto de búsqueda; Retrofit lo codifica como parámetro de URL.
     * @param page índice de página, base 0; por defecto 0.
     * @param size tamaño de página; por defecto 20.
     * @return sobre con los resultados paginados.
     */
    @GET("api/parking/search")
    suspend fun searchParkingLots(
        @Query("q") query: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponseDto<PageDto<ParkingLotDto>>>

    /**
     * Consulta los espacios disponibles de un estacionamiento.
     *
     * `GET /api/parking/{id}/availability?vehicleTypeId=...`. `vehicleTypeId` es
     * opcional: si se envía, filtra por tipo de vehículo; si se omite, no se agrega a
     * la URL. Responde `200 OK` con la lista de `ParkingSpaceDto` disponibles y `404`
     * si el estacionamiento no existe. En el backend comparte implementación con
     * `/spaces/available`, por lo que ambas rutas devuelven lo mismo.
     *
     * @param id identificador del estacionamiento.
     * @param vehicleTypeId filtro opcional por tipo de vehículo; `null` significa todos.
     * @return sobre con los espacios disponibles.
     */
    @GET("api/parking/{id}/availability")
    suspend fun getAvailability(
        @Path("id") id: Long,
        @Query("vehicleTypeId") vehicleTypeId: Long? = null
    ): Response<ApiResponseDto<List<ParkingSpaceDto>>>

    /**
     * Alias de disponibilidad con nombre orientado a espacios.
     *
     * `GET /api/parking/{id}/spaces/available?vehicleTypeId=...`. En el backend ambas
     * rutas están mapeadas al mismo método, así que el contrato, los filtros y los
     * códigos (`200`, `404`) son idénticos a [getAvailability]. Se conserva para que
     * distintas pantallas expresen su intención sin duplicar la URL.
     *
     * @param id identificador del estacionamiento.
     * @param vehicleTypeId filtro opcional por tipo de vehículo; `null` significa todos.
     * @return sobre con los espacios disponibles.
     */
    @GET("api/parking/{id}/spaces/available")
    suspend fun getAvailableSpaces(
        @Path("id") id: Long,
        @Query("vehicleTypeId") vehicleTypeId: Long? = null
    ): Response<ApiResponseDto<List<ParkingSpaceDto>>>
}
