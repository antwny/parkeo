/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/remote/api/VehicleApi.kt
 * Capa: datos/red (contrato Retrofit de vehículos del cliente).
 * Propósito: gestionar los vehículos del usuario autenticado y consultar el catálogo
 * de tipos de vehículo.
 * Conceptos clave: Retrofit, `@GET`, `@POST`, `@PUT`, `@DELETE`, `@Path`, `@Body`,
 * `Response<T>`, borrado lógico, placas, tipos de vehículo.
 * Flujo resumido: la pantalla de vehículos lista los del usuario, crea uno nuevo,
 * actualiza sus datos o lo desactiva; al reservar se usa `vehicleId` y `vehicleTypeId`.
 * Relación: la crea AppContainer y la consume VehicleRepositoryImpl. Todas las rutas
 * requieren autenticación; el backend filtra por propietario y devuelve 403/404 cuando
 * el vehículo no pertenece al usuario.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.remote.api

import pe.parkeo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Contrato REST de vehículos y catálogo de tipos.
 *
 * Los métodos son `suspend` y devuelven `Response<ApiResponseDto<T>>`. El token lo
 * inyecta `AuthInterceptor`; sin él, el backend responde `401`. El borrado es lógico:
 * `DELETE` desactiva (`isActive = false`) y no elimina la fila, para conservar el
 * historial de reservas. La placa se normaliza en el backend (mayúsculas y guion).
 */
interface VehicleApi {
    /**
     * Lista los vehículos activos del usuario autenticado.
     *
     * `GET /api/vehicles`. Responde `200 OK` con una lista plana de `VehicleDto`.
     * No hay paginación: el conjunto esperado es pequeño. Devuelve `401` sin sesión.
     * El backend filtra por el usuario del token, no por un id enviado por el cliente.
     *
     * @return sobre con los vehículos propios.
     */
    @GET("api/vehicles")
    suspend fun getMyVehicles(): Response<ApiResponseDto<List<VehicleDto>>>

    /**
     * Registra un vehículo para el usuario autenticado.
     *
     * `POST /api/vehicles`. El backend valida placa y tipo, normaliza la placa y
     * responde `201 Created` con `VehicleDto`. Devuelve `400` por datos inválidos,
     * `404` si el tipo de vehículo no existe, `409` si la placa ya está registrada y
     * `401` sin sesión.
     *
     * @param request datos del vehículo: placa obligatoria, marca/modelo/color
     *   opcionales y tipo obligatorio.
     * @return sobre con el vehículo creado.
     */
    @POST("api/vehicles")
    suspend fun createVehicle(@Body request: CreateVehicleRequestDto): Response<ApiResponseDto<VehicleDto>>

    /**
     * Actualiza un vehículo propio.
     *
     * `PUT /api/vehicles/{id}`. Semánticamente es una actualización completa, aunque el
     * backend solo aplica los campos no nulos del cuerpo. Se reutiliza
     * `CreateVehicleRequestDto`; la placa enviada no se actualiza en el backend (la
     * identidad del vehículo se conserva). Responde `200 OK` con `VehicleDto`, `400`
     * por validación, `404` si el vehículo no existe o no es del usuario y `401` sin
     * sesión.
     *
     * @param id identificador del vehículo.
     * @param request nuevos datos; los campos nulos se interpretan como “no cambiar”.
     * @return sobre con el vehículo actualizado.
     */
    @PUT("api/vehicles/{id}")
    suspend fun updateVehicle(
        @Path("id") id: Long,
        @Body request: CreateVehicleRequestDto
    ): Response<ApiResponseDto<VehicleDto>>

    /**
     * Desactiva un vehículo propio (borrado lógico).
     *
     * `DELETE /api/vehicles/{id}`. El backend marca `isActive = false` y responde
     * `200 OK` con `data = null`; no devuelve `204`. Si el vehículo no existe o no
     * pertenece al usuario responde `404`; sin sesión, `401`. El registro se conserva
     * para no romper reservas históricas.
     *
     * @param id identificador del vehículo.
     * @return sobre sin datos; el resultado se interpreta por HTTP y `success`.
     */
    @DELETE("api/vehicles/{id}")
    suspend fun deleteVehicle(@Path("id") id: Long): Response<ApiResponseDto<Unit>>

    /**
     * Lista los tipos de vehículo activos.
     *
     * `GET /api/vehicles/types`. Devuelve `200 OK` con `VehicleTypeDto` (por ejemplo,
     * automóvil, motocicleta, camioneta). Se usa para poblar selectores y para enviar
     * `vehicleTypeId` al crear un vehículo o consultar disponibilidad. Requiere sesión
     * porque cuelga de `/api/vehicles`; sin token responde `401`.
     *
     * @return sobre con el catálogo de tipos.
     */
    @GET("api/vehicles/types")
    suspend fun getVehicleTypes(): Response<ApiResponseDto<List<VehicleTypeDto>>>
}
