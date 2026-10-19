/*
 * PARKeo · Android
 * Archivo: util/ReminderReceiver.kt
 * Capa: utilidades transversales (BroadcastReceiver de notificaciones).
 * Propósito: recibir un broadcast con título, texto e id y mostrar una notificación de
 * recordatorio, comprobando antes el permiso de notificaciones.
 * Conceptos clave: `BroadcastReceiver`, `onReceive`, `Intent` explícito, extras,
 *   `AlarmManager` como posible emisor, `PendingIntent`, `NotificationManager`,
 *   `NotificationChannel`, `NotificationCompat`, Android 13+ y `POST_NOTIFICATIONS`.
 * Flujo: el receptor está declarado en AndroidManifest.xml con `exported="false"`. Un
 *   emisor interno (por ejemplo, una alarma programada con `AlarmManager` y un
 *   `PendingIntent`) puede enviarle un Intent explícito; `onReceive` valida extras y
 *   permiso y publica la notificación.
 * Relación: es una alternativa al camino activo WorkManager -> ReservationReminderWorker.
 *   En el código actual ReminderScheduler no lo invoca, pero se mantiene registrado como
 *   receptor independiente. Guía de estudio:
 *   docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones 3, 5 y 14).
 */
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

/**
 * Recibe la alarma y muestra la notificación del recordatorio.
 *
 * Un `BroadcastReceiver` es un componente de Android con vida muy corta: `onReceive` debe
 * terminar rápido y no puede abrir diálogos ni realizar trabajo largo. Por eso aquí solo se
 * leen extras, se comprueba el permiso y se publica una notificación; no se consulta la red
 * ni se accede a la base de datos local. Si se necesitara trabajo pesado, se delegaría en
 * WorkManager o en un servicio.
 *
 * El receptor no decide cuándo ejecutarse: eso lo hace quien envía el broadcast. En un
 * diseño con `AlarmManager`, se registraría una alarma con un `PendingIntent` que apunte a
 * este componente; el sistema la dispararía incluso con la app cerrada, sujeto a las
 * restricciones de alarmas exactas de Android 12+.
 */
class ReminderReceiver : BroadcastReceiver() {

    /**
     * Punto de entrada del broadcast.
     *
     * Pasos:
     * 1. Lee `EXTRA_TITLE` y `EXTRA_TEXT`; si falta alguno, termina sin notificar.
     * 2. Lee `EXTRA_NOTIFICATION_ID` (0 si no viene) para que avisos distintos puedan
     *    convivir o reemplazarse.
     * 3. En Android 13+ (`TIRAMISU`), si `POST_NOTIFICATIONS` no está concedido, retorna:
     *    el sistema bloquea la publicación y no tiene sentido construirla.
     * 4. Obtiene `NotificationManager`, crea el canal en Android 8+ y publica la
     *    notificación con prioridad alta y autocancelación al tocarla.
     *
     * El canal es obligatorio desde Android 8 (`OREO`): agrupa las notificaciones y fija su
     * importancia. Una vez creado, el usuario puede cambiar sus ajustes; volver a llamar a
     * `createNotificationChannel` con los mismos datos no lo duplica.
     *
     * @param context contexto de la aplicación que recibe el broadcast.
     * @param intent broadcast con los extras definidos en el companion.
     * @return no devuelve un valor.
     */
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

        /* Canales disponibles desde Android 8; en versiones anteriores se ignora. */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Recordatorios de reserva",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        /*
         * `setSmallIcon` usa el icono de la aplicación como aproximación. En producción se
         * recomienda un icono monocromo propio. `setAutoCancel(true)` elimina la
         * notificación cuando el usuario la toca; este receptor no define `contentIntent`.
         */
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(notificationId, notification)
    }

    /**
     * Claves y constantes compartidas con quien envía el broadcast.
     *
     * Los extras son cadenas simples porque un Intent no transporta objetos arbitrarios de
     * forma segura. `CHANNEL_ID` debe coincidir entre creación y publicación; cambiarlo
     * crea un canal nuevo. `EXTRA_NOTIFICATION_ID` permite distinguir avisos y evitar que
     * uno reemplace a otro.
     */
    companion object {
        const val CHANNEL_ID = "parkeo_reservation_reminders"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
