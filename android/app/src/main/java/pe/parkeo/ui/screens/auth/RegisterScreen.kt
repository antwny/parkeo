/*
 * PARKeo · Android
 * Archivo: RegisterScreen.kt
 * Capa: presentación (creación de cuenta).
 * Propósito: recoger nombres, correo, teléfono opcional y contraseña, validar requisitos
 * mínimos y registrar al usuario mediante AuthViewModel.
 * Conceptos clave: Composable, estado local, validación de contraseña por reglas,
 * coincidencia de confirmación, Patterns.EMAIL_ADDRESS, ParkeoTextField, ParkeoTopBar,
 * ParkeoButton, estados de carga/error y navegación por callbacks.
 * Flujo: el formulario calcula en cada recomposición si es válido; al enviar llama a
 * `viewModel.register`; AuthViewModel publica el resultado; `LaunchedEffect` observa
 * `isSuccess` y ejecuta `onRegisterSuccess`, que el grafo traduce en navegación a home.
 * Relación con otros archivos: ParkeoNavGraph.kt crea AuthViewModel y conecta los callbacks;
 * AuthViewModel.kt expone AuthUiState; ParkeoTextField.kt y ParkeoButton.kt aportan los
 * controles reutilizables; ParkeoTopBar.kt permite volver al login.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones de Compose, estado y
 * seguridad).
 */
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
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.components.ParkeoButton
import pe.parkeo.ui.components.ParkeoButtonStyle
import pe.parkeo.ui.components.ParkeoTextField
import pe.parkeo.ui.components.ParkeoTopBar
import pe.parkeo.ui.theme.Dimens
import pe.parkeo.ui.theme.ParkeoCardShape
import pe.parkeo.ui.theme.ParkeoTheme
import pe.parkeo.ui.viewmodel.AuthViewModel

/**
 * Pantalla de creación de cuenta.
 *
 * Los campos viven en estado de composición; el ViewModel conserva solo el resultado de la
 * operación. Las reglas de contraseña se calculan en cada recomposición a partir del texto
 * actual, sin necesidad de un botón "validar": la lista de requisitos se actualiza en vivo
 * y el botón principal solo se habilita cuando todo se cumple.
 *
 * La validación local es una ayuda de usabilidad, no una garantía de seguridad. El backend
 * vuelve a validar el correo, la contraseña y la unicidad de la cuenta. La contraseña se
 * envía sin recortar espacios; los nombres, el correo y el teléfono sí se recortan, y un
 * teléfono vacío se convierte en `null`.
 *
 * Accesibilidad: todos los campos tienen etiqueta visible, el campo de confirmación anuncia
 * su error cuando las contraseñas no coinciden y los iconos de requisitos son decorativos
 * porque el texto de cada fila ya expresa la regla.
 *
 * @param viewModel ViewModel de autenticación que expone `uiState` y la acción `register`.
 * @param onRegisterSuccess callback ejecutado cuando el registro termina con éxito; el
 * grafo limpia la señal y navega a home.
 * @param onNavigateBack callback de la barra superior para volver al login.
 */
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // Estado asíncrono compartido con el login: carga, error y éxito.
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors

    // Campos controlados del formulario. Se mantienen locales porque son efímeros.
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Navegación reactiva al éxito: se ejecuta cuando el flujo cambia a verdadero.
    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onRegisterSuccess()
    }

    // Reglas de contraseña evaluadas en vivo para la lista de requisitos.
    val hasMinLength = password.length >= 8
    val hasUpperCase = password.any { it.isUpperCase() }
    val hasLowerCase = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val isPasswordStrong = hasMinLength && hasUpperCase && hasLowerCase && hasDigit

    // Si la confirmación está vacía todavía no se marca error; la igualdad final se exige
    // de todos modos en `isValid`, junto con nombres, correo y fortaleza de contraseña.
    val passwordsMatch = confirmPassword.isBlank() || password == confirmPassword
    val isValid = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() &&
            isPasswordStrong &&
            password == confirmPassword

    Scaffold(
        containerColor = extended.background,
        topBar = {
            // Barra superior con flecha de regreso al login; no hay botón de acción.
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
            // Introducción breve del formulario.
            Text(
                text = "Únete a la red de movilidad Parkeo",
                style = MaterialTheme.typography.bodyMedium,
                color = extended.textSecondary,
                modifier = Modifier.padding(bottom = Dimens.spacingLg)
            )

            // Nombre y apellido comparten fila; cada campo ocupa la mitad del ancho.
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

            // Correo: teclado especializado. Al editar se limpia el error del backend.
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

            // Teléfono opcional: no participa en `isValid`; si queda vacío se envía null.
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

            // Contraseña: `isPassword` oculta el texto y ofrece el botón de visibilidad.
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

            Spacer(Modifier.height(Dimens.spacingSm))

            // Lista de requisitos: cada fila cambia de icono y color cuando se cumple.
            // Es retroalimentación local; el backend sigue siendo la autoridad final.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingXs),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PasswordRequirementRow(
                    label = "Mínimo 8 caracteres",
                    isMet = hasMinLength
                )
                PasswordRequirementRow(
                    label = "Al menos una mayúscula y una minúscula",
                    isMet = hasUpperCase && hasLowerCase
                )
                PasswordRequirementRow(
                    label = "Al menos un número (0-9)",
                    isMet = hasDigit
                )
            }

            Spacer(Modifier.height(Dimens.spacingMd))

            // Confirmación: muestra error solo cuando hay texto y no coincide. También
            // activa la transformación de contraseña y su propio botón de visibilidad.
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

            // Banner de error del backend: correo ya registrado, datos inválidos o fallo
            // de red. Se oculta al editar el correo; los demás campos no lo limpian.
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

            // Acción principal: envía los datos normalizados. Se deshabilita hasta que
            // `isValid` sea verdadero y muestra progreso mientras el ViewModel carga.
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

            // Enlace de retorno al login; equivale a la flecha de la barra superior.
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

/**
 * Fila de un requisito de contraseña con retroalimentación visual.
 *
 * Cuando `isMet` es verdadero se muestra un check verde; si no, un círculo vacío en color
 * terciario. El icono es decorativo porque el texto indica la regla; el estado se percibe
 * también por color, pero no se anuncia como control interactivo.
 *
 * @param label descripción legible del requisito.
 * @param isMet indica si la contraseña actual ya cumple la regla.
 */
@Composable
private fun PasswordRequirementRow(
    label: String,
    isMet: Boolean
) {
    val extended = ParkeoTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = if (isMet) extended.signalGreen else extended.textTertiary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = if (isMet) extended.textSecondary else extended.textTertiary
        )
    }
}

