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

object NavigationHelper {

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

/** Botón "Cómo llegar" listo para usar en cualquier pantalla. */
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