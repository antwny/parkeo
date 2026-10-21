package pe.parkeo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateSpaceStatusRequestDto(
    val status: String,
    val notes: String? = null
)

@Serializable
data class CreateWalkInRequestDto(
    @SerialName("parkingLotId") val parkingLotId: Long,
    @SerialName("vehicleTypeId") val vehicleTypeId: Long,
    @SerialName("parkingSpaceId") val parkingSpaceId: Long? = null,
    val notes: String? = null
)
