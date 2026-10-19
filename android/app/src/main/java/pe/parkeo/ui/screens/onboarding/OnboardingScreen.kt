/*
 * PARKeo · Android
 * Archivo: OnboardingScreen.kt
 * Capa: presentación (introducción al producto antes de autenticarse).
 * Propósito: explicar las ventajas de PARKeo en tres páginas, permitir avanzar, omitir o
 * comenzar, y persistir que la introducción ya fue vista.
 * Conceptos clave: HorizontalPager, PagerState, rememberPagerState, coroutineScope,
 * estado local, animaciones de indicadores, ParkeoButton, persistencia mediante ViewModel
 * y callback de finalización.
 * Flujo: ParkeoNavGraph abre esta pantalla si splash no encontró sesión; al pulsar
 * "Omitir" o "Comenzar" se llama a `markOnboardingShown()` y luego a `onFinish`, que navega
 * a login eliminando onboarding del back stack.
 * Relación con otros archivos: OnboardingViewModel.kt guarda el indicador en
 * SessionDataStore; ParkeoNavGraph.kt conecta `onFinish`; los componentes ParkeoButton y el
 * tema aportan la presentación.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones de Compose y flujos).
 */
package pe.parkeo.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pe.parkeo.ui.components.ParkeoButton
import pe.parkeo.ui.components.ParkeoButtonStyle
import pe.parkeo.ui.theme.Dimens
import pe.parkeo.ui.theme.ParkeoPillShape
import pe.parkeo.ui.theme.ParkeoTheme
import pe.parkeo.ui.viewmodel.OnboardingViewModel

/**
 * Modelo de una página del onboarding.
 *
 * Es un `data class` inmutable: la lista de páginas se crea una vez y la UI solo lee sus
 * campos. No contiene lógica ni estado; sirve para que el contenido editorial y su icono
 * viajen juntos.
 *
 * @param icon icono vectorial que representa el beneficio de la página.
 * @param badge etiqueta corta en mayúsculas que clasifica el beneficio.
 * @param title titular principal de la página.
 * @param description texto explicativo que acompaña al titular.
 */
data class OnboardingPage(
    val icon: ImageVector,
    val badge: String,
    val title: String,
    val description: String
)

/**
 * Pantalla de introducción con tres páginas deslizables.
 *
 * El `HorizontalPager` permite cambiar de página con gesto o con el botón inferior. La
 * lista se envuelve en `remember` porque su contenido es constante; el estado del pager se
 * crea con `rememberPagerState` y el número total de páginas se calcula con una lambda.
 * La corrutina del `rememberCoroutineScope` se usa para animar el desplazamiento cuando se
 * pulsa "Siguiente".
 *
 * Al terminar u omitir se llama a `finish()`: primero se pide al ViewModel que persista el
 * indicador y después se ejecuta `onFinish`. La escritura en DataStore es asíncrona y no se
 * espera aquí; la navegación continúa de inmediato.
 *
 * @param viewModel ViewModel que persiste que el onboarding fue visto.
 * @param onFinish callback que el grafo traduce a la navegación hacia login.
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinish: () -> Unit
) {
    val extended = ParkeoTheme.colors

    // Catálogo editorial fijo: cada página asocia un icono, una etiqueta y un mensaje.
    val pages = remember {
        listOf(
            OnboardingPage(
                icon = Icons.Filled.Map,
                badge = "RED DE COCHERAS",
                title = "Encuentra tu lugar al instante",
                description = "Explora cocheras verificadas cerca de tu destino con navegación y disponibilidad en tiempo real."
            ),
            OnboardingPage(
                icon = Icons.Filled.EventAvailable,
                badge = "MONITOREO EN VIVO",
                title = "Espacios libres sin adivinar",
                description = "Conoce el estado exacto de cada cochera antes de salir. Ahorra tiempo, combustible y estrés."
            ),
            OnboardingPage(
                icon = Icons.Filled.BookmarkAdded,
                badge = "RESERVA SEGURA",
                title = "Tu espacio garantizado",
                description = "Separa tu lugar con anticipación. Llega con total tranquilidad sabiendo que tu vehículo tiene sitio."
            )
        )
    }

    // Estado del pager: página actual, desplazamiento y conteo total.
    val pagerState = rememberPagerState { pages.size }

    // Scope ligado a la composición para lanzar la animación de cambio de página.
    val scope = rememberCoroutineScope()

    /**
     * Cierra la introducción y navega al siguiente destino.
     *
     * `markOnboardingShown()` actualiza DataStore dentro de `viewModelScope`; no es una
     * función suspendida desde el punto de vista de la UI, por eso la pantalla puede llamar
     * a `onFinish` justo después. Si la app se cerrara en ese instante, la escritura podría
     * quedar pendiente; es un compromiso habitual para no bloquear la navegación.
     */
    fun finish() {
        viewModel.markOnboardingShown()
        onFinish()
    }

    // La última página cambia el texto del CTA y oculta el botón "Omitir".
    val isLastPage = pagerState.currentPage == pages.size - 1

    Scaffold(
        containerColor = extended.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera: marca a la izquierda y acción "Omitir" a la derecha (salvo en la
            // última página, donde se reserva el espacio para mantener el equilibrio).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingLg, vertical = Dimens.spacingMd),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wordmark reducido de la marca con el punto de acento.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Parkeo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = extended.textPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 2.dp, top = 6.dp)
                            .size(5.dp)
                            .background(extended.accent, CircleShape)
                    )
                }

                if (!isLastPage) {
                    TextButton(
                        onClick = { finish() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = extended.textTertiary
                        )
                    ) {
                        Text(
                            text = "Omitir",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(Modifier.width(48.dp))
                }
            }

            // Contenido deslizable: cada página ocupa el espacio restante entre la
            // cabecera y los indicadores. El gesto y el botón comparten el mismo estado.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                OnboardingPageView(
                    page = pages[pageIndex],
                    extendedColors = extended
                )
            }

            // Indicadores de paso: barras que cambian de ancho y color según la página
            // seleccionada. No son interactivas; solo comunican progreso.
            Row(
                modifier = Modifier
                    .padding(vertical = Dimens.spacingMd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val barWidth by animateDpAsState(
                        targetValue = if (isSelected) 28.dp else 8.dp,
                        animationSpec = tween(durationMillis = 250),
                        label = "indicatorWidth"
                    )
                    val barColor by animateColorAsState(
                        targetValue = if (isSelected) extended.accent else extended.surface3,
                        animationSpec = tween(durationMillis = 250),
                        label = "indicatorColor"
                    )

                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(barWidth)
                            .clip(ParkeoPillShape)
                            .background(barColor)
                    )
                }
            }

            // Acción principal inferior: "Siguiente" avanza una página y "Comenzar"
            // finaliza. En la última página también se puede llegar aquí deslizando.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingLg)
                    .padding(bottom = Dimens.spacingLg)
            ) {
                ParkeoButton(
                    text = if (isLastPage) "Comenzar" else "Siguiente",
                    onClick = {
                        if (!isLastPage) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            finish()
                        }
                    },
                    style = ParkeoButtonStyle.Primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.buttonHeightLarge)
                )
            }
        }
    }
}

/**
 * Renderiza una página individual del onboarding.
 *
 * Es una función privada de presentación: recibe el modelo y los colores extendidos para
 * evitar leer el tema global en cada detalle. El icono se coloca dentro de un halo radial
 * y una superficie circular; debajo aparecen la etiqueta, el titular y la descripción.
 *
 * @param page contenido de la página que se va a mostrar.
 * @param extendedColors paleta extendida de PARKeo (acento, superficies, textos y bordes)
 * obtenida por la pantalla contenedora.
 */
@Composable
private fun OnboardingPageView(
    page: OnboardingPage,
    extendedColors: pe.parkeo.ui.theme.ParkeoExtendedColors
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.spacingXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Contenedor del icono: halo radial tenue sobre una superficie circular con borde.
        // El icono es decorativo (contentDescription = null) porque el texto ya explica
        // el beneficio; anunciarlo duplicaría la información al lector de pantalla.
        Box(
            modifier = Modifier
                .size(130.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            extendedColors.accent.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(extendedColors.surface2)
                    .border(
                        width = Dimens.borderHairline,
                        color = extendedColors.borderSubtle,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = extendedColors.accent
                )
            }
        }

        Spacer(Modifier.height(Dimens.spacingXl))

        // Etiqueta técnica que clasifica el beneficio (red, monitoreo, reserva).
        Surface(
            color = extendedColors.surface2,
            shape = RoundedCornerShape(4.dp),
            border = androidx.compose.foundation.BorderStroke(
                Dimens.borderHairline,
                extendedColors.borderSubtle
            )
        ) {
            Text(
                text = page.badge,
                style = MaterialTheme.typography.labelSmall,
                color = extendedColors.accent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        Spacer(Modifier.height(Dimens.spacingMd))

        // Titular principal, centrado y con interlineado ajustado.
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = extendedColors.textPrimary,
            letterSpacing = (-0.5).sp,
            lineHeight = 32.sp
        )

        Spacer(Modifier.height(Dimens.spacingSm))

        // Descripción de apoyo: explica el beneficio en lenguaje del usuario.
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = extendedColors.textSecondary,
            lineHeight = 22.sp
        )
    }
}
