package pe.parkeo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdminStatisticsDto(
    val totalParkingLots: Long = 0,
    val totalReservations: Long = 0,
    val pendingReservations: Long = 0,
    val confirmedReservations: Long = 0,
    val activeReservations: Long = 0,
    val completedReservations: Long = 0,
    val cancelledReservations: Long = 0
)

@Serializable
data class UpdateUserStatusRequestDto(
    @SerialName("isActive") val isActive: Boolean
)

@Serializable
data class UpdateParkingLotStatusRequestDto(
    @SerialName("isOpen") val isOpen: Boolean? = null,
    @SerialName("isActive") val isActive: Boolean? = null
)
