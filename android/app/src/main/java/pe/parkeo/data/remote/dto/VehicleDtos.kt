package pe.parkeo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VehicleDto(
    val id: Long,
    @SerialName("licensePlate") val licensePlate: String,
    val brand: String? = null,
    val model: String? = null,
    val color: String? = null,
    @SerialName("vehicleType") val vehicleType: VehicleTypeDto
)

@Serializable
data class VehicleTypeDto(
    val id: Long,
    val name: String,
    val icon: String? = null
)

@Serializable
data class CreateVehicleRequestDto(
    @SerialName("licensePlate") val licensePlate: String,
    val brand: String? = null,
    val model: String? = null,
    val color: String? = null,
    @SerialName("vehicleTypeId") val vehicleTypeId: Long
)
