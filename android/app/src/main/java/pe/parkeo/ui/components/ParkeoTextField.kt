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
    var isFocused by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    val hasError = !errorMessage.isNullOrBlank()

    val borderColor = when {
        hasError -> ParkeoRed500
        isFocused -> extended.accent
        else -> extended.border
    }

    val borderWidth = if (isFocused || hasError) Dimens.borderFocus else Dimens.borderThin

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (hasError) ParkeoRed500 else if (isFocused) extended.accent else extended.textSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.inputHeight)
                .background(extended.surface2, ParkeoInputShape)
                .border(borderWidth, borderColor, ParkeoInputShape)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = extended.textTertiary
                    )
                }

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
