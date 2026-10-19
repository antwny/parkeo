/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/remote/dto/AdminDtos.kt
 * Capa: datos/red · contrato (DTO del panel administrativo).
 * Propósito: modelar las estadísticas del dashboard y los cuerpos para cambiar el
 * estado de usuarios y estacionamientos o asignar un operador.
 * Contrato JSON: estadísticas `{"totalParkingLots":3,"totalReservations":120,
 * "pendingReservations":4,...}`; estados `{"isActive":true}` o
 * `{"isOpen":false,"isActive":true}`; asignación `{"operatorId":7}`.
 * Endpoints: `GET /api/admin/statistics`, `GET /api/admin/users`,
 * `PUT /api/admin/users/{id}/status`, `GET /api/admin/parking-lots`,
 * `PUT /api/admin/parking-lots/{id}/status`,
 * `PUT /api/admin/parking-lots/{id}/operator`,
 * `DELETE /api/admin/parking-lots/{id}`.
 * Conceptos clave: Kotlin Serialization, `@SerialName`, conteos, RBAC, banderas
 * `isActive`/`isOpen`, campos opcionales, administración de cuentas.
 * Flujo resumido: AdminApi obtiene estadísticas y listados; AdminRepositoryImpl los
 * expone a AdminViewModel; las acciones de estado envían un cuerpo pequeño y reciben
 * el recurso actualizado.
 * Relación: lo usan AdminApi y AdminRepositoryImpl; `UserDto` y `ParkingLotDto` vienen
 * de AuthDtos y ParkingDtos respectivamente. Todo requiere `ROLE_ADMIN` en el backend.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Conteos globales del dashboard administrativo.
 *
 * El backend devuelve un `Map<String, Object>`; Kotlin lo deserializa en esta clase
 * porque las claves coinciden. Son totales absolutos: `totalParkingLots` incluye
 * cocheras inactivas y los conteos de reservas suman todos los estados. Si una clave
 * falta, el campo conserva `0`; si llega un valor mayor que `Long`, la conversión
 * fallaría.
 *
 * @property totalParkingLots total de estacionamientos registrados.
 * @property totalReservations total histórico de reservas.
 * @property pendingReservations reservas en `PENDING`.
 * @property confirmedReservations reservas en `CONFIRMED`.
 * @property activeReservations reservas en `ACTIVE` (cliente dentro).
 * @property completedReservations reservas en `COMPLETED` (salida registrada).
 * @property cancelledReservations reservas en `CANCELLED`.
 */
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

/**
 * Cuerpo de `PUT /api/admin/users/{id}/status`.
 *
 * Desactiva o reactiva una cuenta sin borrarla. El backend exige el campo; si falta,
 * responde `400`. Una cuenta inactiva no puede iniciar sesión, pero sus reservas y
 * datos históricos se conservan.
 *
 * @property isActive `true` para habilitar la cuenta, `false` para deshabilitarla.
 */
@Serializable
data class UpdateUserStatusRequestDto(
    @SerialName("isActive") val isActive: Boolean
)

/**
 * Cuerpo de `PUT /api/admin/parking-lots/{id}/status`.
 *
 * Ambos campos son opcionales y solo se aplican los no nulos. `isOpen` controla la
 * operación inmediata (abierto/cerrado) y `isActive` la existencia pública (activo/
 * inactivo). Si ambos son `null`, el cuerpo serializado es `{}` y el backend no cambia
 * nada; conviene enviar al menos uno.
 *
 * @property isOpen estado de apertura; `null` significa “no cambiar”.
 * @property isActive estado de actividad; `null` significa “no cambiar”.
 */
@Serializable
data class UpdateParkingLotStatusRequestDto(
    @SerialName("isOpen") val isOpen: Boolean? = null,
    @SerialName("isActive") val isActive: Boolean? = null
)

/**
 * Cuerpo de `PUT /api/admin/parking-lots/{id}/operator`.
 *
 * Asigna o reemplaza el operador responsable de la cochera. El backend valida que el
 * usuario exista; en la implementación actual no verifica que tenga rol de operador,
 * por lo que el filtrado por rol corresponde a la UI. Un id inexistente produce `404`.
 *
 * @property operatorId identificador del usuario que será operador de la cochera.
 */
@Serializable
data class AssignOperatorRequestDto(
    @SerialName("operatorId") val operatorId: Long
)

