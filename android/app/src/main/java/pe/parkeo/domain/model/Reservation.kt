// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Contrato o modelo del dominio; expresa conceptos/reglas de Parkeo sin acoplarse a HTTP ni a Android.
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

    /** Ejecuta la operación indicada por el nombre: display name. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    fun displayName(): String = when (this) {
        PENDING -> "Pendiente"
        CONFIRMED -> "Confirmada"
        ACTIVE -> "Activa"
        COMPLETED -> "Completada"
        CANCELLED -> "Cancelada"
        NO_SHOW -> "No se presentó"
    }
}
