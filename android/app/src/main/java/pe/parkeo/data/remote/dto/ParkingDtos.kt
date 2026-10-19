/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/remote/dto/ParkingDtos.kt
 * Capa: datos/red · contrato (DTO de estacionamientos).
 * Propósito: modelar el JSON de listados, detalle, servicios, horarios, tarifas y
 * espacios de un estacionamiento, incluyendo alias de compatibilidad entre versiones
 * del backend.
 * Contrato JSON: respuestas como `{"success":true,"data":{"content":[...]}}`,
 * `{"data":{"services":[...],"schedules":[...],"tariffs":[...]}}` o
 * `{"data":[{"id":1,"spaceNumber":"A-01","status":"AVAILABLE"}]}`.
 * Endpoints: `GET /api/parking`, `/api/parking/{id}`,
 * `/api/parking/nearby`, `/api/parking/search`,
 * `/api/parking/{id}/availability` y `/api/parking/{id}/spaces/available`.
 * Conceptos clave: Kotlin Serialization, `@SerialName`, `@Serializable`, valores por
 * defecto, campos nulos, coordenadas WGS-84, distancias en kilómetros, calificaciones
 * de 0 a 5, estados de espacio, tarifas, `ignoreUnknownKeys`.
 * Flujo resumido: ParkingApi deserializa estos DTO; ParkingRepositoryImpl toma
 * `data.content` o `data` y la UI los muestra. Algunos campos calculados resuelven
 * diferencias de nombre (`totalCapacity`/`totalSpaces`, `rating`/`averageRating`,
 * `distance`/`distanceKm`) sin romper si llega una sola variante.
 * Relación: los usa ParkingApi y los consume ParkingRepositoryImpl; `ParkingLotDto` y
 * `ParkingSpaceDto` también aparecen en AdminApi y OperatorApi.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Resumen de un estacionamiento para listados, búsqueda y cercanía.
 *
 * El backend actual envía `totalSpaces`, `availableSpaces`, `averageRating` y
 * `distanceKm`; sus getters añaden también `totalCapacity`, `rating` y `distance`, y
 * otras versiones pueden usar solo una de las variantes. Los tres valores calculados
 * al final unifican esas diferencias. Los campos no enviados conservan sus valores por
 * defecto gracias a Kotlin Serialization; los desconocidos se ignoran.
 *
 * @property id identificador interno del estacionamiento; `0` si falta.
 * @property name nombre comercial.
 * @property description descripción opcional; puede no venir en el listado.
 * @property address dirección textual.
 * @property district distrito opcional.
 * @property city ciudad opcional.
 * @property latitude latitud en grados decimales (WGS-84); `0.0` si falta.
 * @property longitude longitud en grados decimales (WGS-84); `0.0` si falta.
 * @property phone teléfono opcional; puede no venir en el listado.
 * @property totalSpaces cantidad total de espacios según `totalSpaces`.
 * @property totalCapacityVal alias opcional `totalCapacity` enviado por algunas
 *   versiones del backend.
 * @property availableSpaces espacios disponibles en el momento de la consulta.
 * @property isActive indica si el estacionamiento está habilitado; por defecto `true`.
 * @property isOpen indica si está abierto para operar ahora; por defecto `true`.
 * @property imageUrl URL opcional de la imagen principal.
 * @property distanceKm distancia en kilómetros; solo la rellena la búsqueda cercana.
 * @property distanceVal alias opcional `distance`, también en kilómetros.
 * @property services nombres de servicios; en el listado puede venir vacío.
 * @property averageRating promedio de calificaciones (0.0 a 5.0) o `null`.
 * @property ratingVal alias opcional `rating`.
 * @property ratingCount cantidad de calificaciones; por defecto `0`.
 * @property operatorId identificador del operador asignado, si existe.
 * @property operatorName nombre del operador asignado, si existe.
 */
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
    @SerialName("ratingCount") val ratingCount: Int = 0,
    @SerialName("operatorId") val operatorId: Long? = null,
    @SerialName("operatorName") val operatorName: String? = null
) {
    /**
     * Capacidad total resuelta para la UI.
     *
     * Prioriza `totalSpaces` cuando es positivo y, si no, usa el alias
     * `totalCapacity`; si ambos faltan, devuelve `0`.
     */
    val totalCapacity: Int get() = if (totalSpaces > 0) totalSpaces else (totalCapacityVal ?: 0)

    /**
     * Distancia en kilómetros resuelta para la UI.
     *
     * Prefiere `distanceKm` y cae al alias `distance`; `null` significa que la
     * consulta no calculó distancia (por ejemplo, un listado normal).
     */
    val distance: Double? get() = distanceKm ?: distanceVal

    /**
     * Calificación promedio resuelta para la UI.
     *
     * Prefiere `averageRating` y cae al alias `rating`; `null` significa que aún no
     * hay calificaciones.
     */
    val rating: Double? get() = averageRating ?: ratingVal
}

/**
 * Detalle completo de un estacionamiento.
 *
 * Incluye datos de contacto, servicios, horarios y tarifas. El backend envía tanto
 * `totalSpaces`/`totalCapacity` como `averageRating`/`rating`/`ratingCount` por sus
 * getters; los calculados eligen el valor disponible. Los elementos de `services`,
 * `schedules` y `tariffs` son objetos, a diferencia de la lista de nombres de
 * [ParkingLotDto].
 *
 * @property id identificador interno; `0` si falta.
 * @property name nombre comercial.
 * @property description descripción opcional.
 * @property address dirección textual.
 * @property district distrito opcional.
 * @property city ciudad opcional.
 * @property latitude latitud WGS-84 en grados decimales.
 * @property longitude longitud WGS-84 en grados decimales.
 * @property phone teléfono de contacto opcional.
 * @property email correo de contacto opcional.
 * @property totalSpaces cantidad total de espacios.
 * @property totalCapacityVal alias opcional `totalCapacity`.
 * @property availableSpaces espacios disponibles ahora.
 * @property isActive indica si está habilitado; por defecto `true`.
 * @property isOpen indica si está abierto ahora; por defecto `true`.
 * @property imageUrl URL opcional de imagen.
 * @property services servicios ofrecidos por el estacionamiento.
 * @property schedules horarios semanales.
 * @property tariffs tarifas por tipo de vehículo y modalidad.
 * @property averageRating promedio de calificaciones (0.0 a 5.0) o `null`.
 * @property ratingVal alias opcional `rating`.
 * @property totalRatings total de calificaciones como `Long` opcional.
 * @property ratingCountVal alias opcional `ratingCount` como entero.
 */
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
    /**
     * Capacidad total resuelta para la UI.
     *
     * Usa `totalSpaces` si es positivo; si no, el alias `totalCapacity`; si ambos
     * faltan, `0`.
     */
    val totalCapacity: Int get() = if (totalSpaces > 0) totalSpaces else (totalCapacityVal ?: 0)

    /**
     * Calificación promedio resuelta para la UI.
     *
     * Prefiere `averageRating` y cae al alias `rating`.
     */
    val rating: Double? get() = averageRating ?: ratingVal

    /**
     * Cantidad de calificaciones resuelta para la UI.
     *
     * Prefiere `totalRatings` (se convierte a `Int`) y cae al alias `ratingCount`;
     * si no hay ninguno, devuelve `0`.
     */
    val ratingCount: Int get() = totalRatings?.toInt() ?: (ratingCountVal ?: 0)
}

/**
 * Servicio ofrecido por un estacionamiento (por ejemplo, videovigilancia, lavado o
 * carga eléctrica).
 *
 * @property id identificador opcional del servicio.
 * @property name nombre visible; por defecto cadena vacía.
 * @property icon nombre o clave del icono que usa la UI; opcional.
 */
@Serializable
data class ParkingServiceDto(
    val id: Long? = null,
    val name: String = "",
    val icon: String? = null
)

/**
 * Horario semanal de un estacionamiento.
 *
 * El backend envía `dayOfWeek` como texto (por ejemplo, `MONDAY`) y las horas como
 * cadenas de hora local. `isOpen` se calcula: si el backend envía el booleano se usa
 * tal cual; si no, se deduce de `isClosed`.
 *
 * @property id identificador opcional del horario.
 * @property dayOfWeek día de la semana como texto del contrato.
 * @property openTime hora de apertura, normalmente `HH:mm` o `HH:mm:ss`.
 * @property closeTime hora de cierre, normalmente `HH:mm` o `HH:mm:ss`.
 * @property isClosed indica si el local cierra ese día; por defecto `false`.
 * @property isOpenVal valor explícito opcional `isOpen` enviado por el backend.
 */
@Serializable
data class ScheduleDto(
    val id: Long? = null,
    @SerialName("dayOfWeek") val dayOfWeek: String = "",
    @SerialName("openTime") val openTime: String = "",
    @SerialName("closeTime") val closeTime: String = "",
    @SerialName("isClosed") val isClosed: Boolean = false,
    @SerialName("isOpen") val isOpenVal: Boolean? = null
) {
    /**
     * Indica si el horario está abierto.
     *
     * Usa `isOpenVal` si el backend lo envía; en su defecto, lo contrario de
     * `isClosed`.
     */
    val isOpen: Boolean get() = isOpenVal ?: !isClosed
}

/**
 * Tarifa de un estacionamiento.
 *
 * El backend actual envía `tariffType`, `price`, `vehicleTypeName` y
 * `minHours`/`maxHours`. Este DTO no declara `minHours`/`maxHours`, por lo que
 * `minimumMinutes` conserva su valor por defecto (`30`) salvo que el contrato cambie;
 * es una diferencia de contrato a tener en cuenta al mostrar precios por tiempo.
 *
 * @property id identificador de la tarifa.
 * @property vehicleTypeName nombre del tipo de vehículo aplicable; opcional.
 * @property vehicleTypeVal alias opcional `vehicleType`, equivalente al nombre.
 * @property tariffType modalidad: `HOURLY`, `DAILY`, `WEEKLY`, `MONTHLY` o `FIXED`.
 * @property price precio en la moneda del contrato (el backend usa `PEN`); por
 *   defecto `0.0`.
 * @property minimumMinutes minutos mínimos de cobro; por defecto `30` si no llega.
 */
@Serializable
data class TariffDto(
    val id: Long = 0,
    @SerialName("vehicleTypeName") val vehicleTypeName: String? = null,
    @SerialName("vehicleType") val vehicleTypeVal: String? = null,
    @SerialName("tariffType") val tariffType: String = "",
    val price: Double = 0.0,
    @SerialName("minimumMinutes") val minimumMinutes: Int = 30
) {
    /**
     * Tipo de vehículo resuelto para la UI.
     *
     * Prefiere `vehicleTypeName`, luego el alias `vehicleType` y, si ambos faltan,
     * devuelve cadena vacía.
     */
    val vehicleType: String get() = vehicleTypeName ?: (vehicleTypeVal ?: "")
}

/**
 * Resumen agregado de disponibilidad de un estacionamiento.
 *
 * No lo devuelve directamente `ParkingApi`: `ParkingRepositoryImpl.getAvailability`
 * lo construye a partir de la lista de espacios disponibles (cuenta los disponibles y
 * los ocupados, y deja `reservedSpaces = 0`). Sirve a la UI como vista consolidada.
 *
 * @property parkingLotId identificador del estacionamiento consultado.
 * @property totalCapacity total de espacios considerados en el cálculo.
 * @property availableSpaces espacios disponibles.
 * @property occupiedSpaces espacios no disponibles.
 * @property reservedSpaces espacios reservados; el cálculo local actual lo deja en 0.
 */
@Serializable
data class AvailabilityDto(
    @SerialName("parkingLotId") val parkingLotId: Long = 0,
    @SerialName("totalCapacity") val totalCapacity: Int = 0,
    @SerialName("availableSpaces") val availableSpaces: Int = 0,
    @SerialName("occupiedSpaces") val occupiedSpaces: Int = 0,
    @SerialName("reservedSpaces") val reservedSpaces: Int = 0
)

/**
 * Espacio físico de estacionamiento.
 *
 * El backend envía `status` y `vehicleTypeName`; `isAvailable` puede venir explícito
 * o deducirse del estado. Los estados posibles son `AVAILABLE`, `OCCUPIED`,
 * `RESERVED`, `MAINTENANCE` e `INACTIVE`. Otros campos del backend (`floor`,
 * `section`, `isCovered`, etc.) no se modelan y se ignoran.
 *
 * @property id identificador del espacio.
 * @property spaceNumber número o código visible del espacio; por defecto vacío.
 * @property status estado actual; por defecto `AVAILABLE`.
 * @property vehicleTypeName tipo de vehículo admitido; opcional.
 * @property vehicleTypeVal alias opcional `vehicleType`.
 * @property isAvailableVal indicador explícito opcional de disponibilidad.
 */
@Serializable
data class ParkingSpaceDto(
    val id: Long = 0,
    @SerialName("spaceNumber") val spaceNumber: String = "",
    val status: String = "AVAILABLE",
    @SerialName("vehicleTypeName") val vehicleTypeName: String? = null,
    @SerialName("vehicleType") val vehicleTypeVal: String? = null,
    @SerialName("isAvailable") val isAvailableVal: Boolean? = null
) {
    /**
     * Tipo de vehículo resuelto para la UI.
     *
     * Prefiere `vehicleTypeName`, luego `vehicleType` y, si ambos faltan, cadena vacía.
     */
    val vehicleType: String get() = vehicleTypeName ?: (vehicleTypeVal ?: "")

    /**
     * Disponibilidad resuelta para la UI.
     *
     * Usa `isAvailableVal` si viene explícito; si no, considera disponible solo el
     * estado `AVAILABLE`.
     */
    val isAvailable: Boolean get() = isAvailableVal ?: (status == "AVAILABLE")
}
