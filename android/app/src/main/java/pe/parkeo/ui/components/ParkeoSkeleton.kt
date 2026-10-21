package pe.parkeo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import pe.parkeo.ui.theme.Dimens
import pe.parkeo.ui.theme.ParkeoCardShape
import pe.parkeo.ui.theme.ParkeoTheme

/**
 * Placeholder shimmer para estados de carga.
 * Mantiene la estética Parkeo (superficies obsidianas/titanio).
 */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
    val colors = ParkeoTheme.colors
    background(
        brush = Brush.linearGradient(
            colors = listOf(colors.surface2, colors.surface3, colors.surface2),
            start = Offset(x = -260f + (progress * 900f), y = 0f),
            end = Offset(x = 40f + (progress * 900f), y = 0f)
        )
    )
}

private fun Modifier.skeletonBlock(): Modifier = composed {
    clip(RoundedCornerShape(6.dp)).shimmer()
}

@Composable
fun ParkeoSkeletonCard(modifier: Modifier = Modifier) {
    val extended = ParkeoTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(extended.surface1, ParkeoCardShape)
            .border(Dimens.borderHairline, extended.borderSubtle, ParkeoCardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shimmer()
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth(0.55f)
                        .height(15.dp)
                        .skeletonBlock()
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.4f)
                        .height(11.dp)
                        .skeletonBlock()
                )
            }
            Box(
                Modifier
                    .size(52.dp, 20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .shimmer()
            )
        }
        Box(
            Modifier
                .fillMaxWidth(0.35f)
                .height(20.dp)
                .skeletonBlock()
        )
    }
}

@Composable
fun ParkeoSkeletonList(itemCount: Int = 4, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(itemCount) { ParkeoSkeletonCard() }
    }
}
