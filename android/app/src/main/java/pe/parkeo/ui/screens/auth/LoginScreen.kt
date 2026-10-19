/*
 * PARKeo · Android
 * Archivo: LoginScreen.kt
 * Capa: presentación (autenticación).
 * Propósito: recoger correo y contraseña, validar el formato localmente, mostrar carga y
 * errores del backend, iniciar sesión mediante AuthViewModel y ofrecer el registro.
 * Conceptos clave: Composable, estado local con remember/mutableStateOf, StateFlow,
 * collectAsState, validación con Patterns.EMAIL_ADDRESS, KeyboardOptions/KeyboardActions,
 * FocusManager, campo de contraseña con visibilidad, ParkeoTextField, ParkeoButton y
 * navegación por callbacks.
 * Flujo: el usuario escribe credenciales; "Iniciar sesión" valida y llama a
 * `viewModel.login`; AuthViewModel llama al repositorio y publica `isLoading`, `error` o
 * `isSuccess`; un `LaunchedEffect` observa el éxito y ejecuta `onLoginSuccess`, que el grafo
 * traduce en navegación a home.
 * Relación con otros archivos: ParkeoNavGraph.kt crea AuthViewModel y conecta los
 * callbacks; AuthViewModel.kt expone AuthUiState; ParkeoTextField.kt implementa el campo y
 * el ojo de visibilidad; ParkeoButton.kt muestra el progreso de carga.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones de Compose, estado y
 * seguridad).
 */
package pe.parkeo.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * Pantalla de inicio de sesión.
 *
 * Mantiene en estado de composición los textos escritos y el error local del correo. Los
 * datos del formulario no se guardan en el ViewModel porque son efímeros; el ViewModel sí
 * conserva el resultado asíncrono (`isLoading`, `error`, `isSuccess`) y la sesión. Por eso,
 * si la Activity se recrea, los campos vuelven a vaciarse, pero la operación en curso y el
 * resultado siguen en el ViewModel.
 *
 * La validación local comprueba el formato del correo antes de llamar al backend. La
 * contraseña no se valida por longitud aquí: el servidor es la autoridad final y devuelve
 * un error legible que se muestra en el banner. Ocultar o mostrar la contraseña lo resuelve
 * `ParkeoTextField` internamente con `isPassword = true`; su icono de ojo tiene descripción
 * accesible ("Mostrar"/"Ocultar"). Cada campo conserva una etiqueta visible, de modo que el
 * lector de pantalla no depende del placeholder.
 *
 * @param viewModel ViewModel de autenticación; expone `uiState` y las acciones `login` y
 * `clearError`.
 * @param onLoginSuccess callback que se ejecuta cuando el ViewModel publica éxito. El grafo
 * limpia la señal y navega a home.
 * @param onNavigateToRegister callback para abrir el formulario de registro sin crear un
 * ViewModel nuevo de forma manual.
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    // Estado asíncrono del ViewModel: carga, error del backend y éxito.
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors

    // Permite mover el foco del correo a la contraseña y cerrar el teclado.
    val focusManager = LocalFocusManager.current

    // Campos controlados del formulario. `emailError` es validación local, no del servidor.
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }

    // Reacciona al éxito una sola vez por emisión. La navegación no se hace durante el
    // clic para no mezclar la acción de red con el cambio de pantalla.
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onLoginSuccess()
        }
    }

    /**
     * Valida el correo y, si el formulario está completo, inicia sesión.
     *
     * `Patterns.EMAIL_ADDRESS` es una comprobación de formato, no de existencia: un correo
     * bien escrito puede no estar registrado y el backend responderá con error. Se exige
     * correo y contraseña no vacíos; el correo se envía sin espacios laterales, la
     * contraseña se envía tal como se escribió.
     */
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

            // Marca: símbolo de estacionamiento dentro de un halo y una superficie circular.
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

            // Nombre de la marca con el punto de acento.
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

            // Formulario: correo electrónico. Al escribir se limpia el error local y el
            // error del backend para que el mensaje viejo no confunda al usuario.
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

            // Contraseña: `isPassword` activa la transformación visual y el botón para
            // mostrar u ocultar. "Listo" en el teclado valida y envía el formulario.
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

            // Banner de error del backend: credenciales inválidas, red caída o mensaje del
            // servidor. Se muestra debajo del formulario y desaparece al editar un campo.
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

            // Acción principal: se deshabilita si falta un campo y muestra un indicador de
            // progreso mientras `isLoading` es verdadero. ParkeoButton también deshabilita
            // el clic durante la carga para evitar envíos duplicados.
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

            // Enlace secundario al registro: no envía el formulario ni valida el correo.
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
        }
    }
}
