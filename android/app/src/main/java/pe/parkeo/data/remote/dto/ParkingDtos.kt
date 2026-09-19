package pe.parkeo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParkingLotDto(
    val id: Long = 0,
    val name: String = "",
    val description: String? = null,
    val address: String = "",
    val district: String? = null,
    val city: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val phone: String? = null,
    @SerialName("totalSpaces") val totalSpaces: Int = 0,
    @SerialName("totalCapacity") val totalCapacityVal: Int? = null,
    @SerialName("availableSpaces") val availableSpaces: Int = 0,
    @SerialName("isActive") val isActive: Boolean = true,
    @SerialName("isOpen") val isOpen: Boolean = true,
    @SerialName("imageUrl") val imageUrl: String? = null,
    @SerialName("distanceKm") val distanceKm: Double? = null,
    @SerialName("distance") val distanceVal: Double? = null,
    val services: List<String> = emptyList(),
    @SerialName("averageRating") val averageRating: Double? = null,
    @SerialName("rating") val ratingVal: Double? = null,
    @SerialName("ratingCount") val ratingCount: Int = 0
) {
    val totalCapacity: Int get() = if (totalSpaces > 0) totalSpaces else (totalCapacityVal ?: 0)
    val distance: Double? get() = distanceKm ?: distanceVal
    val rating: Double? get() = averageRating ?: ratingVal
}

@Serializable
data class ParkingLotDetailDto(
    val id: Long = 0,
    val name: String = "",
    val description: String? = null,
    val address: String = "",
    val district: String? = null,
    val city: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val phone: String? = null,
    val email: String? = null,
    @SerialName("totalSpaces") val totalSpaces: Int = 0,
    @SerialName("totalCapacity") val totalCapacityVal: Int? = null,
    @SerialName("availableSpaces") val availableSpaces: Int = 0,
    @SerialName("isActive") val isActive: Boolean = true,
    @SerialName("isOpen") val isOpen: Boolean = true,
    @SerialName("imageUrl") val imageUrl: String? = null,
    val services: List<ParkingServiceDto> = emptyList(),
    val schedules: List<ScheduleDto> = emptyList(),
    val tariffs: List<TariffDto> = emptyList(),
    @SerialName("averageRating") val averageRating: Double? = null,
    @SerialName("rating") val ratingVal: Double? = null,
    @SerialName("totalRatings") val totalRatings: Long? = null,
    @SerialName("ratingCount") val ratingCountVal: Int? = null
) {
    val totalCapacity: Int get() = if (totalSpaces > 0) totalSpaces else (totalCapacityVal ?: 0)
    val rating: Double? get() = averageRating ?: ratingVal
    val ratingCount: Int get() = totalRatings?.toInt() ?: (ratingCountVal ?: 0)
}

@Serializable
data class ParkingServiceDto(
    val id: Long? = null,
    val name: String = "",
    val icon: String? = null
)

@Serializable
data class ScheduleDto(
    val id: Long? = null,
    @SerialName("dayOfWeek") val dayOfWeek: String = "",
    @SerialName("openTime") val openTime: String = "",
    @SerialName("closeTime") val closeTime: String = "",
    @SerialName("isClosed") val isClosed: Boolean = false,
    @SerialName("isOpen") val isOpenVal: Boolean? = null
) {
    val isOpen: Boolean get() = isOpenVal ?: !isClosed
}

@Serializable
data class TariffDto(
    val id: Long = 0,
    @SerialName("vehicleTypeName") val vehicleTypeName: String? = null,
    @SerialName("vehicleType") val vehicleTypeVal: String? = null,
    @SerialName("tariffType") val tariffType: String = "",
    val price: Double = 0.0,
    @SerialName("minimumMinutes") val minimumMinutes: Int = 30
) {
    val vehicleType: String get() = vehicleTypeName ?: (vehicleTypeVal ?: "")
}

@Serializable
data class AvailabilityDto(
    @SerialName("parkingLotId") val parkingLotId: Long = 0,
    @SerialName("totalCapacity") val totalCapacity: Int = 0,
    @SerialName("availableSpaces") val availableSpaces: Int = 0,
    @SerialName("occupiedSpaces") val occupiedSpaces: Int = 0,
    @SerialName("reservedSpaces") val reservedSpaces: Int = 0
)

@Serializable
data class ParkingSpaceDto(
    val id: Long = 0,
    @SerialName("spaceNumber") val spaceNumber: String = "",
    val status: String = "AVAILABLE",
    @SerialName("vehicleTypeName") val vehicleTypeName: String? = null,
    @SerialName("vehicleType") val vehicleTypeVal: String? = null,
    @SerialName("isAvailable") val isAvailableVal: Boolean? = null
) {
    val vehicleType: String get() = vehicleTypeName ?: (vehicleTypeVal ?: "")
    val isAvailable: Boolean get() = isAvailableVal ?: (status == "AVAILABLE")
}
