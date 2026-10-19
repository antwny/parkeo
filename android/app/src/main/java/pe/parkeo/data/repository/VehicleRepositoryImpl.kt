/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/repository/VehicleRepositoryImpl.kt
 * Capa: datos (implementación de repositorio).
 * Propósito: implementar `VehicleRepository` con `VehicleApi`: listar, crear,
 * actualizar y eliminar vehículos, además de consultar el catálogo de tipos.
 * Traduce respuestas y excepciones al `Result` del proyecto.
 * Conceptos clave:
 *   - Implementación de contrato: `VehicleViewModel` usa la interfaz;
 *     `DefaultAppContainer` construye esta clase con `VehicleApi`.
 *   - `Response`: HTTP exitoso no basta; se exige `body().success == true`
 *     porque el backend envuelve todo en `ApiResponseDto`.
 *   - `errorBody`: en creación/actualización se usa `NetworkErrorParser` para
 *     conservar errores de validación; en listados/eliminación se usa el
 *     mensaje del cuerpo, que puede ser `null` si el HTTP falló.
 *   - `Result<Unit>`: representa una operación sin valor de retorno.
 *   - DTO frente a modelo: se devuelven `VehicleDto`/`VehicleTypeDto`; los
 *     modelos de dominio `Vehicle`/`VehicleType` no se construyen aquí.
 * Flujo: pantalla -> ViewModel -> contrato -> esta clase -> Retrofit ->
 * backend Spring -> MySQL. El backend verifica que el vehículo pertenezca al
 * usuario autenticado y valida la placa.
 * Relación: `VehicleViewModel` usa la interfaz; `AppContainer` inyecta
 * `VehicleApi`; el backend y MySQL conservan el vehículo.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.repository

import pe.parkeo.data.remote.api.VehicleApi
import pe.parkeo.data.remote.dto.*
import pe.parkeo.domain.repository.VehicleRepository
import pe.parkeo.util.Result

/**
 * Implementación concreta de `VehicleRepository`.
 *
 * No guarda estado local; cada operación refleja lo que responde el backend.
 *
 * @param vehicleApi Cliente Retrofit con las rutas de vehículos y tipos.
 */
class VehicleRepositoryImpl(
    private val vehicleApi: VehicleApi
) : VehicleRepository {

    /**
     * Lista los vehículos del usuario autenticado.
     *
     * Llama a GET `api/vehicles`. Si la respuesta es válida, devuelve `data` o
     * lista vacía; si no, devuelve `Error` con el mensaje del cuerpo o
     * "Error". Los fallos de red se capturan y producen "Error de conexión".
     *
     * @return `Result.Success` con la lista o `Result.Error`.
     */
    override suspend fun getMyVehicles(): Result<List<VehicleDto>> {
        return try {
            val response = vehicleApi.getMyVehicles()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data ?: emptyList())
            } else Result.Error(response.body()?.message ?: "Error")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Registra un vehículo nuevo.
     *
     * Construye `CreateVehicleRequestDto`, llama a POST `api/vehicles` y, si
     * HTTP es exitoso y `success = true`, devuelve `data!!`. En caso contrario
     * usa `NetworkErrorParser` sobre `errorBody` para extraer validaciones del
     * backend, con el mensaje del cuerpo o "Error al crear vehículo" como
     * respaldo. El `Result.Error` no incluye código HTTP.
     *
     * @param licensePlate Placa del vehículo.
     * @param brand Marca opcional.
     * @param model Modelo opcional.
     * @param color Color opcional.
     * @param vehicleTypeId Tipo del catálogo.
     * @return `Result.Success` con el vehículo creado o `Result.Error`.
     */
    override suspend fun createVehicle(
        licensePlate: String,
        brand: String?,
        model: String?,
        color: String?,
        vehicleTypeId: Long
    ): Result<VehicleDto> {
        return try {
            val response = vehicleApi.createVehicle(
                CreateVehicleRequestDto(licensePlate, brand, model, color, vehicleTypeId)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data!!)
            } else {
                val errorMsg = pe.parkeo.util.NetworkErrorParser.parseErrorMessage(
                    response.errorBody(),
                    response.body()?.message ?: "Error al crear vehículo"
                )
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Actualiza un vehículo existente.
     *
     * Llama a PUT `api/vehicles/{id}` reutilizando `CreateVehicleRequestDto`:
     * envía todos los campos editables, por lo que se comporta como reemplazo
     * y no como actualización parcial. Si la respuesta es válida devuelve el
     * DTO; si no, extrae el mensaje con `NetworkErrorParser` y usa "Error al
     * actualizar vehículo" como respaldo.
     *
     * @param id Identificador del vehículo a actualizar.
     * @param licensePlate Nueva placa.
     * @param brand Nueva marca opcional.
     * @param model Nuevo modelo opcional.
     * @param color Nuevo color opcional.
     * @param vehicleTypeId Nuevo tipo del catálogo.
     * @return `Result.Success` con el vehículo actualizado o `Result.Error`.
     */
    override suspend fun updateVehicle(
        id: Long,
        licensePlate: String,
        brand: String?,
        model: String?,
        color: String?,
        vehicleTypeId: Long
    ): Result<VehicleDto> {
        return try {
            val response = vehicleApi.updateVehicle(
                id, CreateVehicleRequestDto(licensePlate, brand, model, color, vehicleTypeId)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data!!)
            } else {
                val errorMsg = pe.parkeo.util.NetworkErrorParser.parseErrorMessage(
                    response.errorBody(),
                    response.body()?.message ?: "Error al actualizar vehículo"
                )
                Result.Error(errorMsg)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Elimina o desactiva un vehículo.
     *
     * Llama a DELETE `api/vehicles/{id}`; el backend aplica una baja lógica
     * (`isActive = false`) y no borra la fila. Si la respuesta es válida
     * devuelve `Result.Success(Unit)`, que indica éxito sin datos. Si no, usa el
     * mensaje del cuerpo o "Error al eliminar vehículo". No se usa
     * `NetworkErrorParser` aquí, así que un error HTTP con cuerpo vacío
     * produce el mensaje genérico.
     *
     * @param id Identificador del vehículo.
     * @return `Result.Success(Unit)` o `Result.Error`.
     */
    override suspend fun deleteVehicle(id: Long): Result<Unit> {
        return try {
            val response = vehicleApi.deleteVehicle(id)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(Unit)
            } else Result.Error(response.body()?.message ?: "Error al eliminar vehículo")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Obtiene el catálogo de tipos de vehículo.
     *
     * Llama a GET `api/vehicles/types`. Si la respuesta es válida devuelve
     * `data` o lista vacía; si no, `Error` con el mensaje del cuerpo o
     * "Error". Los fallos de red se capturan.
     *
     * @return `Result.Success` con los tipos o `Result.Error`.
     */
    override suspend fun getVehicleTypes(): Result<List<VehicleTypeDto>> {
        return try {
            val response = vehicleApi.getVehicleTypes()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()!!.data ?: emptyList())
            } else Result.Error(response.body()?.message ?: "Error")
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }
}
