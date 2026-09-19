package pe.parkeo.domain.model

data class Vehicle(
    val id: Long,
    val licensePlate: String,
    val brand: String?,
    val model: String?,
    val color: String?,
    val vehicleType: VehicleType
)

data class VehicleType(
    val id: Long,
    val name: String
)
