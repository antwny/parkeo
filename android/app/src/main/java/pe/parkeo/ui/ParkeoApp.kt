/*
 * PARKeo · Android
 * Archivo: ParkeoApp.kt
 * Capa: presentación (raíz de la composición y host de navegación).
 * Propósito: conectar el contenedor de dependencias con el grafo de navegación y crear el
 * NavHostController que controla el back stack.
 * Conceptos clave: Composable raíz, LocalContext, contexto de aplicación, ViewModelFactory,
 * rememberNavController, NavHost, estado de navegación y tema raíz.
 * Flujo: MainActivity llama a ParkeoApp dentro de ParkeoTheme; aquí se recupera
 * ParkeoApplication, se crea la factory de ViewModels y se recuerda el NavController.
 * Después ParkeoNavGraph declara los destinos y las pantallas.
 * Relación con otros archivos: MainActivity.kt aplica el tema; ParkeoApplication.kt aporta
 * el contenedor; ParkeoNavGraph.kt define el grafo; ViewModelFactory.kt construye los
 * ViewModels con repositorios del contenedor.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones de Compose, estado y DI).
 */
package pe.parkeo.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import pe.parkeo.ParkeoApplication
import pe.parkeo.ui.navigation.ParkeoNavGraph
import pe.parkeo.ui.viewmodel.ParkeoViewModelFactory

/**
 * Raíz composable de la aplicación y punto donde se ensambla la navegación.
 *
 * El tema no se declara aquí: `MainActivity` ya envuelve esta función con `ParkeoTheme`,
 * de modo que colores, tipografía y formas están disponibles para todo el árbol. Esta
 * función se ocupa de tres conexiones:
 *
 * 1. Obtener el contexto de la aplicación y comprobarlo como `ParkeoApplication` para
 *    acceder al contenedor de dependencias.
 * 2. Construir `ParkeoViewModelFactory`, que sabe crear cada ViewModel con sus repositorios.
 * 3. Crear y recordar un `NavHostController`, que es el estado de navegación y sobrevive a
 *    recomposiciones; después se entrega a `ParkeoNavGraph`.
 *
 * `LocalContext.current.applicationContext` evita retener una Activity: el contexto de
 * aplicación vive tanto como el proceso. El cast es seguro porque el manifiesto declara
 * `ParkeoApplication` como clase de aplicación. Si se cambiara esa declaración, el cast
 * fallaría en tiempo de ejecución.
 *
 * No recibe parámetros: toda la configuración se obtiene del entorno Android y del
 * contenedor. La lógica de decidir el destino inicial y los argumentos de cada pantalla
 * pertenece a `ParkeoNavGraph`.
 */
@Composable
fun ParkeoApp() {
    // Contexto global de la app: se usa para llegar al contenedor creado en Application.
    val context = LocalContext.current.applicationContext as ParkeoApplication

    // La factory traduce cada clase de ViewModel a una instancia con sus repositorios.
    val viewModelFactory = ParkeoViewModelFactory(context.container)

    // El NavController es estado de UI recordado: conserva el back stack entre
    // recomposiciones y se restaura con el estado guardado de la Activity.
    val navController = rememberNavController()

    // A partir de aquí, el grafo declara destinos, argumentos y transiciones.
    ParkeoNavGraph(
        viewModelFactory = viewModelFactory,
        navController = navController
    )
}
