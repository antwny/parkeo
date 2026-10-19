/*
 * PARKeo · Android
 * Archivo: util/NavigationHelper.kt
 * Capa: utilidades transversales de interfaz (navegación externa y componente Compose).
 * Propósito: abrir la navegación hacia un estacionamiento en Google Maps y ofrecer un
 * botón reutilizable "Cómo llegar" para cualquier pantalla.
 * Conceptos clave: `Intent` implícito, `Uri`, esquema `google.navigation:`, paquete
 *   explícito de Google Maps, `FLAG_ACTIVITY_NEW_TASK`, `ActivityNotFoundException`,
 *   respaldo web, `Toast`, `Locale.US`, `@Composable`, `LocalContext`, `Modifier`.
 * Flujo: DirectionsButton recibe latitud/longitud y llama a `openDirections`; el helper
 *   intenta la app de Maps en modo navegación, luego el enlace web y, si nada está
 *   disponible, muestra un aviso.
 * Relación: se usa en ParkingDetailScreen y ReservationsScreen. No navega entre rutas
 *   internas de la app: para eso existen `NavRoutes` y `ParkeoNavGraph`. Cualquier rol
 *   puede pulsar el botón si la pantalla se lo muestra; no hay autorización involucrada.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones 3, 4 y 14).
 */
package pe.parkeo.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import pe.parkeo.ui.components.ParkeoButton
import pe.parkeo.ui.components.ParkeoButtonStyle
import pe.parkeo.ui.theme.Dimens
import java.util.Locale

/**
 * Utilidades para salir de la app hacia una aplicación de mapas.
 *
 * Se mantiene como `object` porque no guarda estado: solo agrupa funciones puras de
 * intención. La decisión de qué app abrir la resuelve Android; aquí se ofrecen dos
 * alternativas ordenadas por conveniencia.
 */
object NavigationHelper {

    /**
     * Abre indicaciones desde la ubicación actual hasta las coordenadas dadas.
     *
     * Estrategia:
     * 1. Construye la URI `google.navigation:q=lat,lon` y la limita al paquete
     *    `com.google.android.apps.maps`. Es el modo navegación de Google Maps.
     * 2. Si Maps no está instalado (o no puede atender la URI), captura
     *    `ActivityNotFoundException` y abre `https://www.google.com/maps/dir/?api=1...`
     *    con cualquier navegador disponible.
     * 3. Si tampoco hay navegador, muestra un `Toast` explicativo.
     *
     * Las coordenadas se formatean con `Locale.US` para que el separador decimal sea un
     * punto; con una configuración regional que usa coma, la URI quedaría ambigua o rota.
     * Se añade `FLAG_ACTIVITY_NEW_TASK` porque el `Context` podría no ser una Activity (por
     * ejemplo, el contexto de aplicación) y Android exige esa bandera en ese caso.
     *
     * @param context contexto utilizado para iniciar la Activity; normalmente el de la
     *   pantalla actual.
     * @param latitude latitud en grados decimales.
     * @param longitude longitud en grados decimales.
     * @return no devuelve un valor; el efecto es abrir otra app o mostrar un aviso.
     */
    fun openDirections(context: Context, latitude: Double, longitude: Double) {
        // Locale.US para que el decimal sea punto (-12.09) y no coma (-12,09)
        val coords = String.format(Locale.US, "%.6f,%.6f", latitude, longitude)

        // 1) App de Google Maps en modo navegación
        val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$coords")).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(mapsIntent)
            return
        } catch (e: ActivityNotFoundException) {
            // Google Maps no está instalado: se intenta con el navegador
        }

        // 2) Respaldo: enlace web de Google Maps
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$coords")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        try {
            context.startActivity(webIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No hay ninguna app para abrir el mapa", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Botón "Cómo llegar" listo para usar en cualquier pantalla.
 *
 * Obtiene el contexto de Compose y delega en `NavigationHelper.openDirections`. El estilo
 * secundario y el icono de direcciones mantienen la identidad visual frente a la acción
 * principal de cada pantalla. El `modifier` permite que quien lo usa ajuste tamaño o
 * márgenes; por defecto ocupa el ancho disponible con la altura estándar de botón.
 *
 * @param latitude latitud del destino.
 * @param longitude longitud del destino.
 * @param modifier modificador de Compose aplicado al botón; por defecto ancho completo y
 *   altura `Dimens.buttonHeightDefault`.
 */
@Composable
fun DirectionsButton(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(Dimens.buttonHeightDefault)
) {
    val context = LocalContext.current
    ParkeoButton(
        text = "Cómo llegar",
        onClick = { NavigationHelper.openDirections(context, latitude, longitude) },
        leadingIcon = Icons.Filled.Directions,
        style = ParkeoButtonStyle.Secondary,
        modifier = modifier
    )
}
