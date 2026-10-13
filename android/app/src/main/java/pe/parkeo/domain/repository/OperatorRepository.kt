// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Contrato o modelo del dominio; expresa conceptos/reglas de Parkeo sin acoplarse a HTTP ni a Android.
package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.*
import pe.parkeo.util.Result

interface OperatorRepository {
    /** Consulta y devuelve: get my parking lots. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getMyParkingLots(): Result<List<ParkingLotDto>>
    suspend fun getSpaces(lotId: Long): Result<List<ParkingSpaceDto>>
    suspend fun updateSpaceStatus(spaceId: Long, status: String, notes: String? = null): Result<ParkingSpaceDto>
    /** Consulta y devuelve: get reservations. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getReservations(lotId: Long, status: String? = null): Result<List<ReservationDto>>
    suspend fun checkIn(reservationId: Long): Result<ReservationDto>
    suspend fun checkOut(reservationId: Long): Result<ReservationDto>
}
