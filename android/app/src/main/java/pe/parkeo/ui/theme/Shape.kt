package pe.parkeo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// Specialized Parkeo shapes
val ParkeoTagShape = RoundedCornerShape(6.dp)
val ParkeoCardShape = RoundedCornerShape(14.dp)
val ParkeoInputShape = RoundedCornerShape(10.dp)
val ParkeoButtonShape = RoundedCornerShape(10.dp)
val ParkeoSheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
val ParkeoPillShape = RoundedCornerShape(50)
