// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Implementación del módulo Parkeo identificado por el paquete y nombre de esta clase.
package pe.parkeo.ui.navigation

object NavRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val HOME = "home"
    const val PARKING_DETAIL = "parking/{parkingId}"

    const val RESERVATION_CREATE = "reservation/create/{parkingId}"
    const val RESERVATIONS = "reservations"

    const val VEHICLES = "vehicles"
    const val PROFILE = "profile"

    /** Ejecuta la operación indicada por el nombre: parking detail. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    fun parkingDetail(id: Long) = "parking/$id"
    fun reservationCreate(parkingId: Long) = "reservation/create/$parkingId"
}
