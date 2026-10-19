/*
 * PARKeo · Android
 * Archivo: `android/app/src/main/java/pe/parkeo/ui/screens/admin/AdminUsersScreen.kt`
 * Capa: presentación (Jetpack Compose), rol administrador (`ADMIN`).
 *
 * Propósito:
 * Mostrar las cuentas registradas en la red, permitir buscar por nombre, correo
 * o teléfono, filtrar por rol y activar o desactivar un usuario. La pantalla es
 * de supervisión: no incluye un formulario de alta o edición de datos.
 *
 * Conceptos clave:
 * - Rol `ADMIN`: perfil que administra cuentas globales. La pantalla asume que
 *   el usuario actual es administrador, pero esa condición debe comprobarla el
 *   backend en cada endpoint.
 * - Roles: `UserDto.role` es el rol principal y `UserDto.roles` la lista completa.
 *   El filtro reconoce `ADMIN`, `OPERADOR`/`OPERATOR` y `CLIENTE`/`USER`.
 * - Búsqueda: se aplica en memoria sobre la lista cargada, sin consultas
 *   remotas por texto.
 * - Filtro por rol: chips locales que no alteran los datos del servidor.
 * - `Compose`: la lista filtrada se recalcula con `remember` a partir del
 *   estado, la búsqueda y el rol seleccionado.
 * - `StateFlow`: `AdminViewModel.uiState` aporta `users`, `isLoading`, `error`
 *   y `successMessage`.
 * - `LazyColumn`: dibuja solo las tarjetas visibles.
 * - Paginación: no está implementada en esta pantalla. El ViewModel carga la
 *   lista completa de usuarios y aquí solo se filtra; no hay páginas, scroll
 *   infinito ni metadatos de paginación.
 * - Validaciones: no existe formulario de alta/edición, por lo que no hay
 *   validación local de campos. El cambio de estado activo/inactivo se envía
 *   directamente, sin diálogo de confirmación; el backend debe comprobar que
 *   el solicitante tenga permiso y que la transición sea válida.
 *
 * Flujo de usuario y datos:
 * 1. La lista se llena con la carga que realiza el dashboard o con una recarga
 *    manual; esta pantalla no dispara su propia carga inicial y observa
 *    `uiState.users`.
 * 2. Escribir en el buscador actualiza `searchQuery`; tocar un chip actualiza
 *    `selectedRoleFilter`. Ambos filtros se combinan con AND.
 * 3. El switch de cada tarjeta llama a `viewModel.toggleUserStatus(...)`, que
 *    envía al backend el estado contrario al actual y actualiza la lista con la
 *    respuesta.
 * 4. Éxitos y errores se muestran en un snackbar y luego se limpian del estado.
 *
 * Relación con ViewModel, navegación y backend:
 * - `AdminViewModel` concentra las operaciones y publica el estado.
 * - `onNavigateBack` permite volver; la pantalla no conoce rutas concretas.
 * - El backend decide si el solicitante puede listar o modificar usuarios. La
 *   interfaz puede ocultar acciones, pero ocultar no es autorizar: un cliente
 *   modificado o una llamada directa a la API podrían intentar la operación.
 * - Autorización por rol frente a validación en backend: la autorización
 *   responde "quién puede ejecutar la operación" y se apoya en el rol o permiso
 *   del usuario autenticado; la validación responde "si los datos y el cambio
 *   solicitado son correctos" (identificador existente, estado permitido,
 *   campos obligatorios, reglas de negocio). Son controles distintos y ambos
 *   deben ejecutarse en el servidor.
 * - Por qué ocultar una acción en la UI no es seguridad: el binario y el
 *   tráfico pueden manipularse, y un atacante puede llamar al endpoint sin usar
 *   esta pantalla. Ocultar un botón solo evita un error al usuario legítimo; la
 *   protección efectiva es que el backend rechace la operación no autorizada.
 *
 * Guía de estudio: `docs/GUIA_MAESTRA_ESTUDIANTE.md`.
 */
package pe.parkeo.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.data.remote.dto.UserDto
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.AdminViewModel

/**
 * Pantalla de administración de usuarios.
 *
 * La lista se filtra localmente por texto y por rol. El texto compara nombre,
 * apellido y correo sin distinguir mayúsculas, y también busca en el teléfono;
 * el rol se resuelve con alias para tolerar variantes del contrato
 * (`OPERATOR`/`OPERADOR`, `USER`/`CLIENTE`). No se modifica el rol desde esta
 * pantalla: solo se consulta y se cambia el estado activo/inactivo.
 *
 * Estados de la vista:
 * - Carga: si `isLoading` es verdadero y la lista aún está vacía.
 * - Vacío: si no hay coincidencias; el mensaje distingue búsqueda de filtro.
 * - Contenido: encabezado con el conteo y tarjetas de usuario.
 * - Error y éxito: snackbar temporal; después se limpian los mensajes.
 *
 * No hay paginación ni creación/edición de usuarios en esta pantalla. El
 * ViewModel carga la lista completa y la vista aplica los filtros en memoria.
 *
 * @param viewModel ViewModel del administrador; aporta la lista de usuarios y
 *   ejecuta los cambios de estado.
 * @param onNavigateBack Callback opcional para volver a la pantalla anterior.
 *   Si es `null`, la barra superior no muestra acción de retroceso.
 */
@Composable
fun AdminUsersScreen(
    viewModel: AdminViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    // Estado remoto y estado local de filtros. Los filtros viven en la
    // composición y no se envían al backend.
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("TODOS") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Lista derivada: se recalcula solo cuando cambian los usuarios o alguno de
    // los filtros. Se aplican dos condiciones combinadas con AND.
    val filteredUsers = remember(uiState.users, searchQuery, selectedRoleFilter) {
        uiState.users.filter { user ->
            // Coincidencia de texto. El teléfono es nullable y se normaliza a
            // cadena vacía; la comparación del teléfono es literal (sin
            // `ignoreCase`), mientras que los demás campos sí lo ignoran.
            val matchesQuery = searchQuery.isBlank() ||
                    user.firstName.contains(searchQuery, ignoreCase = true) ||
                    user.lastName.contains(searchQuery, ignoreCase = true) ||
                    user.email.contains(searchQuery, ignoreCase = true) ||
                    (user.phone ?: "").contains(searchQuery)

            // Coincidencia de rol. `selectedRoleFilter` usa etiquetas de la UI;
            // `user.role` es el rol principal del DTO. Los alias permiten que
            // datos con nombres alternativos sigan apareciendo en el filtro.
            val matchesRole = when (selectedRoleFilter) {
                "ADMIN" -> user.role.contains("ADMIN", ignoreCase = true)
                "OPERADOR" -> user.role.contains("OPERAT", ignoreCase = true) || user.role.contains("OPERADOR", ignoreCase = true)
                "CLIENTE" -> user.role.contains("CLIENTE", ignoreCase = true) || user.role.contains("USER", ignoreCase = true)
                else -> true
            }

            matchesQuery && matchesRole
        }
    }

    // Snackbar para resultados del ViewModel. Se limpia el mensaje después de
    // mostrarlo para que no se repita en recomposiciones o regresos.
    LaunchedEffect(uiState.successMessage, uiState.error) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    // Contenedor principal con barra superior, snackbar y cuerpo.
    Scaffold(
        containerColor = extended.background,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Barra superior: retroceso opcional y recarga de datos.
            ParkeoTopBar(
                title = "Usuarios del Sistema",
                onNavigationClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { viewModel.loadDashboardData(refresh = true) }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Actualizar",
                            tint = extended.textSecondary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Búsqueda: campo controlado por `searchQuery`; el filtrado es
            // inmediato y local, no hay botón de envío.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSm)
            ) {
                ParkeoTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Buscar por nombre, email o teléfono...",
                    leadingIcon = Icons.Filled.Search,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Filtros por rol: una fila de opciones mutuamente excluyentes.
            // "TODOS" desactiva el filtro de rol y conserva el filtro de texto.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingXs),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("TODOS", "ADMIN", "OPERADOR", "CLIENTE").forEach { role ->
                    val isSelected = selectedRoleFilter == role
                    Surface(
                        onClick = { selectedRoleFilter = role },
                        color = if (isSelected) extended.accent else extended.surface2,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            Dimens.borderHairline,
                            if (isSelected) extended.accent else extended.borderSubtle
                        )
                    ) {
                        Text(
                            text = role,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) extended.onAccent else extended.textPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Selección de estado de la lista. La carga inicial tiene prioridad;
            // después se evalúa el vacío y, finalmente, el contenido.
            when {
                uiState.isLoading && uiState.users.isEmpty() -> {
                    // Solo bloquea la vista si aún no existe una lista previa.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        ParkeoLoadingView(message = "Cargando usuarios...")
                    }
                }

                filteredUsers.isEmpty() -> {
                    // Vacío por filtro o por búsqueda sin coincidencias.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        ParkeoEmptyState(
                            title = "No hay resultados",
                            subtitle = if (searchQuery.isBlank())
                                "No hay usuarios registrados con el filtro \"$selectedRoleFilter\""
                            else
                                "No se encontraron usuarios para \"$searchQuery\"",
                            icon = Icons.Filled.PeopleOutline
                        )
                    }
                }

                else -> {
                    // Lista de usuarios filtrados, identificada por `id`.
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(Dimens.spacingMd),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                    ) {
                        item {
                            // Encabezado con el conteo del resultado actual.
                            Text(
                                text = "${filteredUsers.size} USUARIOS ENCONTRADOS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = extended.textTertiary
                            )
                        }

                        items(filteredUsers, key = { it.id }) { user ->
                            // El callback no recibe el valor nuevo del switch:
                            // el ViewModel invierte `user.isActive` y envía el
                            // resultado al backend.
                            AdminUserCard(
                                user = user,
                                onToggleStatus = {
                                    viewModel.toggleUserStatus(user.id, user.isActive)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de un usuario dentro de la lista administrativa.
 *
 * Deriva el rol visual a partir de `user.role` con alias tolerantes a variantes
 * del contrato. Un rol no reconocido se presenta como cliente por defecto; eso
 * es solo una decisión de presentación y no cambia el rol real del usuario ni
 * sus permisos. El switch muestra `user.isActive` y notifica la intención de
 * cambio; el ViewModel calcula el valor opuesto y llama al backend.
 *
 * Ocultar o mostrar esta tarjeta no protege una cuenta: la autorización real
 * vive en el servidor, que debe comprobar rol y estado del solicitante en cada
 * operación. Desactivar un usuario es un cambio de negocio que debe validarse
 * en backend, no una simple preferencia visual.
 *
 * @param user DTO del usuario con nombre, correo, teléfono, rol y estado.
 * @param onToggleStatus Acción sin parámetros que solicita invertir el estado
 *   activo/inactivo del usuario. La decisión del nuevo valor queda en el ViewModel.
 */
@Composable
private fun AdminUserCard(
    user: UserDto,
    onToggleStatus: () -> Unit
) {
    val extended = ParkeoTheme.colors
    // Normalización local del rol. `uppercase()` permite comparar sin depender
    // de mayúsculas/minúsculas. Se aceptan las variantes OPERAT/OPERADOR.
    val roleUpper = user.role.uppercase()
    val isAdmin = roleUpper.contains("ADMIN")
    val isOperator = roleUpper.contains("OPERAT") || roleUpper.contains("OPERADOR")

    // Badge e icono derivados del rol. `ParkeoBadgeStatus` es un enum de
    // presentación y estos valores no otorgan ni representan permisos reales.
    val roleBadge = when {
        isAdmin -> ParkeoBadgeStatus.RoleAdmin
        isOperator -> ParkeoBadgeStatus.RoleOperator
        else -> ParkeoBadgeStatus.RoleClient
    }

    val avatarIcon = when {
        isAdmin -> Icons.Filled.AdminPanelSettings
        isOperator -> Icons.Filled.Badge
        else -> Icons.Filled.Person
    }

    ParkeoCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.spacingLg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar: círculo con icono según el rol, sin foto remota.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(extended.surface2)
                    .border(Dimens.borderHairline, extended.borderSubtle, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = avatarIcon,
                    contentDescription = null,
                    tint = extended.accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Información del usuario: nombre, rol, correo y teléfono opcional.
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Nombre con elipsis si no cabe; el badge conserva su
                    // espacio porque el texto usa `fill = false`.
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = extended.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    ParkeoBadge(status = roleBadge)
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textSecondary
                )

                // El teléfono es nullable: se omite la fila si falta o está en
                // blanco, en lugar de mostrar "Tel: null".
                if (!user.phone.isNullOrBlank()) {
                    Text(
                        text = "Tel: ${user.phone}",
                        style = Typography.MonospaceTechnical,
                        fontSize = 11.sp,
                        color = extended.textTertiary
                    )
                }
            }

            // Estado de la cuenta: switch y etiqueta textual. El color refuerza
            // el significado, pero la etiqueta evita depender solo del color.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Switch(
                    checked = user.isActive,
                    onCheckedChange = { onToggleStatus() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = extended.onAccent,
                        checkedTrackColor = extended.accent,
                        uncheckedThumbColor = extended.textTertiary,
                        uncheckedTrackColor = extended.surface3
                    ),
                    modifier = Modifier.height(28.dp)
                )
                Text(
                    text = if (user.isActive) "Activo" else "Inactivo",
                    fontSize = 10.sp,
                    color = if (user.isActive) extended.signalGreen else extended.signalRed,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
