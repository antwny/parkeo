// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Contrato o modelo del dominio; expresa conceptos/reglas de Parkeo sin acoplarse a HTTP ni a Android.
package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.*
import pe.parkeo.util.Result

interface VehicleRepository {
    /** Consulta y devuelve: get my vehicles. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun getMyVehicles(): Result<List<VehicleDto>>
    suspend fun createVehicle(licensePlate: String, brand: String?, model: String?, color: String?, vehicleTypeId: Long): Result<VehicleDto>
    suspend fun updateVehicle(id: Long, licensePlate: String, brand: String?, model: String?, color: String?, vehicleTypeId: Long): Result<VehicleDto>
    /** Elimina o desactiva el recurso indicado: delete vehicle. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    suspend fun deleteVehicle(id: Long): Result<Unit>
    suspend fun getVehicleTypes(): Result<List<VehicleTypeDto>>
}
