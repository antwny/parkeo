/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/remote/api/OperatorApi.kt
 * Capa: datos/red (contrato Retrofit del operador de cochera).
 * Propósito: consultar las cocheras asignadas, ver y cambiar el estado de sus
 * espacios, listar reservas del estacionamiento y registrar ingreso/salida.
 * Conceptos clave: Retrofit, `@GET`, `@PATCH`, `@Path`, `@Query`, `@Body`,
 * `Response<T>`, `PageDto<T>`, RBAC (`ROLE_OPERATOR`/`ROLE_ADMIN`), estados de
 * espacio y de reserva, control de acceso por cochera asignada.
 * Flujo resumido: el operador abre su cochera, ve los espacios, los marca ocupados o
 * disponibles y gestiona las reservas con check-in (llegada) y check-out (salida).
 * Relación: la crea AppContainer y la consume OperatorRepositoryImpl. Todo
 * el prefijo `/api/operator/` exige `ROLE_OPERATOR` o `ROLE_ADMIN`; además el backend valida
 * que el operador tenga asignada la cochera concreta. Sin token: `401`; sin permiso:
 * `403`.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.remote.api

import pe.parkeo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Contrato REST de operación de cocheras.
 *
 * Los métodos son `suspend` y devuelven `Response<ApiResponseDto<T>>`. La autorización
 * horizontal (que la cochera pertenezca al operador) se valida en el backend; no basta
 * con ocultar botones. Los cambios de estado usan `PATCH` porque modifican una parte
 * del recurso, no lo reemplazan por completo.
 */
interface OperatorApi {
    /**
     * Lista las cocheras asignadas al operador autenticado.
     *
     * `GET /api/operator/parking-lots`. Devuelve `200 OK` con una lista plana. Si el
     * usuario es administrador, el backend devuelve las cocheras activas; si es
     * operador, solo las que tiene asignadas. Sin token: `401`; sin rol: `403`.
     *
     * @return sobre con las cocheras visibles para el usuario.
     */
    @GET("api/operator/parking-lots")
    suspend fun getMyParkingLots(): Response<ApiResponseDto<List<ParkingLotDto>>>

    /**
     * Lista todos los espacios de una cochera asignada.
     *
     * `GET /api/operator/parking-lots/{id}/spaces`. Devuelve `200 OK` con
     * `ParkingSpaceDto` (estado, número, tipo de vehículo). El backend comprueba que
     * el operador tenga acceso a esa cochera; si no, responde `403`. `404` si la
     * cochera no existe.
     *
     * @param id identificador de la cochera.
     * @return sobre con los espacios de la cochera.
     */
    @GET("api/operator/parking-lots/{id}/spaces")
    suspend fun getSpaces(
        @Path("id") id: Long
    ): Response<ApiResponseDto<List<ParkingSpaceDto>>>

    /**
     * Cambia el estado de un espacio.
     *
     * `PATCH /api/operator/spaces/{spaceId}/status`. `status` debe ser uno de
     * `AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE` o `INACTIVE`; `notes` es
     * opcional y se usa para auditoría. Responde `200 OK` con el espacio actualizado,
     * `400` si el estado no es válido, `403` si el espacio no pertenece a una cochera
     * asignada y `404` si no existe.
     *
     * @param spaceId identificador del espacio.
     * @param request nuevo estado y nota opcional.
     * @return sobre con el espacio actualizado.
     */
    @PATCH("api/operator/spaces/{spaceId}/status")
    suspend fun updateSpaceStatus(
        @Path("spaceId") spaceId: Long,
        @Body request: UpdateSpaceStatusRequestDto
    ): Response<ApiResponseDto<ParkingSpaceDto>>

    /**
     * Lista las reservas de una cochera.
     *
     * `GET /api/operator/reservations?parkingLotId=...&status=...&page=0&size=50`.
     * `parkingLotId` es obligatorio para el backend: si falta, responde `400`; el
     * operador debe tener acceso a esa cochera o recibe `403`. `status` es opcional y
     * acepta los estados de reserva. Responde `200 OK` con `PageDto<ReservationDto>`
     * ordenado por fecha de creación descendente.
     *
     * @param parkingLotId cochera cuyas reservas se consultan; obligatorio.
     * @param status filtro opcional por estado; `null` significa todos.
     * @param page índice de página, base 0; por defecto 0.
     * @param size tamaño de página; por defecto 50.
     * @return sobre con la página de reservas de la cochera.
     */
    @GET("api/operator/reservations")
    suspend fun getReservations(
        @Query("parkingLotId") parkingLotId: Long,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<ApiResponseDto<PageDto<ReservationDto>>>

    /**
     * Registra el ingreso del cliente (check-in).
     *
     * `PATCH /api/operator/reservations/{id}/check-in`. El backend solo acepta reservas
     * `PENDING` o `CONFIRMED`; las pasa a `ACTIVE`, marca el espacio como `OCCUPIED` y
     * registra la hora de ingreso. Responde `200 OK` con la reserva, `400` si el estado
     * no lo permite, `403` si la reserva no es de una cochera asignada y `404` si no
     * existe. No envía cuerpo.
     *
     * @param id identificador de la reserva.
     * @return sobre con la reserva en estado `ACTIVE`.
     */
    @PATCH("api/operator/reservations/{id}/check-in")
    suspend fun checkInReservation(
        @Path("id") id: Long
    ): Response<ApiResponseDto<ReservationDto>>

    /**
     * Registra la salida del cliente (check-out).
     *
     * `PATCH /api/operator/reservations/{id}/check-out`. Solo acepta reservas `ACTIVE`;
     * las pasa a `COMPLETED`, libera el espacio (`AVAILABLE`) y registra la hora de
     * salida. Responde `200 OK` con la reserva, `400` si no está en `ACTIVE`, `403` si
     * no pertenece a una cochera asignada y `404` si no existe. No envía cuerpo.
     *
     * @param id identificador de la reserva.
     * @return sobre con la reserva en estado `COMPLETED`.
     */
    @PATCH("api/operator/reservations/{id}/check-out")
    suspend fun checkOutReservation(
        @Path("id") id: Long
    ): Response<ApiResponseDto<ReservationDto>>
}
