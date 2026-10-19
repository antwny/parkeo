/*
 * PARKeo · Android
 * Archivo: `android/app/src/main/java/pe/parkeo/ui/components/ParkeoTopBar.kt`
 * Capa: componentes reutilizables de interfaz.
 * Propósito: unificar la barra superior de las pantallas: título y subtítulo, botón de regreso,
 *   acciones a la derecha, marca PARKeo opcional y divisor inferior.
 * Conceptos clave: composable stateless, slot de acciones (`RowScope`), `TopAppBar` de
 *   Material 3, `Modifier`, accesibilidad del botón de regreso y tokens de tema.
 * Flujo de uso: cada pantalla pasa `title`, opcionalmente `subtitle`, `onNavigateBack` y sus
 *   acciones. La barra no navega por sí misma: ejecuta el callback que recibe.
 * Relación con pantallas/tema: se usa en inicio, reservas, perfil, vehículos y módulos de
 *   administración/operador. Usa `surface0`, textos y bordes de `ParkeoTheme` para integrarse
 *   con el scaffold.
 * Guía: `docs/GUIA_MAESTRA_ESTUDIANTE.md`.
 */
package pe.parkeo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.theme.*

/**
 * Barra superior de PARKeo basada en `TopAppBar` de Material 3. Es stateless: el título, las
 * acciones y la navegación llegan como parámetros.
 *
 * Modos de título:
 * - Marca (`showBrandLogo` o `isBrandTitle`): muestra "Parkeo" con un punto lima decorativo.
 *   En este modo se ignora [title] y no se dibuja [subtitle].
 * - Texto normal: muestra [title] y, si existe, [subtitle] debajo.
 *
 * Navegación: si se pasa [onNavigationClick] o [onNavigateBack], aparece una flecha con
 * `contentDescription = "Regresar"`. Si ambos existen, gana [onNavigationClick]. Si ambos son
 * nulos, no se muestra icono de navegación.
 *
 * Las [actions] se colocan en el slot de acciones de la barra (`RowScope`), normalmente con
 * `IconButton`. La barra usa `surface0` como fondo y un divisor capilar opcional.
 *
 * @param title Título textual de la pantalla. Se ignora en modo marca.
 * @param modifier `Modifier` aplicado a la columna que contiene la barra y el divisor.
 * @param subtitle Texto secundario opcional bajo el título; se ignora en modo marca.
 * @param onNavigateBack Callback de regreso. Se usa como valor por defecto de
 *   [onNavigationClick].
 * @param onNavigationClick Callback explícito de navegación; si se indica, tiene prioridad.
 * @param actions Slot de acciones alineadas a la derecha.
 * @param showBrandLogo Activa el título de marca "Parkeo" con el punto lima.
 * @param isBrandTitle Alias para activar el modo marca; por defecto hereda `showBrandLogo`.
 * @param showBottomDivider Si es `true`, dibuja un divisor inferior de 0.75 dp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkeoTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    onNavigationClick: (() -> Unit)? = onNavigateBack,
    actions: @Composable RowScope.() -> Unit = {},
    showBrandLogo: Boolean = false,
    isBrandTitle: Boolean = showBrandLogo,
    showBottomDivider: Boolean = true
) {
    val extended = ParkeoTheme.colors
    // `onNavigationClick` es el alias prioritario; `onNavigateBack` funciona como fallback.
    val backHandler = onNavigationClick ?: onNavigateBack
    val isBrand = showBrandLogo || isBrandTitle

    Column(modifier = modifier.fillMaxWidth()) {
        // Barra principal: título/marca, navegación y acciones.
        TopAppBar(
            title = {
                if (isBrand) {
                    // Modo marca: "Parkeo" con un punto lima que refuerza la identidad.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Parkeo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                            color = extended.textPrimary
                        )
                        Box(
                            modifier = Modifier
                                .padding(start = 2.dp, top = 2.dp)
                                .size(6.dp)
                                .background(extended.accent, ParkeoPillShape)
                        )
                    }
                } else {
                    // Modo texto: título y subtítulo apilados, ambos de una sola línea.
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = extended.textPrimary,
                            maxLines = 1
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                // Botón de regreso opcional; el ícono se adapta a idiomas RTL.
                if (backHandler != null) {
                    IconButton(onClick = backHandler) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = extended.textPrimary
                        )
                    }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = extended.surface0,
                titleContentColor = extended.textPrimary,
                navigationIconContentColor = extended.textPrimary,
                actionIconContentColor = extended.textPrimary
            )
        )
        // Divisor inferior para separar la barra del contenido del scaffold.
        if (showBottomDivider) {
            HorizontalDivider(
                color = extended.borderSubtle,
                thickness = Dimens.borderHairline
            )
        }
    }
}
