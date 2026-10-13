// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Contrato o modelo del dominio; expresa conceptos/reglas de Parkeo sin acoplarse a HTTP ni a Android.
package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.*
import pe.parkeo.util.Result

interface ParkingRepository {
    /** Consulta y devuelve: get parking lots. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getParkingLots(): Result<List<ParkingLotDto>>
    suspend fun getParkingLotDetail(id: Long): Result<ParkingLotDetailDto>
    suspend fun getNearbyParkingLots(lat: Double, lon: Double, radius: Double = 5.0): Result<List<ParkingLotDto>>
    /** Ejecuta la operación indicada por el nombre: search parking lots. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun searchParkingLots(query: String): Result<List<ParkingLotDto>>
    suspend fun getAvailability(id: Long, date: String?): Result<AvailabilityDto>
    suspend fun getAvailableSpaces(id: Long, startTime: String, endTime: String, vehicleTypeId: Long?): Result<List<ParkingSpaceDto>>
}
