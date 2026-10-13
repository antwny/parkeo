// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Implementación del módulo Parkeo identificado por el paquete y nombre de esta clase.
package pe.parkeo.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import pe.parkeo.ParkeoApplication
import pe.parkeo.ui.navigation.ParkeoNavGraph
import pe.parkeo.ui.viewmodel.ParkeoViewModelFactory

@Composable
/** Ejecuta la operación indicada por el nombre: parkeo app. Consulte el catálogo de métodos y conceptos en la guía maestra. */
fun ParkeoApp() {
    val context = LocalContext.current.applicationContext as ParkeoApplication
    val viewModelFactory = ParkeoViewModelFactory(context.container)
    val navController = rememberNavController()

    ParkeoNavGraph(
        viewModelFactory = viewModelFactory,
        navController = navController
    )
}
