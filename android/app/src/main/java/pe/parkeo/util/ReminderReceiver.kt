package pe.parkeo.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/** Recibe la alarma y muestra la notificación del recordatorio. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val text = intent.getStringExtra(EXTRA_TEXT) ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

        // En Android 13+ sin permiso no se puede mostrar nada
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios de reserva",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "parkeo_reservation_reminders"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}