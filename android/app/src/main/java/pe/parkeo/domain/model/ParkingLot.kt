package pe.parkeo.domain.model

data class ParkingLot(
    val id: Long,
    val name: String,
    val address: String,
    val district: String?,
    val latitude: Double,
    val longitude: Double,
    val totalCapacity: Int,
    val availableSpaces: Int,
    val isOpen: Boolean,
    val imageUrl: String?,
    val services: List<String>,
    val distance: Double?,
    val rating: Double?
) {
    val isAvailable: Boolean get() = availableSpaces > 0
    val occupancyPercent: Float get() = if (totalCapacity > 0)
        (totalCapacity - availableSpaces).toFloat() / totalCapacity else 0f
}
