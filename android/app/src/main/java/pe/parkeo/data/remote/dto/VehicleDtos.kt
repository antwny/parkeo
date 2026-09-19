package pe.parkeo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VehicleDto(
    val id: Long = 0,
    @SerialName("licensePlate") val licensePlate: String = "",
    val brand: String? = null,
    val model: String? = null,
    val color: String? = null,
    val year: Int? = null,
    @SerialName("isActive") val isActive: Boolean = true,
    @SerialName("vehicleTypeId") val vehicleTypeId: Long? = null,
    @SerialName("vehicleTypeName") val vehicleTypeName: String? = null,
    @SerialName("vehicleType") val vehicleTypeObj: VehicleTypeDto? = null
) {
    val vehicleType: VehicleTypeDto
        get() = vehicleTypeObj ?: VehicleTypeDto(
            id = vehicleTypeId ?: 0L,
            name = vehicleTypeName ?: "Vehículo"
        )
}

@Serializable
data class VehicleTypeDto(
    val id: Long = 0,
    val name: String = "",
    val description: String? = null,
    val icon: String? = null,
    @SerialName("isActive") val isActive: Boolean = true
)

@Serializable
data class CreateVehicleRequestDto(
    @SerialName("licensePlate") val licensePlate: String,
    val brand: String? = null,
    val model: String? = null,
    val color: String? = null,
    @SerialName("vehicleTypeId") val vehicleTypeId: Long
)
