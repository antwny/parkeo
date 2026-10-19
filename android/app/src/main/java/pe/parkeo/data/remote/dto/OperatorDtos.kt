/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/remote/dto/OperatorDtos.kt
 * Capa: datos/red · contrato (DTO del operador).
 * Propósito: modelar el cuerpo con el que el operador cambia el estado de un espacio
 * de estacionamiento.
 * Contrato JSON: `{"status":"OCCUPIED","notes":"Ingreso manual"}`. `status` es
 * obligatorio; `notes` puede omitirse.
 * Endpoint: `PATCH /api/operator/spaces/{spaceId}/status`.
 * Conceptos clave: Kotlin Serialization, `@Serializable`, estados de espacio,
 * campos opcionales, auditoría.
 * Flujo resumido: el operador toca un espacio en la pantalla de cochera; el
 * ViewModel llama a OperatorRepository, que envía este DTO; el backend valida el
 * estado y devuelve el `ParkingSpaceDto` actualizado.
 * Relación: lo usan OperatorApi y OperatorRepositoryImpl. Requiere `ROLE_OPERATOR` o
 * `ROLE_ADMIN` y acceso a la cochera del espacio.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Cuerpo de `PATCH /api/operator/spaces/{spaceId}/status`.
 *
 * El backend exige `status` no vacío y lo valida contra el enum de espacios. Los
 * valores posibles son `AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE` e
 * `INACTIVE`. `notes` es informativo y se usa para auditoría; no cambia la lógica de
 * disponibilidad por sí solo.
 *
 * @property status nuevo estado del espacio; obligatorio.
 * @property notes nota opcional para explicar el cambio; `null` omite el campo.
 */
@Serializable
data class UpdateSpaceStatusRequestDto(
    val status: String,
    val notes: String? = null
)
