/*
 * PARKeo · Android
 * Archivo: `android/app/src/main/java/pe/parkeo/ui/components/ParkeoBadge.kt`
 * Capa: componentes reutilizables de interfaz.
 * Propósito: mostrar estados cortos y consistentes (disponibilidad, reserva, cierre, rol de
 *   usuario) mediante una etiqueta compacta con color semántico, borde y punto opcional.
 * Conceptos clave: composable stateless, `BadgeVariant`, `typealias`, color semántico,
 *   contraste claro/oscuro, tokens de tema, `Modifier` y accesibilidad del texto.
 * Flujo de uso: la pantalla elige una variante de dominio (`Available`, `Reserved`, etc.) o pasa
 *   un texto libre con `variant`. El badge solo pinta; no decide el estado ni navega.
 * Relación con pantallas/tema: se usa en tarjetas de estacionamiento, reservas, vehículos y
 *   administración de usuarios. Consume `ParkeoTheme.colors.isDark` para ajustar el texto del
 *   estado disponible en tema claro.
 * Guía: `docs/GUIA_MAESTRA_ESTUDIANTE.md`.
 */
package pe.parkeo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.theme.*

/**
 * Variantes semánticas de `ParkeoBadge`. Cada variante define un color de fondo translúcido, un
 * color de texto y un borde derivado del mismo tono:
 *
 * - [Available]: verde lima; disponibilidad positiva.
 * - [Occupied]: rojo; ocupado, completo o cancelado.
 * - [Reserved]: ámbar; reservado o pendiente.
 * - [Closed]: gris; cerrado o inactivo.
 * - [Neutral]: gris; estado neutro sin señal de negocio.
 * - [RoleAdmin] / [Admin]: violeta; rol administrador.
 * - [RoleOperator] / [Operator]: ámbar; rol operador.
 * - [RoleClient] / [Client]: cian; rol cliente.
 *
 * Los pares `RoleAdmin`/`Admin`, `RoleOperator`/`Operator` y `RoleClient`/`Client` son alias
 * visuales: existen para compatibilidad con pantallas que nombran el estado de distinta forma.
 */
enum class BadgeVariant {
    Available,
    Occupied,
    Reserved,
    Closed,
    Neutral,
    RoleAdmin,
    RoleOperator,
    RoleClient,
    Admin,
    Operator,
    Client
}

/**
 * Alias de compatibilidad para pantallas que llaman al estado del badge `ParkeoBadgeStatus`.
 * No crea un tipo nuevo: es exactamente [BadgeVariant].
 */
typealias ParkeoBadgeStatus = BadgeVariant

/**
 * Badge guiado por estado de dominio. Traduce [status] a un texto por defecto en español y
 * delega el dibujo en la sobrecarga que recibe texto. Es stateless: no guarda el estado.
 *
 * @param status Variante semántica que determina color y texto por defecto.
 * @param modifier `Modifier` aplicado a la superficie del badge.
 * @param labelOverride Texto opcional que sustituye la etiqueta por defecto, por ejemplo
 *   "3 libres" en lugar de "Disponible".
 * @param showDot Si es `true`, dibuja un punto de 6 dp del color del texto.
 */
@Composable
fun ParkeoBadge(
    status: BadgeVariant,
    modifier: Modifier = Modifier,
    labelOverride: String? = null,
    showDot: Boolean = true
) {
    // Texto por defecto según el estado de negocio; el override permite datos dinámicos.
    val defaultText = when (status) {
        BadgeVariant.Available -> "Disponible"
        BadgeVariant.Occupied -> "Ocupado"
        BadgeVariant.Reserved -> "Reservado"
        BadgeVariant.Closed -> "Cerrado"
        BadgeVariant.Neutral -> "Inactivo"
        BadgeVariant.RoleAdmin, BadgeVariant.Admin -> "Admin"
        BadgeVariant.RoleOperator, BadgeVariant.Operator -> "Operador"
        BadgeVariant.RoleClient, BadgeVariant.Client -> "Cliente"
    }

    // Se reutiliza la sobrecarga de texto para mantener una sola implementación visual.
    ParkeoBadge(
        text = labelOverride ?: defaultText,
        modifier = modifier,
        variant = status,
        showDot = showDot
    )
}

/**
 * Badge con texto libre y variante visual. Renderiza una `Surface` con forma de etiqueta (6 dp),
 * fondo translúcido y borde capilar; dentro coloca un punto opcional y el texto en mayúsculas.
 *
 * Es stateless: todo llega por parámetros. El texto es el contenido accesible; el punto es
 * decorativo y no anuncia nada por sí mismo. `maxLines = 1` y `softWrap = false` evitan que una
 * etiqueta corta se parta en dos líneas.
 *
 * @param text Texto a mostrar. Se convierte a mayúsculas en la composición visual.
 * @param modifier `Modifier` aplicado a la superficie.
 * @param variant Variante que selecciona la paleta; por defecto [BadgeVariant.Available].
 * @param showDot Si es `true`, muestra un punto de estado del mismo color que el texto.
 */
@Composable
fun ParkeoBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: BadgeVariant = BadgeVariant.Available,
    showDot: Boolean = true
) {
    // Cada variante produce fondo (15 % de alfa), texto y borde (30-40 % de alfa). En tema claro
    // el texto lima se oscurece para conservar contraste sobre el fondo casi blanco.
    val (bgColor, textColor, borderColor) = when (variant) {
        BadgeVariant.Available -> Triple(
            ParkeoLime.copy(alpha = 0.15f),
            if (ParkeoTheme.colors.isDark) ParkeoLime else Color(0xFF4D7300),
            ParkeoLime.copy(alpha = 0.35f)
        )
        BadgeVariant.Occupied -> Triple(
            ParkeoRed500.copy(alpha = 0.15f),
            ParkeoRed500,
            ParkeoRed500.copy(alpha = 0.35f)
        )
        BadgeVariant.Reserved -> Triple(
            ParkeoAmber500.copy(alpha = 0.15f),
            ParkeoAmber500,
            ParkeoAmber500.copy(alpha = 0.35f)
        )
        BadgeVariant.Closed, BadgeVariant.Neutral -> Triple(
            ParkeoGray500.copy(alpha = 0.15f),
            ParkeoGray500,
            ParkeoGray500.copy(alpha = 0.3f)
        )
        BadgeVariant.RoleAdmin, BadgeVariant.Admin -> Triple(
            Color(0xFF8B5CF6).copy(alpha = 0.15f),
            Color(0xFFA78BFA),
            Color(0xFF8B5CF6).copy(alpha = 0.4f)
        )
        BadgeVariant.RoleOperator, BadgeVariant.Operator -> Triple(
            Color(0xFFF59E0B).copy(alpha = 0.15f),
            Color(0xFFFBBF24),
            Color(0xFFF59E0B).copy(alpha = 0.4f)
        )
        BadgeVariant.RoleClient, BadgeVariant.Client -> Triple(
            ParkeoCyan500.copy(alpha = 0.15f),
            ParkeoCyan500,
            ParkeoCyan500.copy(alpha = 0.35f)
        )
    }

    // Superficie de la etiqueta: forma propia, fondo translúcido y borde fino.
    Surface(
        modifier = modifier,
        shape = ParkeoTagShape,
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Punto de estado decorativo; el significado lo aporta el texto.
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(textColor, CircleShape)
                )
            }
            // Etiqueta en mayúsculas para el estilo de “chip” compacto de PARKeo.
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 9.5.sp,
                letterSpacing = 0.6.sp,
                color = textColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
