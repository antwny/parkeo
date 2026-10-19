/*
 * PARKeo · Android
 * Archivo: SplashScreen.kt
 * Capa: presentación (pantalla de inicio del flujo).
 * Propósito: mostrar la identidad de PARKeo durante el arranque, esperar la lectura de la
 * sesión y decidir si corresponde onboarding o home.
 * Conceptos clave: Composable, StateFlow, collectAsState, estado nullable, LaunchedEffect,
 * animaciones con animateFloatAsState, tween, delay, decisión de navegación y composición
 * declarativa.
 * Flujo: el SplashViewModel expone `isLoggedIn` y `onboardingShown`; la pantalla espera a
 * que ambos tengan valor, anima la marca y, tras 1800 ms, llama al callback de navegación.
 * Relación con otros archivos: ParkeoNavGraph.kt crea el ViewModel y conecta los callbacks;
 * SplashViewModel.kt lee SessionDataStore; ParkeoTheme aporta colores y formas.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones de Compose, estado y flujos).
 */
package pe.parkeo.ui.screens.splash

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.SplashViewModel

/**
 * Pantalla de arranque de PARKeo.
 *
 * La pantalla es declarativa: no guarda la sesión ni consulta DataStore directamente.
 * Observa dos `StateFlow` del ViewModel y los convierte en estado de composición con
 * `collectAsState`. Ambos flujos empiezan con `null` (aún no leído) y luego emiten `true` o
 * `false`; ese `null` es la señal de "todavía no decidir". Cuando ambos tienen valor, la
 * pantalla espera 1800 ms para que la animación sea visible y llama al callback que
 * corresponde.
 *
 * La decisión actual solo mira `isLoggedIn`: si hay sesión va a home; si no, a onboarding.
 * `onboardingShown` se observa para garantizar que la lectura inicial terminó, pero no se
 * usa para elegir entre onboarding y login. Por eso, si el onboarding ya se vio pero no hay
 * sesión, se vuelve a mostrar. El registro de "onboarding visto" lo realiza
 * `OnboardingViewModel` cuando el usuario termina esa pantalla.
 *
 * @param viewModel ViewModel que expone la sesión y el indicador de onboarding mediante
 * `StateFlow`. El grafo lo crea con `ParkeoViewModelFactory`.
 * @param onNavigateToOnboarding callback que el grafo traduce a la ruta de onboarding.
 * @param onNavigateToHome callback que el grafo traduce a la ruta de home.
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToOnboarding: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    // Estado observado: null significa que DataStore aún no ha entregado un valor.
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val onboardingShown by viewModel.onboardingShown.collectAsState()

    // Bandera local que dispara la transición visual de entrada.
    var startAnimation by remember { mutableStateOf(false) }

    // Opacidad: de 0 a 1 en 800 ms con una curva suave.
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    // Escala: de 0.92 a 1.0 para dar una sensación de acercamiento discreta.
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.92f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "scale"
    )

    // `LaunchedEffect(Unit)` se ejecuta una vez al entrar en composición; cambiar la
    // bandera provoca que las animaciones avancen hacia su valor final.
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    // Se reinicia si cambia cualquiera de las dos lecturas. La condición evita navegar
    // mientras alguno de los flujos siga en null. `delay` es suspendido y se cancela si la
    // pantalla sale de composición antes de cumplirse el tiempo.
    LaunchedEffect(isLoggedIn, onboardingShown) {
        if (isLoggedIn != null && onboardingShown != null) {
            delay(1800)
            if (isLoggedIn == true) {
                onNavigateToHome()
            } else {
                onNavigateToOnboarding()
            }
        }
    }

    // Fondo a pantalla completa: el splash usa el obsidiana de la marca sin Scaffold.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ParkeoObsidian),
        contentAlignment = Alignment.Center
    ) {
        // Marca central: se anima como un solo bloque de opacidad y escala.
        Column(
            modifier = Modifier
                .alpha(alphaAnim)
                .scale(scaleAnim),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Wordmark "Parkeo" con el punto verde característico alineado abajo.
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Parkeo",
                    color = ParkeoTextWhite,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.5).sp
                )
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp, bottom = 8.dp)
                        .size(10.dp)
                        .background(ParkeoLime, CircleShape)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Lema en mayúsculas con espaciado amplio para el estilo técnico de la marca.
            Text(
                text = "MOVILIDAD URBANA EN TIEMPO REAL",
                color = ParkeoTextGrayMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        // Indicador inferior de marca: una píldora sutil que refuerza la identidad.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .width(40.dp)
                .height(3.dp)
                .background(ParkeoLime.copy(alpha = 0.4f), ParkeoPillShape)
        )
    }
}
