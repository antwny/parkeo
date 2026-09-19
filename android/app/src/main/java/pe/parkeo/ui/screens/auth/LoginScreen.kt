package pe.parkeo.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.components.ParkeoButton
import pe.parkeo.ui.components.ParkeoButtonStyle
import pe.parkeo.ui.components.ParkeoTextField
import pe.parkeo.ui.theme.Dimens
import pe.parkeo.ui.theme.ParkeoCardShape
import pe.parkeo.ui.theme.ParkeoTheme
import pe.parkeo.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onLoginSuccess()
        }
    }

    fun validateAndLogin() {
        emailError = if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            "Ingresa un correo válido"
        } else {
            null
        }
        if (emailError == null && email.isNotBlank() && password.isNotBlank()) {
            viewModel.login(email.trim(), password)
        }
    }

    Scaffold(
        containerColor = extended.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.spacingLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(Dimens.spacingXl))

            // Brand Symbol Vessel
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                extended.accent.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(extended.surface2, CircleShape)
                        .border(Dimens.borderHairline, extended.borderSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalParking,
                        contentDescription = "Parkeo",
                        tint = extended.accent,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(Modifier.height(Dimens.spacingMd))

            // Brand Name & Dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Parkeo",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = extended.textPrimary,
                    letterSpacing = (-0.5).sp
                )
                Box(
                    modifier = Modifier
                        .padding(start = 3.dp, top = 8.dp)
                        .size(6.dp)
                        .background(extended.accent, CircleShape)
                )
            }

            Spacer(Modifier.height(Dimens.spacingXs))

            Text(
                text = "Acceso a tu red de cocheras en tiempo real",
                style = MaterialTheme.typography.bodyMedium,
                color = extended.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(Dimens.spacingXl))

            // Form Inputs
            ParkeoTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                    viewModel.clearError()
                },
                label = "Correo electrónico",
                placeholder = "usuario@parkeo.pe",
                leadingIcon = Icons.Filled.Email,
                errorMessage = emailError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Dimens.spacingMd))

            ParkeoTextField(
                value = password,
                onValueChange = {
                    password = it
                    viewModel.clearError()
                },
                label = "Contraseña",
                placeholder = "••••••••",
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        validateAndLogin()
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Backend Error Banner
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

            Spacer(Modifier.height(Dimens.spacingLg))

            // Submit Button
            ParkeoButton(
                text = "Iniciar sesión",
                onClick = { validateAndLogin() },
                isLoading = uiState.isLoading,
                enabled = email.isNotBlank() && password.isNotBlank(),
                style = ParkeoButtonStyle.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeightLarge)
            )

            Spacer(Modifier.height(Dimens.spacingMd))

            // Navigation to Register
            TextButton(
                onClick = onNavigateToRegister,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = extended.textSecondary
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "¿No tienes una cuenta? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = extended.textSecondary
                    )
                    Text(
                        text = "Regístrate",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = extended.accent
                    )
                }
            }

            Spacer(Modifier.height(Dimens.spacingLg))

            // Test Access Chips Container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ParkeoCardShape,
                color = extended.surface1,
                border = androidx.compose.foundation.BorderStroke(
                    Dimens.borderHairline,
                    extended.borderSubtle
                )
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.spacingMd),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ACCESO RÁPIDO DE PRUEBAS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = extended.textTertiary,
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(Dimens.spacingSm))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickLoginChip(
                            modifier = Modifier.weight(1f),
                            role = "Admin",
                            icon = Icons.Filled.AdminPanelSettings,
                            color = extended.accent,
                            onClick = {
                                email = "admin@parkeo.pe"
                                password = "Password123!"
                                viewModel.login("admin@parkeo.pe", "Password123!")
                            }
                        )

                        QuickLoginChip(
                            modifier = Modifier.weight(1f),
                            role = "Operador",
                            icon = Icons.Filled.Badge,
                            color = extended.signalAmber,
                            onClick = {
                                email = "operador@parkeo.pe"
                                password = "Password123!"
                                viewModel.login("operador@parkeo.pe", "Password123!")
                            }
                        )

                        QuickLoginChip(
                            modifier = Modifier.weight(1f),
                            role = "Cliente",
                            icon = Icons.Filled.Person,
                            color = extended.textPrimary,
                            onClick = {
                                email = "cliente@parkeo.pe"
                                password = "Password123!"
                                viewModel.login("cliente@parkeo.pe", "Password123!")
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(Dimens.spacingLg))
        }
    }
}

@Composable
private fun QuickLoginChip(
    modifier: Modifier = Modifier,
    role: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    val extended = ParkeoTheme.colors
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = extended.surface2,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, extended.borderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = role,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = extended.textPrimary,
                maxLines = 1
            )
        }
    }
}
