/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/repository/AdminRepositoryImpl.kt
 * Capa: datos (implementación de repositorio).
 * Propósito: implementar `AdminRepository` con `AdminApi` para el panel de
 * administración: estadísticas, estacionamientos, asignación de operadores y
 * usuarios. Normaliza respuestas y errores al `Result` del proyecto.
 * Conceptos clave:
 *   - Implementación de contrato: `AdminViewModel` depende de la interfaz y
 *     `DefaultAppContainer` construye esta clase.
 *   - `Response`: se exige HTTP exitoso y `body().success == true`.
 *   - `safeApiCall`: helper privado que ejecuta una lambda suspendida y
 *     convierte excepciones en `Result.Error`.
 *   - DTO frente a modelo: se devuelven DTO de red; no hay mapeo a modelos de
 *     dominio en esta versión.
 *   - Autorización: el backend exige rol administrador; el cliente solo es
 *     una conveniencia de UI.
 * Flujo: pantalla de administración -> ViewModel -> contrato -> esta clase ->
 * Retrofit -> backend Spring -> MySQL.
 * Relación: `AdminViewModel` consume la interfaz; `AppContainer` inyecta
 * `AdminApi`; el backend Spring valida el rol y persiste en MySQL.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.repository

import pe.parkeo.data.remote.api.AdminApi
import pe.parkeo.data.remote.dto.*
import pe.parkeo.domain.repository.AdminRepository
import pe.parkeo.util.Result

/**
 * Implementación concreta de `AdminRepository`.
 *
 * Todas las rutas administrativas requieren el token de un usuario con rol
 * `ADMIN`; el interceptor lo añade y el backend decide si autoriza.
 *
 * @param adminApi Cliente Retrofit con las rutas administrativas.
 */
class AdminRepositoryImpl(
    private val adminApi: AdminApi
) : AdminRepository {

    /**
     * Obtiene las estadísticas globales.
     *
     * Llama a GET `api/admin/statistics`. Si la respuesta es válida pero
     * `data` es `null`, devuelve un `AdminStatisticsDto()` con ceros para que
     * el panel no falle; si no, lanza una excepción con el mensaje del cuerpo
     * o "Error al obtener estadísticas" que `safeApiCall` convierte en
     * `Error`.
     *
     * @return `Result.Success` con las estadísticas (posiblemente en cero) o
     *   `Result.Error`.
     */
    override suspend fun getStatistics(): Result<AdminStatisticsDto> = safeApiCall {
        val response = adminApi.getStatistics()
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: AdminStatisticsDto()
        } else throw Exception(response.body()?.message ?: "Error al obtener estadísticas")
    }

    /**
     * Lista los estacionamientos para administración.
     *
     * Llama a GET `api/admin/parking-lots` con la paginación por defecto
     * (página 0, tamaño 50) y devuelve `data.content` o lista vacía. El
     * mensaje de respaldo es "Error al obtener estacionamientos".
     *
     * @return `Result.Success` con la lista o `Result.Error`.
     */
    override suspend fun getParkingLots(): Result<List<ParkingLotDto>> = safeApiCall {
        val response = adminApi.getParkingLots()
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data?.content ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error al obtener estacionamientos")
    }

    /**
     * Actualiza el estado de apertura y/o activación de un estacionamiento.
     *
     * Llama a PUT `api/admin/parking-lots/{id}/status` con
     * `UpdateParkingLotStatusRequestDto(isOpen, isActive)`. Los valores `null`
     * permiten no modificar ese aspecto. Devuelve el DTO actualizado; si
     * `data` es `null` o el HTTP falla, lanza una excepción con el mensaje del
     * cuerpo o "Error al actualizar estacionamiento".
     *
     * @param id Identificador del estacionamiento.
     * @param isOpen Nuevo estado de apertura o `null`.
     * @param isActive Nuevo estado de activación o `null`.
     * @return `Result.Success` con el estacionamiento actualizado o
     *   `Result.Error`.
     */
    override suspend fun updateParkingLotStatus(id: Long, isOpen: Boolean?, isActive: Boolean?): Result<ParkingLotDto> = safeApiCall {
        val response = adminApi.updateParkingLotStatus(id, UpdateParkingLotStatusRequestDto(isOpen, isActive))
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(response.body()?.message ?: "Error al actualizar estacionamiento")
    }

    /**
     * Asigna un operador a un estacionamiento.
     *
     * Llama a PUT `api/admin/parking-lots/{id}/operator` con
     * `AssignOperatorRequestDto(operatorId)`. El backend valida que el usuario
     * exista y tenga el rol adecuado. Devuelve el estacionamiento actualizado
     * o lanza una excepción con el mensaje del cuerpo o "Error al asignar
     * operador".
     *
     * @param id Identificador del estacionamiento.
     * @param operatorId Identificador del operador.
     * @return `Result.Success` con el estacionamiento o `Result.Error`.
     */
    override suspend fun assignOperator(id: Long, operatorId: Long): Result<ParkingLotDto> = safeApiCall {
        val response = adminApi.assignOperator(id, AssignOperatorRequestDto(operatorId))
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(response.body()?.message ?: "Error al asignar operador")
    }

    /**
     * Desactiva un estacionamiento.
     *
     * Llama a DELETE `api/admin/parking-lots/{id}`. El backend aplica una baja
     * lógica (`isActive = false`) y no borra la fila. Si la respuesta es válida
     * devuelve `Success(Unit)`; si no, lanza una excepción con el mensaje del
     * cuerpo o "Error al desactivar". Las relaciones existentes las resuelve el
     * servidor.
     *
     * @param id Identificador del estacionamiento.
     * @return `Result.Success(Unit)` o `Result.Error`.
     */
    override suspend fun deactivateParkingLot(id: Long): Result<Unit> = safeApiCall {
        val response = adminApi.deactivateParkingLot(id)
        if (response.isSuccessful && response.body()?.success == true) {
            Unit
        } else throw Exception(response.body()?.message ?: "Error al desactivar")
    }

    /**
     * Lista los usuarios registrados.
     *
     * Llama a GET `api/admin/users`, que devuelve una lista directa (no
     * paginada en esta ruta). Si la respuesta es válida usa `data` o lista
     * vacía; si no, lanza una excepción con el mensaje del cuerpo o "Error al
     * obtener usuarios".
     *
     * @return `Result.Success` con los usuarios o `Result.Error`.
     */
    override suspend fun getUsers(): Result<List<UserDto>> = safeApiCall {
        val response = adminApi.getUsers()
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data ?: emptyList()
        } else throw Exception(response.body()?.message ?: "Error al obtener usuarios")
    }

    /**
     * Activa o desactiva una cuenta de usuario.
     *
     * Llama a PUT `api/admin/users/{id}/status` con
     * `UpdateUserStatusRequestDto(isActive)`. Devuelve el usuario actualizado
     * o lanza una excepción con el mensaje del cuerpo o "Error al actualizar
     * estado del usuario". Es una baja lógica: no elimina datos históricos.
     *
     * @param id Identificador del usuario.
     * @param isActive `true` para activar; `false` para desactivar.
     * @return `Result.Success` con el usuario actualizado o `Result.Error`.
     */
    override suspend fun updateUserStatus(id: Long, isActive: Boolean): Result<UserDto> = safeApiCall {
        val response = adminApi.updateUserStatus(id, UpdateUserStatusRequestDto(isActive))
        if (response.isSuccessful && response.body()?.success == true) {
            response.body()!!.data!!
        } else throw Exception(response.body()?.message ?: "Error al actualizar estado del usuario")
    }

    /**
     * Ejecuta una operación suspensible y normaliza éxito/error.
     *
     * Si la lambda termina, envuelve el valor en `Result.Success`; si lanza
     * cualquier `Exception`, devuelve `Result.Error` con su mensaje o "Error
     * de conexión". No conserva el código HTTP y, al capturar `Exception`,
     * también absorbe cancelaciones de corrutina y errores de programación.
     *
     * @param call Lambda suspendida que realiza la petición y valida la
     *   respuesta.
     * @return `Result.Success` con el valor o `Result.Error` con mensaje.
     */
    private suspend fun <T> safeApiCall(call: suspend () -> T): Result<T> {
        return try {
            Result.Success(call())
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }
}
