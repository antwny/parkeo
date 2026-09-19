package pe.parkeo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.theme.*

data class ParkeoBottomNavItem(
    val title: String,
    val icon: ImageVector
)

@Composable
fun ParkeoBottomBar(
    items: List<ParkeoBottomNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = ParkeoTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(extended.surface1)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(
            color = extended.borderSubtle,
            thickness = Dimens.borderHairline
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                val itemColor by animateColorAsState(
                    targetValue = if (isSelected) extended.accent else extended.textTertiary,
                    animationSpec = tween(durationMillis = 200),
                    label = "itemColor"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onItemSelected(index) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .padding(horizontal = 12.dp)
                            .background(
                                color = if (isSelected) extended.accent.copy(alpha = 0.12f) else Color.Transparent,
                                shape = ParkeoPillShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = itemItemIcon(item.icon, isSelected),
                            contentDescription = item.title,
                            modifier = Modifier.size(20.dp),
                            tint = itemColor
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = itemColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun itemItemIcon(icon: ImageVector, isSelected: Boolean): ImageVector {
    return icon
}
