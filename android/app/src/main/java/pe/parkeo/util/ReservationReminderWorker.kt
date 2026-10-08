package pe.parkeo.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Muestra una notificación local: recordatorio de inicio o de fin de una reserva.
 * No necesita Firebase ni servidor.
 */
class ReservationReminderWorker(
    private val appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    @SuppressLint("MissingPermission")
    override fun doWork(): Result {
        val kind = inputData.getString(KEY_KIND) ?: return Result.failure()
        val reservationId = inputData.getLong(KEY_RESERVATION_ID, 0L)
        val lotName = inputData.getString(KEY_LOT_NAME)?.ifBlank { null } ?: "tu estacionamiento"

        // Android 13+: sin permiso no se puede mostrar la notificación
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        createChannel()

        val (title, text) = when (kind) {
            KIND_START -> "Tu reserva empieza pronto" to
                    "Tu espacio en $lotName te espera. Sal con tiempo."
            KIND_END -> "Tu tiempo está por terminar" to
                    "Tu reserva en $lotName termina en unos minutos. Retira tu vehículo a tiempo."
            else -> return Result.failure()
        }

        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // cámbialo por tu propio ícono monocromo
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = (reservationId * 2 + if (kind == KIND_END) 1 else 0).toInt()
        NotificationManagerCompat.from(appContext).notify(notificationId, notification)
        return Result.success()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de reservas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Avisos de inicio y fin de tus reservas" }
            val manager = appContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "reservation_reminders"
        const val KEY_KIND = "kind"
        const val KEY_RESERVATION_ID = "reservation_id"
        const val KEY_LOT_NAME = "lot_name"
        const val KIND_START = "START"
        const val KIND_END = "END"
    }
}