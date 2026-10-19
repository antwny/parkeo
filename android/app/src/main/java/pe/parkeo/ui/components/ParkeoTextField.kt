/*
 * PARKeo · Android
 * Archivo: `android/app/src/main/java/pe/parkeo/ui/components/ParkeoTextField.kt`
 * Capa: componentes reutilizables de interfaz.
 * Propósito: ofrecer un campo de texto con la apariencia de PARKeo, etiqueta visible, placeholder,
 *   icono inicial, mensaje de error, alternancia de contraseña y acción final personalizable,
 *   manteniendo el valor controlado por la pantalla.
 * Conceptos clave: composable con estado interno mínimo, estado elevado (`value`/`onValueChange`),
 *   `BasicTextField`, `Modifier`, foco, transformación visual, `KeyboardOptions`,
 *   `KeyboardActions`, slots y accesibilidad.
 * Flujo de uso: la pantalla guarda el texto y pasa `value` + `onValueChange`; el campo solo
 *   recuerda foco y visibilidad de contraseña. Si hay error, la pantalla pasa `errorMessage` y
 *   el componente cambia borde, etiqueta y añade el mensaje.
 * Relación con pantallas/tema: se usa en inicio de sesión, registro, filtros, reservas y perfil.
 *   Consume `ParkeoTheme.colors` y `Dimens` para superficies, bordes, acento y alturas.
 * Guía: `docs/GUIA_MAESTRA_ESTUDIANTE.md`.
 */
package pe.parkeo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.theme.*

/**
 * Campo de texto de PARKeo construido sobre `BasicTextField`, no sobre el `OutlinedTextField` de
 * Material, para controlar por completo fondo, borde, etiqueta y acciones. El valor es controlado
 * por el llamador: el componente no guarda el texto, solo estado de presentación (foco y
 * visibilidad de contraseña).
 *
 * Comportamiento visual:
 * - En reposo usa `extended.border` con `Dimens.borderThin`.
 * - Con foco usa `extended.accent` con `Dimens.borderFocus` y tiñe el icono inicial.
 * - Con error usa `ParkeoRed500`, prioridad sobre el foco, y muestra el mensaje debajo.
 *
 * La etiqueta se dibuja fuera del contenedor y el placeholder es un `Text` superpuesto cuando el
 * valor está vacío; no son la semántica nativa de `BasicTextField`. El icono inicial es
 * decorativo. Los botones automáticos de mostrar/ocultar contraseña y limpiar sí tienen
 * `contentDescription` ("Mostrar", "Ocultar", "Limpiar").
 *
 * @param value Texto actual. Es la fuente de verdad controlada por la pantalla.
 * @param onValueChange Callback que recibe el nuevo texto en cada edición, limpieza o pegado.
 * @param modifier `Modifier` aplicado a la columna completa (etiqueta, campo y error).
 * @param label Etiqueta opcional encima del campo. Cambia de color con foco/error.
 * @param placeholder Texto de ayuda opcional que se muestra solo cuando `value` está vacío.
 * @param leadingIcon Icono opcional al inicio; decorativo y teñido según foco.
 * @param isPassword Activa la transformación de contraseña y el botón de ojo cuando no hay un
 *   `trailingContent` personalizado.
 * @param errorMessage Mensaje de error. Si no es nulo ni blanco, activa el estado de error y se
 *   muestra debajo del campo.
 * @param keyboardOptions Configuración del teclado (tipo, capitalización, IME action).
 * @param keyboardActions Acciones del teclado, por ejemplo enviar o siguiente.
 * @param singleLine Si es `true`, limita la entrada a una sola línea.
 * @param enabled Si es `false`, el campo no recibe interacción y se muestra deshabilitado.
 * @param readOnly Permite foco y selección pero impide modificar el texto.
 * @param trailingIcon Slot opcional para una acción final personalizada.
 * @param trailingContent Alias de [trailingIcon]; si se proporciona, tiene prioridad sobre el
 *   icono de contraseña o limpieza automático.
 */
@Composable
fun ParkeoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = trailingIcon
) {
    val extended = ParkeoTheme.colors
    // Estado de presentación local: foco visual y visibilidad de la contraseña. No es el valor
    // del campo, que permanece elevado en la pantalla.
    var isFocused by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val hasError = !errorMessage.isNullOrBlank()

    // El error gana al foco para que una validación fallida siga siendo visible.
    val borderColor = when {
        hasError -> ParkeoRed500
        isFocused -> extended.accent
        else -> extended.border
    }

    // El borde se refuerza en foco o error; en reposo se mantiene fino.
    val borderWidth = if (isFocused || hasError) Dimens.borderFocus else Dimens.borderThin

    Column(modifier = modifier.fillMaxWidth()) {
        // Etiqueta: semántica visual, alineada con el estado del campo.
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (hasError) ParkeoRed500 else if (isFocused) extended.accent else extended.textSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        // Contenedor del campo: fondo, borde y padding horizontal compartidos.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.inputHeight)
                .background(extended.surface2, ParkeoInputShape)
                .border(borderWidth, borderColor, ParkeoInputShape)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono inicial decorativo; el acento indica foco.
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconSizeMedium),
                    tint = if (isFocused) extended.accent else extended.textTertiary
                )
                Spacer(Modifier.width(10.dp))
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                // Placeholder visual: solo aparece cuando no hay texto.
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = extended.textTertiary
                    )
                }

                // Campo real. La transformación de contraseña se aplica solo si el ojo está oculto.
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = extended.textPrimary,
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(extended.accent),
                    singleLine = singleLine,
                    enabled = enabled,
                    readOnly = readOnly,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None
                )
            }

            // Acción final: slot personalizado con prioridad, o botón automático de contraseña,
            // o limpieza rápida cuando hay texto y el campo tiene foco.
            val finalTrailing = trailingContent ?: trailingIcon
            if (finalTrailing != null) {
                finalTrailing()
            } else if (isPassword) {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Ocultar" else "Mostrar",
                        tint = extended.textTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (value.isNotEmpty() && isFocused) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Limpiar",
                        tint = extended.textTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Mensaje de error debajo del campo, con el mismo rojo semántico del borde.
        if (hasError) {
            Text(
                text = errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = ParkeoRed500,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}
