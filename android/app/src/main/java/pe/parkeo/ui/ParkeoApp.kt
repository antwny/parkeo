package pe.parkeo.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import pe.parkeo.ParkeoApplication
import pe.parkeo.ui.navigation.ParkeoNavGraph
import pe.parkeo.ui.viewmodel.ParkeoViewModelFactory

@Composable
fun ParkeoApp() {
    val context = LocalContext.current.applicationContext as ParkeoApplication
    val viewModelFactory = ParkeoViewModelFactory(context.container)
    val navController = rememberNavController()

    ParkeoNavGraph(
        viewModelFactory = viewModelFactory,
        navController = navController
    )
}
