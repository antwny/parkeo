/*
 * PARKeo · Android
 * Archivo: `android/app/src/main/java/pe/parkeo/ui/components/ParkeoStates.kt`
 * Capa: componentes reutilizables de interfaz (estados de pantalla).
 * Propósito: unificar las vistas de estado vacío, error con reintento y carga para que todas las
 *   pantallas comuniquen la misma situación con el mismo lenguaje visual.
 * Conceptos clave: composable stateless, estado de UI, slots de acción, `Modifier`,
 *   `CircularProgressIndicator`, botones reutilizables, accesibilidad y jerarquía tipográfica.
 * Flujo de uso: la pantalla observa su estado (por ejemplo `isLoading`, `errorMessage` o una
 *   lista vacía) y elige la vista correspondiente. Estos componentes no hacen peticiones ni
 *   deciden el estado: solo lo presentan y elevan eventos como `onRetry` o `onAction`.
 * Relación con pantallas/tema: inicio, reservas, vehículos y módulos de administración los usan
 *   dentro de sus scaffolds. Consumen `ParkeoTheme.colors`, `Dimens` y `ParkeoButton`.
 * Guía: `docs/GUIA_MAESTRA_ESTUDIANTE.md`.
 */
package pe.parkeo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.theme.*

/**
 * Estado vacío de PARKeo. Se muestra cuando una consulta terminó correctamente pero no hay
 * elementos que presentar (por ejemplo, un usuario sin vehículos o sin reservas). Incluye un
 * icono dentro de un círculo, un título, una descripción opcional y una acción opcional.
 *
 * Es stateless y ofrece alias para facilitar la migración: [description]/[subtitle] y
 * [actionButtonText]/[actionLabel] representan lo mismo. La descripción efectiva es
 * [description] si no está en blanco; en caso contrario se usa [subtitle]. El texto del botón es
 * [actionLabel] si existe; en caso contrario, [actionButtonText]. El handler es [onAction] si
 * existe; en caso contrario, [onActionClick]. El botón solo aparece cuando hay texto y handler.
 *
 * La acción usa `ParkeoButtonStyle.Secondary` y tamaño compacto, de modo que el estado vacío no
 * compita con la acción principal de la pantalla. El icono es decorativo
 * (`contentDescription = null`); el título y la descripción son el contenido accesible.
 *
 * @param title Título breve que explica la ausencia de contenido.
 * @param modifier `Modifier` aplicado a la columna; el componente añade ancho completo y padding.
 * @param description Texto explicativo; si está en blanco se intenta usar [subtitle].
 * @param subtitle Alias de respaldo para la descripción.
 * @param icon Icono del estado; por defecto `Icons.Filled.Info`.
 * @param actionButtonText Texto del botón opcional.
 * @param actionLabel Alias prioritario del texto del botón.
 * @param onActionClick Callback del botón opcional.
 * @param onAction Alias prioritario del callback del botón.
 */
@Composable
fun ParkeoEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String = "",
    subtitle: String = description,
    icon: ImageVector = Icons.Filled.Info,
    actionButtonText: String? = null,
    actionLabel: String? = actionButtonText,
    onActionClick: (() -> Unit)? = null,
    onAction: (() -> Unit)? = onActionClick
) {
    val extended = ParkeoTheme.colors
    // Resolución de alias: description/actionLabel/onAction tienen prioridad cuando se indican.
    val desc = if (description.isNotBlank()) description else subtitle
    val actionText = actionLabel ?: actionButtonText
    val actionHandler = onAction ?: onActionClick

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icono en un círculo de superficie con borde sutil.
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(extended.surface2, CircleShape)
                .border(Dimens.borderThin, extended.border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = extended.accent
            )
        }

        Spacer(Modifier.height(Dimens.space20))

        // Título del estado vacío.
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = extended.textPrimary,
            textAlign = TextAlign.Center
        )

        // Descripción opcional, centrada y con altura de línea legible.
        if (desc.isNotBlank()) {
            Spacer(Modifier.height(Dimens.space8))

            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = extended.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }

        // Acción opcional: solo se muestra si existen texto y callback.
        if (actionText != null && actionHandler != null) {
            Spacer(Modifier.height(Dimens.space24))
            ParkeoButton(
                text = actionText,
                onClick = actionHandler,
                style = ParkeoButtonStyle.Secondary,
                compact = true,
                modifier = Modifier.widthIn(max = 200.dp)
            )
        }
    }
}

/**
 * Vista de error de PARKeo. Presenta un icono de advertencia, el título fijo "Algo salió mal",
 * el mensaje recibido y un botón "Reintentar". Se usa cuando una operación falla y la pantalla
 * puede ofrecer una nueva oportunidad al usuario.
 *
 * Es stateless: [onRetry] eleva la intención de reintento; el componente no repite la petición.
 * El título y el mensaje no están limitados a una línea, por lo que pueden ocupar varias líneas
 * y se alinean al centro. El icono es decorativo; el botón es la acción accesible principal.
 *
 * @param message Detalle del error que se muestra al usuario. Debería ser un texto comprensible,
 *   no una traza ni un mensaje técnico crudo.
 * @param onRetry Callback que la pantalla ejecuta para volver a intentar la operación.
 * @param modifier `Modifier` aplicado a la columna; el componente añade ancho completo y padding.
 */
@Composable
fun ParkeoErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extended = ParkeoTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icono de error dentro de un círculo rojo translúcido.
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(ParkeoRedContainer, CircleShape)
                .border(Dimens.borderThin, ParkeoRed500.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = ParkeoRed500
            )
        }

        Spacer(Modifier.height(Dimens.space16))

        // Título fijo del error.
        Text(
            text = "Algo salió mal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = extended.textPrimary
        )

        Spacer(Modifier.height(Dimens.space8))

        // Mensaje concreto recibido desde la capa que detectó el fallo.
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = extended.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(Dimens.space24))

        // Acción de reintento: botón primario compacto y limitado en ancho.
        ParkeoButton(
            text = "Reintentar",
            onClick = onRetry,
            style = ParkeoButtonStyle.Primary,
            compact = true,
            modifier = Modifier.widthIn(max = 180.dp)
        )
    }
}

/**
 * Vista de carga de PARKeo. Muestra un indicador circular con el acento del tema y un mensaje
 * opcional debajo. Se usa mientras la pantalla espera una operación asíncrona y todavía no tiene
 * datos que mostrar.
 *
 * Es stateless y no controla el progreso: `CircularProgressIndicator` sin valor representa una
 * espera indeterminada. El mensaje es texto visible y complementa la semántica de progreso del
 * indicador; si se necesita un anuncio más específico, la pantalla puede pasar un texto claro.
 *
 * @param modifier `Modifier` aplicado a la columna; el componente añade ancho completo y padding.
 * @param message Texto opcional que describe qué se está cargando.
 */
@Composable
fun ParkeoLoadingView(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    val extended = ParkeoTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.space32),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Indicador indeterminado con el acento de marca.
        CircularProgressIndicator(
            color = extended.accent,
            strokeWidth = 2.5.dp,
            modifier = Modifier.size(36.dp)
        )
        // Mensaje opcional para dar contexto a la espera.
        if (message != null) {
            Spacer(Modifier.height(Dimens.space16))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = extended.textSecondary
            )
        }
    }
}
