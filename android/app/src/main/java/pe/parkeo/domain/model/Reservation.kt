/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/domain/model/Reservation.kt
 * Capa: dominio (modelo de dominio y enumeración de estados).
 * Propósito: describir una reserva de estacionamiento tal como la entiende el
 * negocio: qué cochera, qué espacio, qué vehículo, en qué intervalo de tiempo,
 * en qué estado y a qué precio. Incluye la traducción de los estados a textos
 * en español para la interfaz.
 * Conceptos clave:
 *   - Modelo de dominio: objeto inmutable e independiente de Retrofit,
 *     DataStore o Compose.
 *   - DTO frente a modelo: `ReservationDto` conserva los nombres y tipos del
 *     JSON (por ejemplo `status: String`); `Reservation` usa una enumeración
 *     tipada y solo los campos que la app necesita. El mapeo debería ocurrir en
 *     la capa de datos, aunque en esta versión del proyecto las interfaces de
 *     repositorio todavía devuelven DTO.
 *   - Nulabilidad: `totalPrice` puede no estar calculado; los demás campos del
 *     modelo se asumen presentes.
 *   - Estado derivado: `displayName()` transforma el estado técnico en texto
 *     legible; no cambia el estado real ni llama al backend.
 * Flujo: backend -> JSON -> `ReservationDto` -> [mapeo previsto] ->
 * `Reservation` -> ViewModel -> pantalla. El backend calcula el precio, valida
 * conflictos de horario y hace las transiciones de estado; la app solo muestra
 * y solicita acciones.
 * Relación: `ReservationRepositoryImpl` y las APIs de usuario/operador
 * obtienen los datos remotos. `AppContainer` ensambla el repositorio y los
 * ViewModels consumen el resultado mediante `Result`.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.domain.model

/**
 * Reserva de un espacio de estacionamiento para un vehículo y un intervalo.
 *
 * El backend es la fuente de verdad: valida que el vehículo pertenezca al
 * usuario, que no tenga otra reserva superpuesta, que el espacio esté libre y
 * que el horario respete los límites. Este modelo resume la respuesta para que
 * la pantalla pueda listar y detallar reservas sin conocer el JSON.
 *
 * Todos los campos provienen del backend; este modelo no agrega datos locales,
 * salvo la interpretación tipada de `status` y el texto que produce
 * `displayName()`.
 *
 * @param id Identificador de la reserva asignado por el backend.
 * @param parkingLotName Nombre del estacionamiento desnormalizado en la
 *   respuesta para evitar una consulta adicional desde la lista.
 * @param parkingLotAddress Dirección del estacionamiento, también
 *   desnormalizada y útil para mostrar el detalle.
 * @param spaceNumber Número o código visible del espacio reservado (por
 *   ejemplo "A-12"). Puede quedar vacío si el backend aún no lo asigna.
 * @param vehicleLicensePlate Placa del vehículo reservado, tal como la
 *   registró el usuario y normalizó el backend.
 * @param startTime Inicio de la reserva como texto. Se infiere formato
 *   ISO-8601 local (por ejemplo `2026-10-19T14:30:00`, sin zona horaria)
 *   porque el backend usa `LocalDateTime`; la app no lo convierte aquí.
 * @param endTime Fin de la reserva, con el mismo formato que `startTime`.
 *   Debe ser posterior a `startTime`; esa regla se valida en el servidor.
 * @param status Estado actual tipado. Ver [ReservationStatus] para el
 *   significado de cada valor.
 * @param totalPrice Monto total calculado por el backend a partir de la tarifa
 *   y la duración. Es nullable porque una reserva recién creada o un cálculo
 *   pendiente podría no traerlo; además, el DTO admite `totalAmount` o
 *   `totalPrice` como nombres alternativos.
 */
data class Reservation(
    val id: Long,
    val parkingLotName: String,
    val parkingLotAddress: String,
    val spaceNumber: String,
    val vehicleLicensePlate: String,
    val startTime: String,
    val endTime: String,
    val status: ReservationStatus,
    val totalPrice: Double?
)

/**
 * Estados posibles del ciclo de vida de una reserva.
 *
 * El backend envía el estado como texto (`String`) dentro del DTO; convertirlo
 * a esta enumeración permite que `when` sea exhaustivo y evita errores de
 * tipeo. Las transiciones válidas las decide el servidor, no la app. De forma
 * general:
 * - `PENDING`: creada, a la espera de confirmación.
 * - `CONFIRMED`: aceptada; el usuario puede presentarse.
 * - `ACTIVE`: el operador registró el ingreso del vehículo.
 * - `COMPLETED`: el operador registró la salida; el espacio se libera.
 * - `CANCELLED`: anulada por el usuario o el sistema.
 * - `NO_SHOW`: el usuario no se presentó en el horario acordado.
 *
 * Los nombres coinciden con los valores que usa el backend en sus respuestas y
 * filtros (`status`), por lo que no deben traducirse para las peticiones.
 */
enum class ReservationStatus {
    PENDING, CONFIRMED, ACTIVE, COMPLETED, CANCELLED, NO_SHOW;

    /**
     * Devuelve la etiqueta en español que se muestra al usuario.
     *
     * Es una traducción puramente de presentación: no modifica el estado, no
     * consulta la red y no debe usarse para comparar o enviar filtros. Para
     * lógica o para la API se usa el nombre técnico de la constante.
     *
     * @return Texto legible para la UI correspondiente a cada constante; si se
     *   agrega un estado nuevo, el `when` obliga a actualizar esta función.
     */
    fun displayName(): String = when (this) {
        PENDING -> "Pendiente"
        CONFIRMED -> "Confirmada"
        ACTIVE -> "Activa"
        COMPLETED -> "Completada"
        CANCELLED -> "Cancelada"
        NO_SHOW -> "No se presentó"
    }
}
