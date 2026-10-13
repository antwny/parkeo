// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Contrato o modelo del dominio; expresa conceptos/reglas de Parkeo sin acoplarse a HTTP ni a Android.
package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.ReservationDto
import pe.parkeo.util.Result

interface ReservationRepository {

    /** Consulta y devuelve: get my reservations. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getMyReservations(status: String?): Result<List<ReservationDto>>

    suspend fun getReservationDetail(id: Long): Result<ReservationDto>

    /** Crea y devuelve: create reservation. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun createReservation(
        parkingLotId: Long,
        parkingSpaceId: Long? = null,
        vehicleId: Long,
        startTime: String,
        endTime: String,
        notes: String? = null
    ): Result<ReservationDto>

    /** Valida y cancela la reserva indicada: cancel reservation. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun cancelReservation(id: Long, reason: String? = null): Result<ReservationDto>
}