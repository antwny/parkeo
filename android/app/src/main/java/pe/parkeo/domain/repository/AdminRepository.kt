// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Contrato o modelo del dominio; expresa conceptos/reglas de Parkeo sin acoplarse a HTTP ni a Android.
package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.*
import pe.parkeo.util.Result

interface AdminRepository {
    /** Consulta y devuelve: get statistics. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getStatistics(): Result<AdminStatisticsDto>
    suspend fun getParkingLots(): Result<List<ParkingLotDto>>
    suspend fun updateParkingLotStatus(id: Long, isOpen: Boolean?, isActive: Boolean?): Result<ParkingLotDto>
    /** Asigna el recurso al destino indicado: assign operator. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun assignOperator(id: Long, operatorId: Long): Result<ParkingLotDto>
    suspend fun deactivateParkingLot(id: Long): Result<Unit>
    suspend fun getUsers(): Result<List<UserDto>>
    /** Actualiza el recurso o estado indicado: update user status. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun updateUserStatus(id: Long, isActive: Boolean): Result<UserDto>
}
