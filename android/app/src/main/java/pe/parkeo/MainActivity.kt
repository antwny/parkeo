// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Punto de entrada o inicialización del proceso; integra la configuración principal de la aplicación.
package pe.parkeo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pe.parkeo.ui.ParkeoApp
import pe.parkeo.ui.theme.ParkeoTheme

class MainActivity : ComponentActivity() {
    /** Gestiona el evento de ciclo de vida o interacción indicado: on create. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParkeoTheme {
                ParkeoApp()
            }
        }
    }
}
