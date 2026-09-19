package pe.parkeo.domain.model

data class Reservation(
    val id: Long,
    val parkingLotName: String,
    val parkingLotAddress: String,
    val spaceNumber: String,
    val vehicleLicensePlate: String,
    val startTime: String,
    val endTime: String,
    val status: ReservationStatus,
    val totalPrice: Double?
)

enum class ReservationStatus {
    PENDING, CONFIRMED, ACTIVE, COMPLETED, CANCELLED, NO_SHOW;

    fun displayName(): String = when (this) {
        PENDING -> "Pendiente"
        CONFIRMED -> "Confirmada"
        ACTIVE -> "Activa"
        COMPLETED -> "Completada"
        CANCELLED -> "Cancelada"
        NO_SHOW -> "No se presentó"
    }
}
