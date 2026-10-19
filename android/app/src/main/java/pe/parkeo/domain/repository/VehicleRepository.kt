/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/domain/repository/VehicleRepository.kt
 * Capa: dominio (contrato de repositorio).
 * Propósito: declarar la gestión de vehículos del usuario (listar, crear,
 * actualizar, eliminar) y la consulta del catálogo de tipos de vehículo que el
 * backend usa para tarifas y espacios.
 * Conceptos clave:
 *   - Contrato: `VehicleViewModel` depende de esta interfaz; la implementación
 *     real es `VehicleRepositoryImpl`, que habla con `VehicleApi`.
 *   - `suspend`: las operaciones acceden a red y se ejecutan en corrutinas.
 *   - `Result`: éxito/error con mensaje; `Result<Unit>` representa una
 *     operación sin datos de retorno.
 *   - DTO frente a modelo: se devuelven `VehicleDto` y `VehicleTypeDto` (tipos
 *     de la capa de datos) en lugar de `Vehicle`/`VehicleType` del dominio. El
 *     mapeo está pendiente en esta versión.
 * Flujo: pantalla de vehículos -> ViewModel -> contrato -> implementación ->
 * Retrofit -> backend/MySQL. `AppContainer` inyecta la implementación y el
 * token JWT lo añade el interceptor; el backend comprueba la propiedad del
 * vehículo antes de modificarlo.
 * Relación: `VehicleViewModel` consume el contrato; `VehicleRepositoryImpl` usa
 * `VehicleApi`; el backend y MySQL conservan los datos.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.*
import pe.parkeo.util.Result

/**
 * Contrato de vehículos del usuario autenticado.
 *
 * La implementación concreta es `VehicleRepositoryImpl`. El backend identifica
 * al usuario por el token, de modo que el cliente no puede pedir vehículos de
 * otra cuenta a través de estas operaciones.
 */
interface VehicleRepository {
    /**
     * Lista los vehículos registrados por el usuario autenticado.
     *
     * @return `Result.Success` con la lista de `VehicleDto` o lista vacía;
     *   `Result.Error` si falla la red o la autenticación.
     */
    suspend fun getMyVehicles(): Result<List<VehicleDto>>

    /**
     * Registra un vehículo nuevo para el usuario.
     *
     * La placa y el tipo son obligatorios; el backend valida duplicados y
     * reglas de formato. Los campos descriptivos pueden omitirse con `null`.
     *
     * @param licensePlate Placa del vehículo, obligatoria.
     * @param brand Marca opcional.
     * @param model Modelo opcional.
     * @param color Color opcional.
     * @param vehicleTypeId Identificador del tipo del catálogo.
     * @return `Result.Success` con el vehículo creado, o `Result.Error` con el
     *   mensaje de validación del backend (por ejemplo placa duplicada).
     */
    suspend fun createVehicle(licensePlate: String, brand: String?, model: String?, color: String?, vehicleTypeId: Long): Result<VehicleDto>

    /**
     * Actualiza un vehículo existente del usuario.
     *
     * La implementación reutiliza el mismo DTO de creación y envía todos los
     * campos, por lo que la operación se comporta como un reemplazo de los
     * datos editables, no como un parche parcial.
     *
     * @param id Identificador del vehículo a actualizar; debe pertenecer al
     *   usuario autenticado.
     * @param licensePlate Nueva placa.
     * @param brand Nueva marca opcional.
     * @param model Nuevo modelo opcional.
     * @param color Nuevo color opcional.
     * @param vehicleTypeId Tipo de vehículo actualizado.
     * @return `Result.Success` con el vehículo actualizado o `Result.Error`
     *   con el mensaje del backend o de conexión.
     */
    suspend fun updateVehicle(id: Long, licensePlate: String, brand: String?, model: String?, color: String?, vehicleTypeId: Long): Result<VehicleDto>

    /**
     * Elimina o desactiva un vehículo del usuario.
     *
     * En esta versión el backend aplica una baja lógica (`isActive = false`),
     * no borra la fila; el contrato solo comunica si la operación tuvo éxito.
     *
     * @param id Identificador del vehículo; debe pertenecer al usuario.
     * @return `Result.Success(Unit)` si el backend confirma; `Result.Error`
     *   si no existe, no pertenece al usuario, tiene reservas asociadas o falla
     *   la red.
     */
    suspend fun deleteVehicle(id: Long): Result<Unit>

    /**
     * Obtiene el catálogo de tipos de vehículo.
     *
     * Se usa para llenar el selector del formulario y para enviar el
     * `vehicleTypeId` al crear/editar. En esta versión el backend devuelve
     * únicamente los tipos activos de su catálogo.
     *
     * @return `Result.Success` con la lista de `VehicleTypeDto` o
     *   `Result.Error` si falla la consulta.
     */
    suspend fun getVehicleTypes(): Result<List<VehicleTypeDto>>
}
