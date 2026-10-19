/*
 * PARKeo · Android
 * Archivo: util/RequestNotificationPermission.kt
 * Capa: utilidades transversales de interfaz (permisos en tiempo de ejecución).
 * Propósito: solicitar `POST_NOTIFICATIONS` en Android 13 o superior para que los
 * recordatorios de reserva puedan mostrarse como notificaciones.
 * Conceptos clave: permiso en runtime, `Build.VERSION.SDK_INT`, `TIRAMISU`,
 *   `Manifest.permission.POST_NOTIFICATIONS`, `rememberLauncherForActivityResult`,
 *   `ActivityResultContracts.RequestPermission`, `LaunchedEffect`, `checkSelfPermission`
 *   y `ContextCompat`.
 * Flujo: ReservationCreateScreen llama a este composable. Si el sistema es Android 13+ y
 *   el permiso no está concedido, se lanza el diálogo del sistema una vez por composición.
 *   El resultado se ignora: si el usuario niega, la app sigue funcionando sin avisos.
 * Relación: el manifiesto declara el permiso; ReservationReminderWorker y ReminderReceiver
 *   comprueban el mismo permiso antes de publicar una notificación.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones 3, 5 y 14).
 */
package pe.parkeo.util

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Pide el permiso de notificaciones cuando hace falta.
 *
 * `POST_NOTIFICATIONS` se introdujo en Android 13 (API 33, `TIRAMISU`). Antes de esa
 * versión no existe como permiso runtime: las notificaciones se controlan desde los ajustes
 * del sistema y no se debe mostrar un diálogo. Por eso la función retorna de inmediato si
 * `SDK_INT` es menor que `TIRAMISU`.
 *
 * `rememberLauncherForActivityResult` registra un contrato de actividad y devuelve un
 * lanzador estable entre recomposiciones; es la forma moderna de obtener un resultado de
 * una pantalla del sistema sin `startActivityForResult` manual. El callback está vacío a
 * propósito: una denegación no bloquea la reserva, solo impide ver recordatorios. No se
 * insiste ni se muestra una explicación adicional.
 *
 * `LaunchedEffect(Unit)` ejecuta la comprobación una vez por entrada en la composición.
 * Antes de lanzar el diálogo se consulta `ContextCompat.checkSelfPermission` para no pedir
 * un permiso ya concedido. Declarar el permiso en `AndroidManifest.xml` es necesario pero
 * no suficiente: en Android 13+ el usuario debe autorizarlo en runtime.
 *
 * @return no devuelve un valor; el efecto es abrir (o no) el diálogo del sistema.
 */
@Composable
fun RequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* si lo niega, simplemente no se mostrarán los avisos */ }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
