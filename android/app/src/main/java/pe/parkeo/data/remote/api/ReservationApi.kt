/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/remote/api/ReservationApi.kt
 * Capa: datos/red (contrato Retrofit de reservas del cliente).
 * Propósito: listar, consultar, crear y cancelar reservas propias del usuario
 * autenticado.
 * Conceptos clave: Retrofit, `@GET`, `@POST`, `@PATCH`, `@Path`, `@Query`, `@Body`,
 * paginación, `Response<T>`, estados de reserva, fechas ISO-8601, autorización
 * horizontal por propietario.
 * Flujo resumido: el cliente elige estacionamiento, vehículo y horario; el repositorio
 * envía `CreateReservationRequestDto`; el backend valida disponibilidad, calcula el
 * precio y devuelve la reserva creada. Las pantallas de historial filtran por estado y
 * la cancelación solo se permite en estados iniciales.
 * Relación: la crea AppContainer y la consume ReservationRepositoryImpl. Todos los
 * endpoints requieren `Authorization` (cualquier usuario autenticado) y el backend
 * comprueba que la reserva pertenezca al usuario.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.remote.api

import pe.parkeo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Contrato REST de reservas desde la perspectiva del cliente.
 *
 * Los métodos son `suspend` y devuelven `Response<ApiResponseDto<T>>`. El token lo
 * añade `AuthInterceptor`; sin token el backend responde `401`. Los errores de
 * validación, conflicto de horario o falta de propietario llegan como `400`, `409`,
 * `403` o `404` con un sobre `success = false`. Las fechas viajan como texto
 * ISO-8601 local (`yyyy-MM-ddTHH:mm:ss`), sin zona horaria.
 */
interface ReservationApi {
    /**
     * Lista las reservas del usuario autenticado.
     *
     * `GET /api/reservations?status=...&page=0&size=20`. Solo devuelve reservas del
     * usuario del token (autorización horizontal). `status` es opcional y acepta
     * `PENDING`, `CONFIRMED`, `ACTIVE`, `COMPLETED`, `CANCELLED` o `NO_SHOW`; si se
     * omite, no se envía el parámetro. Responde `200 OK` con una página; `400` si el
     * estado no es válido y `401` sin sesión.
     *
     * @param status filtro opcional por estado; `null` significa todos.
     * @param page índice de página, base 0; por defecto 0.
     * @param size tamaño de página; por defecto 20.
     * @return sobre con la página de reservas propias.
     */
    @GET("api/reservations")
    suspend fun getMyReservations(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponseDto<PageDto<ReservationDto>>>

    /**
     * Obtiene el detalle de una reserva propia.
     *
     * `GET /api/reservations/{id}`. Responde `200 OK` con `ReservationDto`, que en el
     * detalle puede incluir coordenadas, `createdAt` y otros campos que la lista no
     * trae. Devuelve `404` si no existe y `403` si pertenece a otro usuario. El DTO de
     * Android comparte forma con el de lista; los campos extra que no estén declarados
     * se ignoran al deserializar.
     *
     * @param id identificador de la reserva.
     * @return sobre con la reserva solicitada.
     */
    @GET("api/reservations/{id}")
    suspend fun getReservationDetail(@Path("id") id: Long): Response<ApiResponseDto<ReservationDto>>

    /**
     * Crea una reserva para el usuario autenticado.
     *
     * `POST /api/reservations`. El backend valida el rango horario, que el vehículo
     * pertenezca al usuario, que no tenga otra reserva solapada, que el espacio esté
     * libre y que el estacionamiento esté activo y abierto; calcula el precio con las
     * tarifas y genera el código de confirmación. Responde `201 Created` con la reserva
     * en estado `PENDING`; `400` por validación, `404` si falta el vehículo o espacio,
     * `409` por solapamiento y `401` sin sesión.
     *
     * @param request datos de la reserva: estacionamiento, espacio opcional, vehículo,
     *   inicio, fin y notas opcionales.
     * @return sobre con la reserva creada.
     */
    @POST("api/reservations")
    suspend fun createReservation(@Body request: CreateReservationRequestDto): Response<ApiResponseDto<ReservationDto>>

    /**
     * Cancela una reserva en estado inicial.
     *
     * `PATCH /api/reservations/{id}/cancel`. El cuerpo es opcional en el backend; aquí
     * se envía por defecto `CancelReservationRequestDto()` (JSON `{}`), que equivale a
     * cancelar sin motivo. El backend solo permite cancelar reservas `PENDING` o
     * `CONFIRMED`; responde `200 OK` con la reserva en `CANCELLED`, `400` si el estado
     * no lo permite, `403`/`404` si no existe o no es del usuario. No se usa `DELETE`
     * porque la reserva se conserva para historial y auditoría.
     *
     * @param id identificador de la reserva.
     * @param body motivo opcional de cancelación; el valor por defecto no envía motivo.
     * @return sobre con la reserva actualizada a `CANCELLED`.
     */
    @PATCH("api/reservations/{id}/cancel")
    suspend fun cancelReservation(
        @Path("id") id: Long,
        @Body body: CancelReservationRequestDto = CancelReservationRequestDto()
    ): Response<ApiResponseDto<ReservationDto>>
}