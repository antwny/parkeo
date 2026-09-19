package pe.parkeo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pe.parkeo.ui.ParkeoApp
import pe.parkeo.ui.theme.ParkeoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParkeoTheme {
                ParkeoApp()
            }
        }
    }
}
