/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/remote/dto/VehicleDtos.kt
 * Capa: datos/red · contrato (DTO de vehículos).
 * Propósito: modelar los vehículos del usuario, el catálogo de tipos y el cuerpo para
 * crear o actualizar un vehículo.
 * Contrato JSON: vehículo `{"id":1,"licensePlate":"ABC-123","brand":"Toyota",
 * "vehicleTypeId":2,"vehicleTypeName":"Automóvil","isActive":true}`; tipo
 * `{"id":2,"name":"Automóvil"}`; creación con `licensePlate`, marca, modelo, color y
 * `vehicleTypeId`.
 * Endpoints: `GET/POST /api/vehicles`, `PUT/DELETE /api/vehicles/{id}`,
 * `GET /api/vehicles/types`.
 * Conceptos clave: Kotlin Serialization, `@SerialName`, placas normalizadas, borrado
 * lógico, tipos de vehículo, valores por defecto, campos opcionales, `ignoreUnknownKeys`.
 * Flujo resumido: la UI lista los vehículos, crea uno con placa y tipo, actualiza
 * marca/modelo/color y lo desactiva; al reservar se envía `vehicleId`.
 * Relación: lo usan VehicleApi y VehicleRepositoryImpl. El backend filtra por
 * propietario y normaliza la placa a mayúsculas con guion.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Vehículo registrado por el usuario.
 *
 * El backend envía `vehicleTypeId` y `vehicleTypeName`, no un objeto anidado
 * `vehicleType`; el calculado al final construye un `VehicleTypeDto` de respaldo para
 * que la UI siempre tenga un tipo que mostrar. El borrado es lógico: `isActive` pasa a
 * `false` y el vehículo deja de listarse, pero se conserva para reservas históricas.
 * El DTO no modela `createdAt`, que el backend también envía y aquí se ignora.
 *
 * @property id identificador del vehículo; `0` si falta.
 * @property licensePlate placa normalizada por el backend (mayúsculas, con guion).
 * @property brand marca opcional.
 * @property model modelo opcional.
 * @property color color opcional.
 * @property year año opcional; el backend lo acepta al crear, pero la app no lo envía
 *   en [CreateVehicleRequestDto].
 * @property isActive indica si el vehículo está habilitado; por defecto `true`.
 * @property vehicleTypeId identificador del tipo de vehículo; opcional.
 * @property vehicleTypeName nombre del tipo de vehículo; opcional.
 * @property vehicleTypeObj objeto `vehicleType` opcional que algunas versiones del
 *   contrato pueden anidar.
 */
@Serializable
data class VehicleDto(
    val id: Long = 0,
    @SerialName("licensePlate") val licensePlate: String = "",
    val brand: String? = null,
    val model: String? = null,
    val color: String? = null,
    val year: Int? = null,
    @SerialName("isActive") val isActive: Boolean = true,
    @SerialName("vehicleTypeId") val vehicleTypeId: Long? = null,
    @SerialName("vehicleTypeName") val vehicleTypeName: String? = null,
    @SerialName("vehicleType") val vehicleTypeObj: VehicleTypeDto? = null
) {
    /**
     * Tipo de vehículo resuelto para la UI.
     *
     * Usa el objeto `vehicleType` si vino anidado; en su defecto construye uno con
     * `vehicleTypeId`/`vehicleTypeName`, y si tampoco existen usa `0` y `"Vehículo"`.
     */
    val vehicleType: VehicleTypeDto
        get() = vehicleTypeObj ?: VehicleTypeDto(
            id = vehicleTypeId ?: 0L,
            name = vehicleTypeName ?: "Vehículo"
        )
}

/**
 * Tipo de vehículo del catálogo (automóvil, motocicleta, camioneta, etc.).
 *
 * Se usa para poblar selectores y para filtrar disponibilidad. `icon` es una clave o
 * nombre de recurso interpretado por la UI; puede no venir del backend. Los tipos
 * inactivos se excluyen en el endpoint de catálogo.
 *
 * @property id identificador del tipo; `0` si falta.
 * @property name nombre visible.
 * @property description descripción opcional.
 * @property icon clave o nombre del icono; opcional.
 * @property isActive indica si el tipo está habilitado; por defecto `true`.
 */
@Serializable
data class VehicleTypeDto(
    val id: Long = 0,
    val name: String = "",
    val description: String? = null,
    val icon: String? = null,
    @SerialName("isActive") val isActive: Boolean = true
)

/**
 * Cuerpo de `POST /api/vehicles` y, reutilizado, de `PUT /api/vehicles/{id}`.
 *
 * En la creación el backend exige placa y `vehicleTypeId`, valida longitudes y
 * normaliza la placa. En la actualización el backend solo aplica los campos no nulos;
 * la placa incluida en el cuerpo se ignora porque el contrato de actualización no la
 * contempla. `year` existe en el backend pero no se modela aquí, por lo que no se
 * envía desde la app.
 *
 * @property licensePlate placa; obligatoria al crear, entre 6 y 10 caracteres según el
 *   backend; se normaliza a mayúsculas y guion.
 * @property brand marca opcional, máximo 50 caracteres.
 * @property model modelo opcional, máximo 50 caracteres.
 * @property color color opcional, máximo 30 caracteres.
 * @property vehicleTypeId tipo de vehículo; obligatorio al crear.
 */
@Serializable
data class CreateVehicleRequestDto(
    @SerialName("licensePlate") val licensePlate: String,
    val brand: String? = null,
    val model: String? = null,
    val color: String? = null,
    @SerialName("vehicleTypeId") val vehicleTypeId: Long
)
