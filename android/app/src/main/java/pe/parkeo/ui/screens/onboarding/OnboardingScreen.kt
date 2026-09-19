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

data class OnboardingPage(
    val icon: ImageVector,
    val badge: String,
    val title: String,
    val description: String
)

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinish: () -> Unit
) {
    val extended = ParkeoTheme.colors
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

    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    fun finish() {
        viewModel.markOnboardingShown()
        onFinish()
    }

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
            // Header: Brand & Skip Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingLg, vertical = Dimens.spacingMd),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Minimal Brand Wordmark
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

            // Pager Content
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

            // Sleek Line/Bar Step Indicators
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

            // Bottom CTA
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
        // Icon Vessel with subtle radial halo & obsidian surface
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

        // Technical Badge
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

        // Headline
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

        // Subtitle
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = extendedColors.textSecondary,
            lineHeight = 22.sp
        )
    }
}
