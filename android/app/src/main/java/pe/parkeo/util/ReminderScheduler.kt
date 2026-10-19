/*
 * PARKeo · Android
 * Archivo: util/ReminderScheduler.kt
 * Capa: utilidades transversales (programación de recordatorios locales).
 * Propósito: programar dos avisos por reserva —uno antes del inicio y otro antes del
 * final— usando WorkManager, y cancelarlos cuando la reserva se cancela.
 * Conceptos clave: WorkManager, `OneTimeWorkRequest`, `setInitialDelay`, `workDataOf`,
 *   trabajo único (`enqueueUniqueWork`), `ExistingWorkPolicy.REPLACE`, `cancelUniqueWork`,
 *   `LocalDateTime`, `ZoneId`, `Duration`, `TimeUnit` y cálculo de tiempos.
 * Flujo: ReservationCreateScreen llama a `schedule` después de crear la reserva;
 *   ReservationsScreen llama a `cancel` cuando el ViewModel confirma la cancelación.
 *   WorkManager ejecuta ReservationReminderWorker en el momento aproximado y este publica
 *   la notificación.
 * Relación: no usa AlarmManager directamente. `ReminderReceiver` es un BroadcastReceiver
 *   alternativo para alarmas, pero el flujo activo de esta app es WorkManager -> Worker.
 *   Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones 3, 5 y 14).
 */
package pe.parkeo.util

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Programador de recordatorios de reserva.
 *
 * Se apoya en WorkManager porque el trabajo debe sobrevivir a que la app pase a segundo
 * plano e, idealmente, a reinicios del dispositivo. A diferencia de `AlarmManager`, que
 * dispara una alarma exacta y requiere permisos especiales desde Android 12 para ser
 * exacta, WorkManager prioriza garantías de ejecución y puede retrasar el aviso por Doze,
 * ahorro de energía o políticas del fabricante. Para un recordatorio "unos minutos antes"
 * ese retraso es aceptable; no lo sería para una alarma crítica al segundo.
 *
 * El programador usa la zona `America/Lima` de forma explícita para que el cálculo no
 * dependa de la zona configurada en el teléfono. Las cadenas ISO del backend se interpretan
 * como hora local de Lima, sin offset.
 */
object ReminderScheduler {

    /* Zona horaria de referencia del negocio; evita depender de la zona del dispositivo. */
    private val LIMA: ZoneId = ZoneId.of("America/Lima")

    /* Minutos de anticipación del aviso de inicio y del aviso de fin. */
    private const val START_MINUTES_BEFORE = 15L
    private const val END_MINUTES_BEFORE = 10L

    /**
     * Programa los recordatorios de inicio y fin de una reserva.
     *
     * Convierte las dos cadenas ISO a `LocalDateTime`. Si alguna no se puede interpretar,
     * aborta sin programar nada y sin lanzar excepción: no tiene sentido avisar de una
     * reserva cuyas horas no se entienden. Para cada extremo calcula:
     * - `fireAt`: instante del aviso (15 minutos antes del inicio, 10 antes del fin).
     * - `deadline`: instante de la reserva que el aviso anticipa (inicio o fin).
     *
     * `enqueue` decide si el aviso aún tiene sentido según la hora actual. Los dos trabajos
     * son independientes y tienen nombres únicos por tipo y reserva, de modo que programar
     * de nuevo la misma reserva reemplaza el trabajo anterior en vez de duplicarlo.
     *
     * @param context contexto de aplicación usado para obtener WorkManager.
     * @param reservationId identificador de la reserva; forma parte del nombre único y de
     *   los datos de entrada del worker.
     * @param parkingLotName nombre visible del estacionamiento para el texto del aviso.
     * @param startIso inicio de la reserva en ISO local de Lima
     *   (`yyyy-MM-dd'T'HH:mm:ss`).
     * @param endIso fin de la reserva en el mismo formato.
     * @return no devuelve un valor; el trabajo queda encolado en WorkManager.
     */
    fun schedule(
        context: Context,
        reservationId: Long,
        parkingLotName: String,
        startIso: String,
        endIso: String
    ) {
        val start = parse(startIso) ?: return
        val end = parse(endIso) ?: return
        val now = LocalDateTime.now(LIMA)

        enqueue(
            context, reservationId, parkingLotName,
            kind = ReservationReminderWorker.KIND_START,
            fireAt = start.minusMinutes(START_MINUTES_BEFORE),
            deadline = start,
            now = now
        )
        enqueue(
            context, reservationId, parkingLotName,
            kind = ReservationReminderWorker.KIND_END,
            fireAt = end.minusMinutes(END_MINUTES_BEFORE),
            deadline = end,
            now = now
        )
    }

    /** Cancela los dos recordatorios (usar al cancelar la reserva).
     *
     * Cada trabajo tiene un nombre único derivado del tipo y del id. `cancelUniqueWork`
     * cancela tanto el trabajo pendiente como el que estuviera ejecutándose. Si la reserva
     * ya había terminado y los trabajos ya se ejecutaron, la llamada no falla: simplemente
     * no hay nada que cancelar.
     *
     * @param context contexto usado para obtener WorkManager.
     * @param reservationId reserva cuyos recordatorios se quieren apagar.
     * @return no devuelve un valor.
     */
    fun cancel(context: Context, reservationId: Long) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(workName(ReservationReminderWorker.KIND_START, reservationId))
        workManager.cancelUniqueWork(workName(ReservationReminderWorker.KIND_END, reservationId))
    }

    /**
     * Encola un único trabajo con retraso inicial.
     *
     * Reglas de tiempo:
     * - Si `deadline` ya pasó (`!deadline.isAfter(now)`), no se programa nada: el aviso
     *   llegaría después del momento que debía anticipar.
     * - Si `fireAt` ya pasó pero `deadline` sigue en el futuro, el retraso se fuerza a cero
     *   con `coerceAtLeast(0L)` y el aviso se intenta de inmediato.
     * - En otro caso, el retraso es la diferencia entre `now` y `fireAt` en milisegundos.
     *
     * `OneTimeWorkRequestBuilder` construye un trabajo que se ejecuta una sola vez.
     * `setInputData` transporta datos simples (tipo, id y nombre) al worker; no se pasan
     * objetos complejos. `enqueueUniqueWork` con `REPLACE` garantiza que no queden dos
     * avisos del mismo tipo para la misma reserva si se reprograma.
     *
     * Limitación de Android 12+: el retraso es una solicitud, no una promesa exacta. El
     * sistema puede posponer la ejecución por Doze, restricciones de batería o cuotas de
     * trabajo. Para alarmas exactas haría falta `AlarmManager` con permiso
     * `SCHEDULE_EXACT_ALARM`, que este flujo no utiliza.
     *
     * @param context contexto para obtener WorkManager.
     * @param reservationId identificador de la reserva.
     * @param parkingLotName nombre del estacionamiento para el aviso.
     * @param kind `START` o `END`, según el extremo de la reserva.
     * @param fireAt instante en que se desea el aviso.
     * @param deadline instante de la reserva que se anticipa.
     * @param now hora actual en la zona de Lima, calculada una vez por `schedule`.
     * @return no devuelve un valor.
     */
    private fun enqueue(
        context: Context,
        reservationId: Long,
        parkingLotName: String,
        kind: String,
        fireAt: LocalDateTime,
        deadline: LocalDateTime,
        now: LocalDateTime
    ) {
        // Si el momento clave ya pasó, no tiene sentido avisar
        if (!deadline.isAfter(now)) return

        // Si ya pasó la hora del aviso pero aún no el momento clave, avisa de inmediato
        val delayMillis = Duration.between(now, fireAt).toMillis().coerceAtLeast(0L)

        val request = OneTimeWorkRequestBuilder<ReservationReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ReservationReminderWorker.KEY_KIND to kind,
                    ReservationReminderWorker.KEY_RESERVATION_ID to reservationId,
                    ReservationReminderWorker.KEY_LOT_NAME to parkingLotName
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(kind, reservationId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /**
     * Construye el nombre único de un trabajo.
     *
     * Incluye el tipo en minúsculas y el id de la reserva, por ejemplo
     * `reservation-reminder-start-42`. Este nombre es la clave que permite reemplazar o
     * cancelar sin afectar a otras reservas.
     *
     * @param kind `START` o `END`.
     * @param reservationId identificador de la reserva.
     * @return nombre único del trabajo.
     */
    private fun workName(kind: String, reservationId: Long) =
        "reservation-reminder-${kind.lowercase()}-$reservationId"

    /**
     * Interpreta una fecha/hora ISO sin offset.
     *
     * `LocalDateTime.parse` espera el formato `yyyy-MM-dd'T'HH:mm:ss` (puede aceptar
     * fracciones de segundo). Si la cadena trae offset o zona, o está mal formada, se
     * devuelve `null` y el llamador decide abortar. Se captura `Exception` porque el texto
     * proviene del backend y no se quiere romper la pantalla por un dato inválido.
     *
     * @param iso texto de fecha/hora enviado por el backend.
     * @return `LocalDateTime` interpretado o `null` si no es válido.
     */
    private fun parse(iso: String): LocalDateTime? =
        try {
            LocalDateTime.parse(iso)
        } catch (e: Exception) {
            null
        }
}
