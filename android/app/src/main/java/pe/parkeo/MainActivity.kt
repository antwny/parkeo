/*
 * PARKeo · Android
 * Archivo: MainActivity.kt
 * Capa: entrada (punto de entrada de la interfaz de usuario).
 * Propósito: ser la única Activity de la aplicación y entregar el control a Jetpack Compose.
 * Conceptos clave: Activity, ComponentActivity, ciclo de vida, onCreate, savedInstanceState,
 * edge-to-edge, setContent, composición inicial, tema raíz y separación entre UI y estado.
 * Flujo: Android lanza esta Activity desde el manifiesto; onCreate configura la ventana y
 * setContent publica el árbol Compose que empieza con ParkeoTheme y continúa en ParkeoApp,
 * el cual crea el NavHost y delega el estado a los ViewModels.
 * Relación con otros archivos: AndroidManifest.xml la declara como activity de launcher;
 * ParkeoApp.kt contiene el host de navegación; ParkeoNavGraph.kt define los destinos;
 * ParkeoTheme envuelve toda la interfaz y los ViewModels conservan el estado de pantalla.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones de Android/Kotlin y Compose).
 */
package pe.parkeo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pe.parkeo.ui.ParkeoApp
import pe.parkeo.ui.theme.ParkeoTheme

/**
 * Actividad principal y único punto de entrada visual de PARKeo.
 *
 * Hereda de `ComponentActivity`, la clase base de AndroidX que integra el ciclo de vida y
 * permite usar `setContent` para escribir la interfaz con funciones `@Composable` en lugar
 * de inflar jerarquías XML. La Activity no guarda datos de negocio: actúa como anfitriona
 * del árbol Compose. El estado de pantalla vive en los ViewModels y el estado de navegación
 * en el `NavHostController`, de modo que una recreación por rotación no dependa de campos
 * locales de esta clase.
 */
class MainActivity : ComponentActivity() {
    /**
     * Inicializa la Activity y monta la interfaz Compose.
     *
     * `super.onCreate(savedInstanceState)` ejecuta la inicialización obligatoria de Android;
     * `savedInstanceState` contiene el estado que el sistema guardó si la Activity se recrea
     * (por ejemplo, al rotar o recuperar el proceso). Después se aplica `enableEdgeToEdge`
     * para dibujar detrás de las barras del sistema y dejar que las pantallas gestionen sus
     * insets. Finalmente, `setContent` crea la composición: `ParkeoTheme` aporta colores,
     * tipografía y formas, y `ParkeoApp` construye el contenedor de navegación.
     *
     * Esta función no decide a qué pantalla ir ni conserva credenciales: solo prepara la
     * ventana y entrega el control al árbol declarativo.
     *
     * @param savedInstanceState estado previamente guardado por el sistema, o `null` en un
     * arranque limpio. Se reenvía a la superclase para que Android restaure lo que corresponda.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permite que el contenido se extienda bajo las barras de estado y navegación.
        // Cada pantalla compensa los insets con padding o Scaffold.
        enableEdgeToEdge()

        // Punto de entrada de Compose: el tema envuelve toda la aplicación y ParkeoApp
        // construye el grafo de navegación. No se mantiene estado mutable aquí.
        setContent {
            ParkeoTheme {
                ParkeoApp()
            }
        }
    }
}
