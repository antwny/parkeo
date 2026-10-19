/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/domain/repository/OperatorRepository.kt
 * Capa: dominio (contrato de repositorio).
 * Propósito: declarar las operaciones del rol operador: consultar los
 * estacionamientos que administra, ver y cambiar el estado de sus espacios,
 * revisar reservas y registrar ingreso/salida de vehículos.
 * Conceptos clave:
 *   - Contrato: `OperatorViewModel` depende de esta interfaz; la
 *     implementación concreta es `OperatorRepositoryImpl`, que usa
 *     `OperatorApi`.
 *   - `suspend`: las operaciones acceden a la red y se ejecutan en corrutinas.
 *   - `Result`: encapsula éxito o error con mensaje para la UI.
 *   - DTO frente a modelo: se devuelven DTO de red (`ParkingLotDto`,
 *     `ParkingSpaceDto`, `ReservationDto`); no hay mapeo a modelos de dominio.
 *   - Autorización horizontal: el backend verifica que el estacionamiento
 *     esté asignado al operador autenticado; no basta con tener rol OPERADOR.
 * Flujo: pantalla de operador -> ViewModel -> contrato -> implementación ->
 * Retrofit -> backend Spring -> MySQL. `AppContainer` inyecta la
 * implementación y el token JWT identifica al operador.
 * Relación: `OperatorViewModel` depende de este contrato;
 * `OperatorRepositoryImpl` usa `OperatorApi`; el backend aplica la autorización
 * horizontal y guarda los cambios en MySQL.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.*
import pe.parkeo.util.Result

/**
 * Contrato de operación de estacionamientos asignados.
 *
 * La implementación concreta es `OperatorRepositoryImpl`. Las consultas se
 * limitan a los estacionamientos vinculados al operador autenticado; el
 * backend aplica esa restricción aunque el cliente envíe otro identificador.
 */
interface OperatorRepository {
    /**
     * Lista los estacionamientos asignados al operador autenticado.
     *
     * @return `Result.Success` con la lista de `ParkingLotDto` o
     *   `Result.Error` si falla la consulta o la autenticación.
     */
    suspend fun getMyParkingLots(): Result<List<ParkingLotDto>>

    /**
     * Obtiene los espacios de un estacionamiento.
     *
     * Incluye el estado actual de cada espacio, que el operador usará para
     * gestionar la ocupación. El backend comprueba la asignación del operador.
     *
     * @param lotId Identificador del estacionamiento.
     * @return `Result.Success` con los `ParkingSpaceDto` o `Result.Error` si
     *   el operador no tiene acceso o falla la red.
     */
    suspend fun getSpaces(lotId: Long): Result<List<ParkingSpaceDto>>

    /**
     * Cambia manualmente el estado de un espacio.
     *
     * Los valores esperados por el backend son `AVAILABLE` (libre),
     * `OCCUPIED` (ocupado), `RESERVED` (apartado) y `MAINTENANCE` (bloqueado
     * por mantenimiento). El backend valida la transición y la pertenencia del
     * espacio al operador.
     *
     * @param spaceId Identificador del espacio.
     * @param status Nuevo estado técnico, en mayúsculas y sin traducción.
     * @param notes Nota opcional para explicar el cambio (por ejemplo, motivo
     *   de mantenimiento); puede ser `null`.
     * @return `Result.Success` con el espacio actualizado o `Result.Error` si
     *   el estado no es válido, no tiene permiso o falla la red.
     */
    suspend fun updateSpaceStatus(spaceId: Long, status: String, notes: String? = null): Result<ParkingSpaceDto>

    /**
     * Lista las reservas de un estacionamiento.
     *
     * La implementación usa paginación por defecto (página 0, tamaño 50) y
     * devuelve solo la primera página de contenido.
     *
     * @param lotId Identificador del estacionamiento.
     * @param status Filtro opcional por estado técnico
     *   (`PENDING`, `CONFIRMED`, `ACTIVE`, `COMPLETED`, `CANCELLED`,
     *   `NO_SHOW`); `null` lista todas.
     * @return `Result.Success` con las reservas o `Result.Error` si el
     *   operador no tiene acceso o falla la consulta.
     */
    suspend fun getReservations(lotId: Long, status: String? = null): Result<List<ReservationDto>>

    /**
     * Registra el ingreso de un vehículo (check-in).
     *
     * El backend exige que la reserva esté `PENDING` o `CONFIRMED`, que el
     * operador administre el estacionamiento y cambia la reserva a `ACTIVE`;
     * además marca el espacio como `OCCUPIED` y ajusta la disponibilidad.
     *
     * @param reservationId Identificador de la reserva.
     * @return `Result.Success` con la reserva actualizada o `Result.Error` si
     *   el estado no permite el ingreso, no hay permiso o falla la red.
     */
    suspend fun checkIn(reservationId: Long): Result<ReservationDto>

    /**
     * Registra la salida de un vehículo (check-out).
     *
     * El backend exige que la reserva esté `ACTIVE`, la marca como
     * `COMPLETED`, libera el espacio a `AVAILABLE` y ajusta la disponibilidad
     * del estacionamiento.
     *
     * @param reservationId Identificador de la reserva.
     * @return `Result.Success` con la reserva actualizada o `Result.Error` si
     *   la reserva no está activa, no hay permiso o falla la red.
     */
    suspend fun checkOut(reservationId: Long): Result<ReservationDto>
}
