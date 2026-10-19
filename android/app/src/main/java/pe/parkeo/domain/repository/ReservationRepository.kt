/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/domain/repository/ReservationRepository.kt
 * Capa: dominio (contrato de repositorio).
 * Propósito: declarar las operaciones de reservas del usuario: listar las
 * propias, ver el detalle, crear una reserva y cancelarla. El backend es quien
 * valida horarios, evita conflictos, asigna espacio si hace falta y calcula el
 * precio.
 * Conceptos clave:
 *   - Contrato: los ViewModels dependen de esta interfaz y no conocen
 *     `ReservationApi` ni Retrofit; la implementación concreta es
 *     `ReservationRepositoryImpl`.
 *   - `suspend`: las llamadas hacen I/O y deben ejecutarse en una corrutina.
 *   - `Result`: evita que la UI tenga que capturar excepciones; los errores
 *     llegan como mensajes legibles.
 *   - DTO frente a modelo: el contrato devuelve `ReservationDto` (estado como
 *     `String`, campos del JSON) en lugar del modelo de dominio `Reservation`
 *     (estado tipado como `ReservationStatus`). El mapeo no está implementado
 *     en esta versión, aunque es el lugar natural para hacerlo.
 *   - Regla crítica: dos reservas no pueden solaparse para el mismo espacio ni
 *     para el mismo vehículo; esa validación vive en el backend y no debe
 *     replicarse como única defensa en el cliente.
 * Flujo: `ReservationViewModel` -> interfaz -> implementación -> `ReservationApi`
 * -> backend Spring/MySQL. `AppContainer` construye la implementación.
 * Relación: el ViewModel solo ve este contrato; `ReservationRepositoryImpl` y
 * `ReservationApi` resuelven la llamada, y el backend aplica las reglas.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.ReservationDto
import pe.parkeo.util.Result

/**
 * Contrato de reservas del usuario autenticado.
 *
 * La implementación concreta es `ReservationRepositoryImpl`. La identidad del
 * usuario no se pasa como parámetro: el backend la obtiene del token JWT que
 * añade `AuthInterceptor`, y solo devuelve reservas de ese usuario.
 */
interface ReservationRepository {

    /**
     * Lista las reservas del usuario autenticado.
     *
     * La implementación envía el filtro de estado y usa la paginación por
     * defecto del backend (página 0, tamaño 20); devuelve únicamente el
     * contenido de esa primera página.
     *
     * @param status Filtro opcional con el nombre técnico del estado
     *   (`PENDING`, `CONFIRMED`, `ACTIVE`, `COMPLETED`, `CANCELLED`,
     *   `NO_SHOW`). Si es `null`, se listan todas.
     * @return `Result.Success` con la lista de `ReservationDto` o lista vacía
     *   si no hay reservas; `Result.Error` con un mensaje legible si falla la
     *   autenticación, la red o la respuesta.
     */
    suspend fun getMyReservations(status: String?): Result<List<ReservationDto>>

    /**
     * Obtiene el detalle de una reserva propia.
     *
     * El detalle puede incluir coordenadas del estacionamiento y la fecha de
     * creación, campos que no siempre vienen en el listado.
     *
     * @param id Identificador de la reserva.
     * @return `Result.Success` con `ReservationDto`; `Result.Error` si la
     *   reserva no existe, no pertenece al usuario o falla la conexión.
     */
    suspend fun getReservationDetail(id: Long): Result<ReservationDto>

    /**
     * Crea una reserva para un vehículo en un estacionamiento y horario.
     *
     * Si se indica `parkingSpaceId`, el backend bloquea y valida ese espacio;
     * si se omite, asigna automáticamente un espacio libre compatible con el
     * tipo de vehículo. También verifica que el vehículo no tenga otra reserva
     * superpuesta, que la cochera esté activa/abierta y calcula el precio.
     *
     * @param parkingLotId Estacionamiento donde se reserva.
     * @param parkingSpaceId Espacio concreto elegido por el usuario; `null`
     *   delega la asignación al backend.
     * @param vehicleId Vehículo del usuario que ocupará el espacio.
     * @param startTime Inicio en formato ISO-8601 local (por ejemplo
     *   `2026-10-19T14:30:00`), tal como espera el `LocalDateTime` del
     *   backend.
     * @param endTime Fin en el mismo formato; debe ser posterior al inicio.
     * @param notes Texto libre opcional para el operador.
     * @return `Result.Success` con la reserva creada (estado inicial
     *   `PENDING` y precio calculado). `Result.Error` con el mensaje real del
     *   backend para conflictos (por ejemplo "El espacio ya está reservado
     *   para el período solicitado"), validaciones o fallos de red.
     */
    suspend fun createReservation(
        parkingLotId: Long,
        parkingSpaceId: Long? = null,
        vehicleId: Long,
        startTime: String,
        endTime: String,
        notes: String? = null
    ): Result<ReservationDto>

    /**
     * Cancela una reserva propia.
     *
     * El backend valida que la reserva pertenezca al usuario y que su estado
     * permita cancelación; luego la marca como `CANCELLED` y libera el espacio
     * si correspondía.
     *
     * @param id Identificador de la reserva a cancelar.
     * @param reason Motivo opcional enviado en el cuerpo de la petición; puede
     *   ser `null`.
     * @return `Result.Success` con la reserva actualizada o `Result.Error` con
     *   el motivo del rechazo (estado no cancelable, falta de permisos, red).
     */
    suspend fun cancelReservation(id: Long, reason: String? = null): Result<ReservationDto>
}