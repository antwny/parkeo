// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. DTO serializable; representa datos que cruzan el contrato HTTP entre la app y el backend.
package pe.parkeo.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateSpaceStatusRequestDto(
    val status: String,
    val notes: String? = null
)
