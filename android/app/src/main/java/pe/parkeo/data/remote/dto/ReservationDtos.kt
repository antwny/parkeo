/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/remote/dto/ReservationDtos.kt
 * Capa: datos/red · contrato (DTO de reservas).
 * Propósito: modelar las reservas que devuelve el backend y los cuerpos para crearlas
 * o cancelarlas.
 * Contrato JSON: respuesta de lista `{"data":{"content":[{"id":1,"status":"PENDING",
 * "startTime":"2026-10-19T14:00:00",...}]}}`; creación
 * `{"parkingLotId":1,"vehicleId":2,"startTime":"...","endTime":"..."}`; cancelación
 * `{"reason":"..."}` o `{}`.
 * Endpoints: `GET /api/reservations`, `GET /api/reservations/{id}`,
 * `POST /api/reservations`, `PATCH /api/reservations/{id}/cancel`; el operador usa
 * las mismas formas en `GET /api/operator/reservations` y sus check-in/check-out.
 * Conceptos clave: Kotlin Serialization, `@SerialName`, fechas ISO-8601 locales,
 * estados de reserva, importes en `PEN`, alias de campos, nulabilidad, `ignoreUnknownKeys`.
 * Flujo resumido: el cliente crea la reserva con horas y vehículo; el backend calcula
 * el precio y asigna espacio si no se eligió uno. La lista y el detalle comparten DTO;
 * los campos exclusivos del detalle quedan `null` cuando no vienen.
 * Relación: lo usan ReservationApi y OperatorApi; ReservationRepositoryImpl y
 * OperatorRepositoryImpl los transforman para los ViewModels.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Reserva tal como la devuelve el backend en listas y detalle.
 *
 * `status` usa los valores `PENDING`, `CONFIRMED`, `ACTIVE`, `COMPLETED`, `CANCELLED`
 * y `NO_SHOW`. Las horas son texto ISO-8601 local sin zona horaria
 * (`yyyy-MM-ddTHH:mm:ss`), por ejemplo `2026-10-19T14:30:00`. El backend envía
 * `totalAmount` y `currency` (`PEN`); el alias `totalPrice` se admite por
 * compatibilidad. Los campos de coordenadas y `createdAt` solo llegan en el detalle;
 * en la lista se quedan en `null`. Campos adicionales del detalle (notas, marcas de
 * check-in/check-out, datos del usuario) no están modelados y se ignoran.
 *
 * @property id identificador de la reserva; `0` si falta.
 * @property confirmationCode código de confirmación generado por el backend; opcional.
 * @property parkingLotName nombre del estacionamiento.
 * @property parkingLotAddress dirección del estacionamiento.
 * @property spaceNumber número del espacio asignado.
 * @property vehicleLicensePlate placa del vehículo reservado.
 * @property startTime inicio en formato ISO-8601 local.
 * @property endTime fin en formato ISO-8601 local.
 * @property status estado de la reserva; por defecto `PENDING`.
 * @property totalAmount importe total calculado por el backend; opcional.
 * @property totalPriceVal alias opcional `totalPrice` del mismo importe.
 * @property currency moneda del importe, normalmente `PEN`; opcional.
 * @property parkingLotId identificador del estacionamiento; opcional en la forma
 *   reducida.
 * @property parkingSpaceId identificador del espacio; opcional.
 * @property vehicleId identificador del vehículo; opcional.
 * @property parkingLotLatitude latitud del estacionamiento; solo en detalle.
 * @property parkingLotLongitude longitud del estacionamiento; solo en detalle.
 * @property createdAt fecha de creación ISO-8601; solo en detalle.
 */
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
    // Solo vienen en el detalle de la reserva (GET /api/reservations/{id})
    @SerialName("parkingLotLatitude") val parkingLotLatitude: Double? = null,
    @SerialName("parkingLotLongitude") val parkingLotLongitude: Double? = null,
    @SerialName("createdAt") val createdAt: String? = null
) {
    /**
     * Importe total resuelto para la UI.
     *
     * Prefiere `totalAmount` y cae al alias `totalPrice`; `null` si el backend aún no
     * calculó el precio.
     */
    val totalPrice: Double? get() = totalAmount ?: totalPriceVal
}

/**
 * Cuerpo de `POST /api/reservations`.
 *
 * El backend valida que el vehículo pertenezca al usuario, que no exista otra reserva
 * solapada, que el espacio esté libre y que el rango sea válido. Si
 * `parkingSpaceId` es `null`, asigna automáticamente un espacio compatible con el
 * tipo del vehículo. El precio y el código de confirmación los genera el servidor.
 *
 * @property parkingLotId estacionamiento donde se reserva; obligatorio.
 * @property parkingSpaceId espacio concreto; `null` permite asignación automática.
 * @property vehicleId vehículo del usuario; obligatorio.
 * @property startTime inicio en ISO-8601 local (`yyyy-MM-ddTHH:mm:ss`).
 * @property endTime fin en ISO-8601 local; debe ser posterior al inicio.
 * @property notes nota libre opcional, con máximo de 500 caracteres en el backend.
 */
@Serializable
data class CreateReservationRequestDto(
    @SerialName("parkingLotId") val parkingLotId: Long,
    @SerialName("parkingSpaceId") val parkingSpaceId: Long? = null,
    @SerialName("vehicleId") val vehicleId: Long,
    @SerialName("startTime") val startTime: String,
    @SerialName("endTime") val endTime: String,
    @SerialName("notes") val notes: String? = null
)

/**
 * Cuerpo opcional de `PATCH /api/reservations/{id}/cancel`.
 *
 * El backend acepta un cuerpo ausente o un mapa con `reason`. Aquí se usa el
 * constructor por defecto, que serializa `{}` y equivale a cancelar sin motivo. El
 * motivo es informativo; la reserva pasa a `CANCELLED` y se conserva para historial.
 *
 * @property reason motivo opcional de la cancelación; `null` no envía motivo.
 */
@Serializable
data class CancelReservationRequestDto(
    @SerialName("reason") val reason: String? = null
)