package pe.parkeo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReservationDto(
    val id: Long,
    @SerialName("parkingLotName") val parkingLotName: String,
    @SerialName("parkingLotAddress") val parkingLotAddress: String,
    @SerialName("spaceNumber") val spaceNumber: String,
    @SerialName("vehicleLicensePlate") val vehicleLicensePlate: String,
    @SerialName("startTime") val startTime: String,
    @SerialName("endTime") val endTime: String,
    val status: String,
    @SerialName("totalPrice") val totalPrice: Double? = null
)

@Serializable
data class CreateReservationRequestDto(
    @SerialName("parkingSpaceId") val parkingSpaceId: Long,
    @SerialName("vehicleId") val vehicleId: Long,
    @SerialName("startTime") val startTime: String,
    @SerialName("endTime") val endTime: String
)
