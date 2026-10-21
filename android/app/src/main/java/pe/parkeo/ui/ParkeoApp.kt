package pe.parkeo.ui

import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import pe.parkeo.ParkeoApplication
import pe.parkeo.ui.navigation.NavRoutes
import pe.parkeo.ui.navigation.ParkeoNavGraph
import pe.parkeo.ui.theme.ParkeoTheme
import pe.parkeo.ui.viewmodel.ParkeoViewModelFactory
import pe.parkeo.util.SessionEvents

@Composable
fun ParkeoApp() {
    val context = LocalContext.current.applicationContext as ParkeoApplication
    val viewModelFactory = ParkeoViewModelFactory(context.container)
    val navController = rememberNavController()

    // ─── Apariencia (sistema / claro / oscuro) ────────────────────────────────
    val themeMode by context.container.sessionDataStore.themeMode.collectAsState(initial = "system")

    // ─── Expiración de sesión (emitida por el TokenAuthenticator) ─────────────
    LaunchedEffect(Unit) {
        SessionEvents.events.collect { event ->
            if (event != null) {
                Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                navController.navigate(NavRoutes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
                SessionEvents.consume()
            }
        }
    }

    ParkeoTheme(
        darkTheme = when (themeMode) {
            "light" -> false
            "dark" -> true
            else -> isSystemInDarkTheme()
        }
    ) {
        ParkeoNavGraph(
            viewModelFactory = viewModelFactory,
            navController = navController
        )
    }
}
