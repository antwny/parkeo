package pe.parkeo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParkingLotDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val address: String,
    val district: String? = null,
    val city: String? = null,
    val latitude: Double,
    val longitude: Double,
    val phone: String? = null,
    @SerialName("totalCapacity") val totalCapacity: Int,
    @SerialName("availableSpaces") val availableSpaces: Int = 0,
    @SerialName("isActive") val isActive: Boolean = true,
    @SerialName("isOpen") val isOpen: Boolean = true,
    @SerialName("imageUrl") val imageUrl: String? = null,
    val distance: Double? = null,
    val services: List<String> = emptyList(),
    val rating: Double? = null,
    @SerialName("ratingCount") val ratingCount: Int = 0
)

@Serializable
data class ParkingLotDetailDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    val address: String,
    val district: String? = null,
    val city: String? = null,
    val latitude: Double,
    val longitude: Double,
    val phone: String? = null,
    val email: String? = null,
    @SerialName("totalCapacity") val totalCapacity: Int,
    @SerialName("availableSpaces") val availableSpaces: Int = 0,
    @SerialName("isActive") val isActive: Boolean = true,
    @SerialName("isOpen") val isOpen: Boolean = true,
    @SerialName("imageUrl") val imageUrl: String? = null,
    val services: List<ParkingServiceDto> = emptyList(),
    val schedules: List<ScheduleDto> = emptyList(),
    val tariffs: List<TariffDto> = emptyList(),
    val rating: Double? = null,
    @SerialName("ratingCount") val ratingCount: Int = 0
)

@Serializable
data class ParkingServiceDto(
    val id: Long,
    val name: String,
    val icon: String? = null
)

@Serializable
data class ScheduleDto(
    val id: Long,
    @SerialName("dayOfWeek") val dayOfWeek: String,
    @SerialName("openTime") val openTime: String,
    @SerialName("closeTime") val closeTime: String,
    @SerialName("isOpen") val isOpen: Boolean
)

@Serializable
data class TariffDto(
    val id: Long,
    @SerialName("vehicleType") val vehicleType: String,
    @SerialName("tariffType") val tariffType: String,
    val price: Double,
    @SerialName("minimumMinutes") val minimumMinutes: Int = 30
)

@Serializable
data class AvailabilityDto(
    @SerialName("parkingLotId") val parkingLotId: Long,
    @SerialName("totalCapacity") val totalCapacity: Int,
    @SerialName("availableSpaces") val availableSpaces: Int,
    @SerialName("occupiedSpaces") val occupiedSpaces: Int,
    @SerialName("reservedSpaces") val reservedSpaces: Int
)

@Serializable
data class ParkingSpaceDto(
    val id: Long,
    @SerialName("spaceNumber") val spaceNumber: String,
    val status: String,
    @SerialName("vehicleType") val vehicleType: String = "",
    @SerialName("isAvailable") val isAvailable: Boolean = true
)
