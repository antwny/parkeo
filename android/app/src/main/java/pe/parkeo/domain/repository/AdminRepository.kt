/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/domain/repository/AdminRepository.kt
 * Capa: dominio (contrato de repositorio).
 * Propósito: declarar las operaciones del rol administrador: estadísticas,
 * listado y estado de estacionamientos, asignación de operadores, desactivación
 * y gestión de usuarios.
 * Conceptos clave:
 *   - Contrato: `AdminViewModel` depende de esta interfaz; la implementación
 *     concreta es `AdminRepositoryImpl`, que usa `AdminApi`.
 *   - `suspend`: todas las operaciones implican red y deben ejecutarse desde
 *     una corrutina.
 *   - `Result`: los fallos esperables se representan como `Result.Error` con
 *     mensaje; la UI no captura excepciones de Retrofit.
 *   - DTO frente a modelo: se devuelven DTO de red (`AdminStatisticsDto`,
 *     `ParkingLotDto`, `UserDto`) para alimentar directamente los estados de
 *     pantalla; no hay mapeo a modelos de dominio en esta versión.
 *   - Autorización: ocultar botones en la UI no protege las rutas. El backend
 *     valida el rol `ADMIN` y responde 403 cuando corresponde.
 * Flujo: pantalla de administración -> `AdminViewModel` -> contrato ->
 * implementación -> Retrofit -> backend Spring -> MySQL. `AppContainer`
 * ensambla la implementación y el token viaja en la cabecera Authorization.
 * Relación: el ViewModel depende de este contrato; `AdminRepositoryImpl` usa
 * `AdminApi`; el backend valida el rol y persiste en MySQL.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.domain.repository

import pe.parkeo.data.remote.dto.*
import pe.parkeo.util.Result

/**
 * Contrato de administración global de PARKeo.
 *
 * La implementación concreta es `AdminRepositoryImpl`. El backend restringe
 * estas rutas al rol administrador; el cliente solo las invoca desde las
 * pantallas de administración.
 */
interface AdminRepository {
    /**
     * Obtiene los contadores globales del panel administrativo.
     *
     * Incluye totales de estacionamientos y reservas agrupadas por estado.
     * La implementación devuelve un `AdminStatisticsDto()` con ceros si el
     * backend responde correctamente pero sin datos, para que el panel no
     * falle; esa decisión oculta la diferencia entre "sin datos" y "no
     * disponible".
     *
     * @return `Result.Success` con las estadísticas (posiblemente en cero), o
     *   `Result.Error` si la petición falla.
     */
    suspend fun getStatistics(): Result<AdminStatisticsDto>

    /**
     * Lista todos los estacionamientos para administración.
     *
     * La implementación usa la paginación por defecto de la ruta (página 0,
     * tamaño 50); no expone páginas siguientes en este contrato.
     *
     * @return `Result.Success` con la lista de `ParkingLotDto` o
     *   `Result.Error` si falla la consulta.
     */
    suspend fun getParkingLots(): Result<List<ParkingLotDto>>

    /**
     * Cambia el estado operativo o de activación de un estacionamiento.
     *
     * Ambos parámetros son opcionales en el cuerpo: `null` significa "no
     * modificar ese campo". Así se puede abrir/cerrar sin tocar `isActive`, o
     * activar/desactivar sin cambiar `isOpen`.
     *
     * @param id Identificador del estacionamiento.
     * @param isOpen Nuevo estado de apertura; `null` para no cambiarlo.
     * @param isActive Nuevo estado de activación; `null` para no cambiarlo.
     * @return `Result.Success` con el estacionamiento actualizado o
     *   `Result.Error` con el mensaje del backend.
     */
    suspend fun updateParkingLotStatus(id: Long, isOpen: Boolean?, isActive: Boolean?): Result<ParkingLotDto>

    /**
     * Asigna un operador a un estacionamiento.
     *
     * El backend valida que el usuario destino exista y tenga rol de operador.
     * La relación determina qué estacionamientos puede gestionar ese operador.
     *
     * @param id Identificador del estacionamiento.
     * @param operatorId Identificador del usuario operador a asignar.
     * @return `Result.Success` con el estacionamiento actualizado o
     *   `Result.Error` si el operador no es válido o falla la petición.
     */
    suspend fun assignOperator(id: Long, operatorId: Long): Result<ParkingLotDto>

    /**
     * Desactiva un estacionamiento.
     *
     * La implementación usa `DELETE` sobre la ruta administrativa; en esta
     * versión el backend aplica una baja lógica (`isActive = false`) y no borra
     * la fila. Las reservas existentes y las reglas asociadas las resuelve el
     * servidor.
     *
     * @param id Identificador del estacionamiento.
     * @return `Result.Success(Unit)` si se desactiva; `Result.Error` si no
     *   existe, tiene restricciones o falla la red.
     */
    suspend fun deactivateParkingLot(id: Long): Result<Unit>

    /**
     * Lista los usuarios registrados.
     *
     * La respuesta puede incluir clientes, operadores y administradores; la
     * UI decide cómo filtrarlos o presentarlos.
     *
     * @return `Result.Success` con la lista de `UserDto` o `Result.Error` si
     *   falla la consulta.
     */
    suspend fun getUsers(): Result<List<UserDto>>

    /**
     * Activa o desactiva una cuenta de usuario.
     *
     * Es una baja lógica: el backend cambia `isActive` y normalmente impide
     * que una cuenta inactiva inicie sesión. No elimina sus datos históricos.
     *
     * @param id Identificador del usuario.
     * @param isActive `true` para activar, `false` para desactivar.
     * @return `Result.Success` con el usuario actualizado o `Result.Error` si
     *   la operación no está permitida o falla la red.
     */
    suspend fun updateUserStatus(id: Long, isActive: Boolean): Result<UserDto>
}
