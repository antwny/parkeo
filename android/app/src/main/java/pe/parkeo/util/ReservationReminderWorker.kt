/*
 * PARKeo · Android
 * Archivo: util/ReservationReminderWorker.kt
 * Capa: utilidades transversales (Worker de WorkManager).
 * Propósito: ejecutar en segundo plano la publicación de un recordatorio local de inicio o
 * fin de reserva, sin Firebase ni servidor.
 * Conceptos clave: WorkManager, `Worker`, `WorkerParameters`, `doWork`, `inputData`,
 *   `Result.success/failure`, `NotificationManagerCompat`, `NotificationChannel`,
 *   `PendingIntent` inmutable, `POST_NOTIFICATIONS` y `@SuppressLint`.
 * Flujo: ReminderScheduler encola un `OneTimeWorkRequest` con retraso; WorkManager crea
 *   este Worker, que lee el tipo y los datos de la reserva, comprueba permiso, crea el canal
 *   y publica la notificación. Al tocar la notificación se abre la app mediante un
 *   `PendingIntent`.
 * Relación: es el camino activo de recordatorios. `ReminderScheduler` programa/cancela;
 *   `ReminderReceiver` es un receptor alternativo basado en broadcasts/AlarmManager.
 *   Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones 3, 5 y 14).
 */
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
 *
 * `Worker` es la variante síncrona de WorkManager: `doWork` se ejecuta en un hilo de fondo
 * proporcionado por la librería, por lo que no se debe llamar a la UI directamente. Aquí el
 * trabajo es corto (leer datos, crear canal y notificar), así que no hace falta
 * `CoroutineWorker`.
 *
 * El tipo de retorno `Result` de `doWork` corresponde al resultado de WorkManager
 * (`androidx.work.ListenableWorker.Result`), no al `Result` sellado de `pe.parkeo.util`.
 * `success` indica que el trabajo terminó; `failure` que no tiene sentido reintentarlo.
 *
 * @param appContext contexto de aplicación entregado por WorkManager.
 * @param params parámetros de ejecución, incluidos `id` y `inputData` del trabajo.
 */
class ReservationReminderWorker(
    private val appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    /*
     * Se suprime la advertencia de permiso faltante porque el permiso se comprueba
     * explícitamente justo después de leer los datos. La anotación no cambia el
     * comportamiento: solo evita que el lint estático marque `notify` como peligroso.
     */
    @SuppressLint("MissingPermission")
    /**
     * Ejecuta el recordatorio.
     *
     * 1. Lee `KEY_KIND`, `KEY_RESERVATION_ID` y `KEY_LOT_NAME`. Si falta el tipo, devuelve
     *    `failure` porque el trabajo no se puede interpretar. Si el nombre está en blanco,
     *    usa "tu estacionamiento" como texto genérico.
     * 2. En Android 13+ comprueba `POST_NOTIFICATIONS`. Si no está concedido, devuelve
     *    `success` (no `failure`): no es un error reintentable, simplemente el usuario no
     *    verá el aviso.
     * 3. Crea el canal (Android 8+) y elige título/texto según `START` o `END`. Un tipo
     *    desconocido devuelve `failure`.
     * 4. Construye un `PendingIntent` hacia la pantalla de lanzamiento de la app. Se usan
     *    `FLAG_UPDATE_CURRENT` para actualizar el contenido si ya existía y
     *    `FLAG_IMMUTABLE` porque Android 12+ exige declarar la mutabilidad.
     * 5. Publica la notificación con prioridad alta, texto expandible y autocancelación.
     *
     * El id de notificación se calcula como `reservationId * 2` para el inicio y
     * `reservationId * 2 + 1` para el fin. Así ambos avisos de una misma reserva no se
     * pisan y reservas distintas tampoco colisionan en el caso habitual. La conversión a
     * `Int` es la que exige la API de notificaciones.
     *
     * WorkManager puede retrasar la ejecución por Doze, ahorro de energía o cuotas; el
     * aviso no es una alarma exacta. Para hora exacta se usaría `AlarmManager` con permiso
     * `SCHEDULE_EXACT_ALARM`, que este flujo no solicita.
     *
     * @return `Result.success()` si se publicó o si no había permiso; `Result.failure()` si
     *   faltan datos o el tipo es desconocido. No se usa `Result.retry()`.
     */
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

        /* Texto distinto según el extremo de la reserva que se aproxima. */
        val (title, text) = when (kind) {
            KIND_START -> "Tu reserva empieza pronto" to
                    "Tu espacio en $lotName te espera. Sal con tiempo."
            KIND_END -> "Tu tiempo está por terminar" to
                    "Tu reserva en $lotName termina en unos minutos. Retira tu vehículo a tiempo."
            else -> return Result.failure()
        }

        /*
         * `getLaunchIntentForPackage` devuelve el Intent que abre la actividad principal de
         * la app. El PendingIntent permite que la notificación abra esa pantalla cuando el
         * usuario la toca, sin que el Worker tenga que seguir vivo.
         */
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

    /**
     * Crea el canal de notificaciones si el sistema lo requiere.
     *
     * Desde Android 8 (`OREO`) toda notificación debe pertenecer a un canal; el usuario
     * controla por canal la importancia y el sonido. La importancia `HIGH` hace que el
     * recordatorio aparezca como aviso destacado. La descripción ayuda al usuario a
     * entender el propósito en los ajustes del sistema. En versiones anteriores no se crea
     * canal y la notificación funciona igual.
     *
     * @return no devuelve un valor.
     */
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

    /**
     * Claves de entrada y constantes del canal.
     *
     * `KEY_KIND`, `KEY_RESERVATION_ID` y `KEY_LOT_NAME` deben coincidir exactamente con las
     * claves que escribe `ReminderScheduler` en `workDataOf`. `KIND_START` y `KIND_END` son
     * los dos valores aceptados por `doWork`. `CHANNEL_ID` identifica el canal creado en
     * `createChannel` y usado por el builder.
     */
    companion object {
        const val CHANNEL_ID = "reservation_reminders"
        const val KEY_KIND = "kind"
        const val KEY_RESERVATION_ID = "reservation_id"
        const val KEY_LOT_NAME = "lot_name"
        const val KIND_START = "START"
        const val KIND_END = "END"
    }
}
