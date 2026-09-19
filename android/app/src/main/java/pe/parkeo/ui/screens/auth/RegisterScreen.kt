package pe.parkeo.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import pe.parkeo.ui.components.ParkeoButton
import pe.parkeo.ui.components.ParkeoButtonStyle
import pe.parkeo.ui.components.ParkeoTextField
import pe.parkeo.ui.components.ParkeoTopBar
import pe.parkeo.ui.theme.Dimens
import pe.parkeo.ui.theme.ParkeoCardShape
import pe.parkeo.ui.theme.ParkeoTheme
import pe.parkeo.ui.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onRegisterSuccess()
    }

    val passwordsMatch = confirmPassword.isBlank() || password == confirmPassword
    val isValid = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
            password.length >= 8 &&
            password == confirmPassword

    Scaffold(
        containerColor = extended.background,
        topBar = {
            ParkeoTopBar(
                title = "Crear cuenta",
                onNavigationClick = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.spacingLg, vertical = Dimens.spacingMd),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Únete a la red de movilidad Parkeo",
                style = MaterialTheme.typography.bodyMedium,
                color = extended.textSecondary,
                modifier = Modifier.padding(bottom = Dimens.spacingLg)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ParkeoTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = "Nombre",
                    placeholder = "Juan",
                    leadingIcon = Icons.Filled.Person,
                    modifier = Modifier.weight(1f)
                )

                ParkeoTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = "Apellido",
                    placeholder = "Pérez",
                    leadingIcon = Icons.Filled.Person,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(Dimens.spacingMd))

            ParkeoTextField(
                value = email,
                onValueChange = {
                    email = it
                    viewModel.clearError()
                },
                label = "Correo electrónico",
                placeholder = "juan.perez@parkeo.pe",
                leadingIcon = Icons.Filled.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Dimens.spacingMd))

            ParkeoTextField(
                value = phone,
                onValueChange = { phone = it },
                label = "Teléfono móvil (opcional)",
                placeholder = "+51 999 888 777",
                leadingIcon = Icons.Filled.Phone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Dimens.spacingMd))

            ParkeoTextField(
                value = password,
                onValueChange = { password = it },
                label = "Contraseña",
                placeholder = "Mínimo 8 caracteres",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Dimens.spacingMd))

            ParkeoTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = "Confirmar contraseña",
                placeholder = "Repite la contraseña",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                errorMessage = if (!passwordsMatch) "Las contraseñas no coinciden" else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            uiState.error?.let { errorMsg ->
                Spacer(Modifier.height(Dimens.spacingMd))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ParkeoCardShape,
                    color = extended.signalRed.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        Dimens.borderHairline,
                        extended.signalRed.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(Dimens.spacingMd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = extended.signalRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(Dimens.spacingSm))
                        Text(
                            text = errorMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.signalRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(Dimens.spacingXl))

            ParkeoButton(
                text = "Crear cuenta",
                onClick = {
                    viewModel.register(
                        email = email.trim(),
                        password = password,
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        phone = phone.trim().ifBlank { null }
                    )
                },
                isLoading = uiState.isLoading,
                enabled = isValid,
                style = ParkeoButtonStyle.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeightLarge)
            )

            Spacer(Modifier.height(Dimens.spacingMd))

            TextButton(
                onClick = onNavigateBack,
                colors = ButtonDefaults.textButtonColors(contentColor = extended.textSecondary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "¿Ya tienes una cuenta? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = extended.textSecondary
                    )
                    Text(
                        text = "Inicia sesión",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = extended.accent
                    )
                }
            }

            Spacer(Modifier.height(Dimens.spacingXl))
        }
    }
}
