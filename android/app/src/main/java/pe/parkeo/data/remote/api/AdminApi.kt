/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/remote/api/AdminApi.kt
 * Capa: datos/red (contrato Retrofit del panel administrativo).
 * Propósito: exponer estadísticas globales, gestión de estacionamientos, asignación
 * de operadores y administración de usuarios.
 * Conceptos clave: Retrofit, `@GET`, `@PUT`, `@DELETE`, `@Path`, `@Query`, `@Body`,
 * `Response<T>`, `PageDto<T>`, RBAC, `ROLE_ADMIN`, borrado lógico.
 * Flujo resumido: el administrador consulta el dashboard, activa/desactiva cocheras,
 * las abre o cierra, les asigna un operador y habilita/deshabilita cuentas. Cada
 * operación devuelve el recurso actualizado o un sobre sin datos para el borrado.
 * Relación: la crea AppContainer y la consume AdminRepositoryImpl. Todo el prefijo
 * `/api/admin/` exige `ROLE_ADMIN`; sin token el backend responde `401` y con otro rol `403`.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.remote.api

import pe.parkeo.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Contrato REST del panel de administración.
 *
 * Todos los métodos son `suspend` y devuelven `Response<ApiResponseDto<T>>`. El control
 * de acceso es del backend: `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`. Ocultar una
 * acción en la UI no protege el endpoint; la verificación real ocurre aquí. El token
 * se añade con `AuthInterceptor` y el backend responde `401` sin sesión o `403` con un
 * rol insuficiente.
 */
interface AdminApi {
    /**
     * Obtiene estadísticas globales del sistema.
     *
     * `GET /api/admin/statistics`. El backend construye un mapa con conteos de
     * estacionamientos y reservas por estado; Kotlin lo deserializa en
     * `AdminStatisticsDto` (los campos ausentes toman su valor por defecto). Responde
     * `200 OK`. No hay paginación.
     *
     * @return sobre con los conteos del dashboard.
     */
    @GET("api/admin/statistics")
    suspend fun getStatistics(): Response<ApiResponseDto<AdminStatisticsDto>>

    /**
     * Lista todos los estacionamientos, incluidos los inactivos.
     *
     * `GET /api/admin/parking-lots?page=0&size=50`. A diferencia de la ruta pública,
     * el administrador ve el universo completo. La paginación es base 0 y el tamaño
     * por defecto es 50. Responde `200 OK` con `PageDto<ParkingLotDto>`.
     *
     * @param page índice de página, base 0; por defecto 0.
     * @param size tamaño de página; por defecto 50.
     * @return sobre con la página de estacionamientos.
     */
    @GET("api/admin/parking-lots")
    suspend fun getParkingLots(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<ApiResponseDto<PageDto<ParkingLotDto>>>

    /**
     * Cambia el estado operativo o de actividad de un estacionamiento.
     *
     * `PUT /api/admin/parking-lots/{id}/status`. El cuerpo puede traer `isOpen` y
     * `isActive`; ambos son opcionales y solo se aplican los enviados. `isOpen`
     * controla si acepta operaciones ahora; `isActive` controla si existe para el
     * público. Responde `200 OK` con el `ParkingLotDto` actualizado, `400` si el cuerpo
     * es inválido y `404` si no existe.
     *
     * @param id identificador del estacionamiento.
     * @param body flags opcionales de apertura y actividad.
     * @return sobre con el estacionamiento actualizado.
     */
    @PUT("api/admin/parking-lots/{id}/status")
    suspend fun updateParkingLotStatus(
        @Path("id") id: Long,
        @Body body: UpdateParkingLotStatusRequestDto
    ): Response<ApiResponseDto<ParkingLotDto>>

    /**
     * Asigna un operador a un estacionamiento.
     *
     * `PUT /api/admin/parking-lots/{id}/operator`. El cuerpo lleva `operatorId`; el
     * backend valida que el usuario exista y lo asigna. En la implementación actual no
     * comprueba que ese usuario tenga rol de operador, así que la UI debe restringir el
     * selector. Responde `200 OK` con el estacionamiento actualizado y `404` si el
     * usuario o la cochera no existen. Reemplaza la asignación anterior.
     *
     * @param id identificador del estacionamiento.
     * @param body identificador del usuario operador que se asignará.
     * @return sobre con el estacionamiento y su operador.
     */
    @PUT("api/admin/parking-lots/{id}/operator")
    suspend fun assignOperator(
        @Path("id") id: Long,
        @Body body: AssignOperatorRequestDto
    ): Response<ApiResponseDto<ParkingLotDto>>

    /**
     * Desactiva un estacionamiento (borrado lógico).
     *
     * `DELETE /api/admin/parking-lots/{id}`. El backend marca el estacionamiento como
     * inactivo y responde `200 OK` con `data = null`; no borra la fila ni el historial.
     * Devuelve `404` si no existe. La ruta pública deja de listarlo.
     *
     * @param id identificador del estacionamiento.
     * @return sobre sin datos; el resultado se interpreta por HTTP y `success`.
     */
    @DELETE("api/admin/parking-lots/{id}")
    suspend fun deactivateParkingLot(
        @Path("id") id: Long
    ): Response<ApiResponseDto<Unit>>

    /**
     * Lista todas las cuentas de usuario.
     *
     * `GET /api/admin/users`. Devuelve `200 OK` con una lista plana de `UserDto`
     * (incluye rol, estado y datos de contacto). No hay paginación. La UI filtra por
     * rol o estado en memoria.
     *
     * @return sobre con todas las cuentas.
     */
    @GET("api/admin/users")
    suspend fun getUsers(): Response<ApiResponseDto<List<UserDto>>>

    /**
     * Activa o desactiva una cuenta de usuario.
     *
     * `PUT /api/admin/users/{id}/status`. El cuerpo exige `isActive`. Responde
     * `200 OK` con el `UserDto` actualizado, `400` si falta el campo y `404` si el
     * usuario no existe. Desactivar una cuenta no borra sus datos ni sus reservas.
     *
     * @param id identificador del usuario.
     * @param body nuevo estado de actividad de la cuenta.
     * @return sobre con el usuario actualizado.
     */
    @PUT("api/admin/users/{id}/status")
    suspend fun updateUserStatus(
        @Path("id") id: Long,
        @Body body: UpdateUserStatusRequestDto
    ): Response<ApiResponseDto<UserDto>>
}
