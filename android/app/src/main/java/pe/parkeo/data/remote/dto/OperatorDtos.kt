package pe.parkeo.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateSpaceStatusRequestDto(
    val status: String,
    val notes: String? = null
)
