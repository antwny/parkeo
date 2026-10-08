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


object ReminderScheduler {

    private val LIMA: ZoneId = ZoneId.of("America/Lima")
    private const val START_MINUTES_BEFORE = 15L
    private const val END_MINUTES_BEFORE = 10L

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

    /** Cancela los dos recordatorios (usar al cancelar la reserva). */
    fun cancel(context: Context, reservationId: Long) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(workName(ReservationReminderWorker.KIND_START, reservationId))
        workManager.cancelUniqueWork(workName(ReservationReminderWorker.KIND_END, reservationId))
    }

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

    private fun workName(kind: String, reservationId: Long) =
        "reservation-reminder-${kind.lowercase()}-$reservationId"

    private fun parse(iso: String): LocalDateTime? =
        try {
            LocalDateTime.parse(iso)
        } catch (e: Exception) {
            null
        }
}