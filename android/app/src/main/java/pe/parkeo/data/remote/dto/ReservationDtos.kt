package pe.parkeo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReservationDto(
    val id: Long = 0,
    @SerialName("confirmationCode") val confirmationCode: String? = null,
    @SerialName("parkingLotName") val parkingLotName: String = "",
    @SerialName("parkingLotAddress") val parkingLotAddress: String = "",
    @SerialName("spaceNumber") val spaceNumber: String = "",
    @SerialName("vehicleLicensePlate") val vehicleLicensePlate: String = "",
    @SerialName("startTime") val startTime: String = "",
    @SerialName("endTime") val endTime: String = "",
    val status: String = "PENDING",
    @SerialName("totalAmount") val totalAmount: Double? = null,
    @SerialName("totalPrice") val totalPriceVal: Double? = null,
    val currency: String? = null,
    @SerialName("parkingLotId") val parkingLotId: Long? = null,
    @SerialName("parkingSpaceId") val parkingSpaceId: Long? = null,
    @SerialName("vehicleId") val vehicleId: Long? = null,
    @SerialName("createdAt") val createdAt: String? = null
) {
    val totalPrice: Double? get() = totalAmount ?: totalPriceVal
}

@Serializable
data class CreateReservationRequestDto(
    @SerialName("parkingSpaceId") val parkingSpaceId: Long,
    @SerialName("vehicleId") val vehicleId: Long,
    @SerialName("startTime") val startTime: String,
    @SerialName("endTime") val endTime: String
)
