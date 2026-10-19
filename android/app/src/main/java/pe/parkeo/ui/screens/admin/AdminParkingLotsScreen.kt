/*
 * PARKeo · Android
 * Archivo: `android/app/src/main/java/pe/parkeo/ui/screens/admin/AdminParkingLotsScreen.kt`
 * Capa: presentación (Jetpack Compose), rol administrador (`ADMIN`).
 *
 * Propósito:
 * Listar los estacionamientos de la red, permitir buscarlos por nombre, dirección
 * o distrito, cambiar su disponibilidad operativa (`isOpen`) y su actividad en
 * la red (`isActive`), y asignarles un operador. También ofrece navegación al
 * detalle de un local mediante un callback.
 *
 * Conceptos clave:
 * - Rol `ADMIN`: la pantalla está pensada para ese perfil, pero la autorización
 *   real de cada operación la decide el backend; ocultar o mostrar controles en
 *   Compose es solo experiencia de usuario.
 * - CRUD parcial: esta pantalla implementa lectura, actualización de estado y
 *   asignación de operador. El alta y la edición completa de datos no tienen
 *   formulario aquí; el callback `onSelectLotDetail` permite abrir el detalle.
 * - Campos visibles: nombre, dirección, distrito, capacidad total, espacios
 *   disponibles, estado abierto/cerrado, estado activo/inactivo y operador
 *   asignado. Las coordenadas, tarifas, horarios y servicios no se muestran en
 *   esta vista; pertenecen al detalle del estacionamiento y a sus formularios.
 * - Validaciones: aquí no hay formulario de alta/edición ni validación de
 *   campos. La única validación local es el filtro de búsqueda. Los cambios de
 *   estado y la asignación de operador se envían al backend, que debe validar
 *   rol, permisos, existencia, estado del local y del operador, y reglas de
 *   negocio. El cliente puede mejorar los mensajes, pero no sustituye esa
 *   validación. Los switches se aplican sin diálogo de confirmación.
 * - `Compose`: la lista se deriva del estado con `remember` y se redibuja al
 *   cambiar la búsqueda, los datos o los filtros.
 * - `StateFlow`: `AdminViewModel.uiState` aporta `parkingLots`, `users`,
 *   `isLoading`, `error` y `successMessage`.
 * - `LazyColumn`: renderiza solo los elementos visibles de la lista filtrada.
 * - Diálogo: `AssignOperatorDialog` selecciona un operador de la lista de
 *   usuarios y confirma la asignación a través del ViewModel.
 *
 * Flujo de usuario y datos:
 * 1. La pantalla observa el estado y calcula en memoria `filteredLots` a partir
 *    de `searchQuery`; la búsqueda no dispara una petición al servidor.
 * 2. Al cambiar un `Switch` se llama a `viewModel.toggleParkingLotStatus(...)`
 *    con el valor nuevo y el estado complementario actual.
 * 3. Al tocar la franja del operador se abre el diálogo, se filtran localmente
 *    los usuarios con rol de operador y, al confirmar, se llama a
 *    `viewModel.assignOperator(...)`.
 * 4. El resultado exitoso o el error se comunica mediante un snackbar; luego
 *    se limpian los mensajes del estado para no repetirlos.
 *
 * Relación con ViewModel, navegación y backend:
 * - `AdminViewModel` centraliza las llamadas al repositorio y actualiza la lista
 *   de estacionamientos con la respuesta del servidor.
 * - `onNavigateBack` y `onSelectLotDetail` desacoplan esta pantalla del
 *   `NavGraph`; la pantalla no conoce rutas concretas.
 * - El backend valida rol, estado y reglas de negocio. El filtrado de operadores
 *   por nombre de rol en el cliente es una ayuda visual, no un control de
 *   seguridad: un cliente modificado podría intentar asignar otro identificador
 *   y el servidor debe rechazarlo si no corresponde.
 *
 * Guía de estudio: `docs/GUIA_MAESTRA_ESTUDIANTE.md`.
 */
package pe.parkeo.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.data.remote.dto.ParkingLotDto
import pe.parkeo.data.remote.dto.UserDto
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.AdminViewModel

/**
 * Pantalla de administración de estacionamientos.
 *
 * Mantiene dos estados locales de interfaz: `searchQuery` (texto del buscador)
 * y `lotToAssignOperator` (local cuyo diálogo de asignación está abierto).
 * El resto del estado proviene del `AdminViewModel`.
 *
 * La búsqueda es local e insensible a mayúsculas: compara nombre, dirección y
 * distrito. El ViewModel carga la lista completa; esta pantalla no implementa
 * paginación ni consultas remotas por texto. Los filtros de estado no existen
 * como chips: el estado se cambia directamente con los `Switch` de cada tarjeta.
 *
 * Estados de la vista:
 * - Carga: si `isLoading` es verdadero y todavía no hay estacionamientos.
 * - Vacío: si el resultado filtrado no tiene elementos; distingue entre lista
 *   realmente vacía y búsqueda sin coincidencias.
 * - Contenido: encabezado con el conteo y lista de tarjetas.
 * - Error y éxito: se muestran como snackbar y se limpian del estado.
 *
 * @param viewModel ViewModel del administrador; aporta la lista y ejecuta los
 *   cambios de estado y la asignación de operador.
 * @param onNavigateBack Callback opcional para volver. Si es `null`, la barra
 *   superior no muestra acción de navegación.
 * @param onSelectLotDetail Callback opcional que recibe el identificador del
 *   local para abrir su detalle o edición en otra pantalla.
 */
@Composable
fun AdminParkingLotsScreen(
    viewModel: AdminViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onSelectLotDetail: ((Long) -> Unit)? = null
) {
    // Estado remoto observado y estado de interfaz recordado entre recomposiciones.
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    var lotToAssignOperator by remember { mutableStateOf<ParkingLotDto?>(null) }

    // Búsqueda local. `remember` evita recalcular el filtro si ninguno de sus
    // insumos cambia. Se conserva el orden original de la lista del servidor.
    val filteredLots = remember(uiState.parkingLots, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.parkingLots
        } else {
            uiState.parkingLots.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.address.contains(searchQuery, ignoreCase = true) ||
                        // `district` es nullable: se normaliza a cadena vacía
                        // para poder aplicar `contains` sin riesgo de nulo.
                        (it.district ?: "").contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Comunicación de resultados: los mensajes del ViewModel se muestran una
    // vez en el snackbar y luego se limpian para que no se repitan al recomponer.
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

    // Contenedor principal: barra superior, snackbar y cuerpo.
    Scaffold(
        containerColor = extended.background,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Barra superior con retroceso opcional y refresco manual de datos.
            ParkeoTopBar(
                title = "Estacionamientos",
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
            // Búsqueda: campo controlado por `searchQuery`. El ícono es
            // decorativo; no hay botón de limpieza explícito.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSm)
            ) {
                ParkeoTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Buscar cochera o distrito...",
                    leadingIcon = Icons.Filled.Search,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Selección de estado visual. El `when` prioriza la carga inicial;
            // después decide entre vacío y lista.
            when {
                uiState.isLoading && uiState.parkingLots.isEmpty() -> {
                    // Estado de carga inicial: solo si aún no hay datos que
                    // mostrar. Un refresco con datos existentes no cae aquí.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        ParkeoLoadingView(message = "Cargando estacionamientos...")
                    }
                }

                filteredLots.isEmpty() -> {
                    // Estado vacío: el subtítulo explica si el sistema no tiene
                    // locales o si la búsqueda no encontró coincidencias.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        ParkeoEmptyState(
                            title = "No hay resultados",
                            subtitle = if (searchQuery.isBlank())
                                "No hay estacionamientos registrados en el sistema"
                            else
                                "No se encontraron estacionamientos para \"$searchQuery\"",
                            icon = Icons.Filled.LocalParking
                        )
                    }
                }

                else -> {
                    // Lista de estacionamientos filtrados. La clave `id` ayuda
                    // a Compose a conservar identidad entre recomposiciones.
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(Dimens.spacingMd),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                    ) {
                        item {
                            // Encabezado con el tamaño del resultado filtrado.
                            Text(
                                text = "${filteredLots.size} ESTACIONAMIENTOS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = extended.textTertiary
                            )
                        }

                        items(filteredLots, key = { it.id }) { lot ->
                            // Cada tarjeta recibe acciones concretas. Los
                            // toggles envían el valor nuevo junto con el otro
                            // estado actual para no pisarlo en el backend.
                            AdminParkingLotCard(
                                lot = lot,
                                onToggleOpen = { isOpen ->
                                    viewModel.toggleParkingLotStatus(lot.id, isOpen = isOpen, isActive = lot.isActive)
                                },
                                onToggleActive = { isActive ->
                                    viewModel.toggleParkingLotStatus(lot.id, isOpen = lot.isOpen, isActive = isActive)
                                },
                                onAssignOperator = { lotToAssignOperator = lot },
                                onClick = { onSelectLotDetail?.invoke(lot.id) }
                            )
                        }
                    }
                }
            }
        }

        // Diálogo de asignación. Se abre solo cuando hay un local seleccionado.
        lotToAssignOperator?.let { lot ->
            // Filtro local de operadores. `role` es el rol principal y `roles`
            // la lista completa; se revisan ambos por compatibilidad con el DTO.
            // Este filtro es una comodidad de UI, no una garantía de seguridad.
            val operators = remember(uiState.users) {
                uiState.users.filter {
                    it.role.contains("OPERATOR", ignoreCase = true) ||
                            it.roles.any { r -> r.contains("OPERATOR", ignoreCase = true) }
                }
            }
            AssignOperatorDialog(
                lot = lot,
                operators = operators,
                onDismiss = { lotToAssignOperator = null },
                onConfirm = { opId ->
                    viewModel.assignOperator(lot.id, opId)
                    lotToAssignOperator = null
                }
            )
        }
    }
}

/**
 * Tarjeta de un estacionamiento dentro de la lista administrativa.
 *
 * Resume los datos que el administrador necesita para supervisar el local:
 * identidad y ubicación, estado operativo y de red, capacidad y operador.
 * La tarjeta es una vista controlada: no muta el DTO; notifica intenciones al
 * llamador mediante callbacks, y es el ViewModel quien actualiza el estado.
 *
 * El borde de acento se activa solo cuando el local está abierto y activo, como
 * señal visual rápida de que puede operar. Los badges distinguen dos dimensiones
 * distintas: `isOpen` (atiende o no en este momento) e `isActive` (participa o no
 * en la red). Un local puede estar activo pero cerrado, por ejemplo fuera de
 * horario.
 *
 * @param lot DTO con los datos del estacionamiento y del operador asignado.
 * @param onToggleOpen Notifica el nuevo valor de `isOpen` al accionar el switch.
 * @param onToggleActive Notifica el nuevo valor de `isActive` al accionar el switch.
 * @param onAssignOperator Solicita abrir el diálogo para asignar o reasignar
 *   operador; se dispara al tocar la franja del operador.
 * @param onClick Solicita abrir el detalle del local; se dispara al tocar la
 *   tarjeta fuera de los controles interactivos.
 */
@Composable
private fun AdminParkingLotCard(
    lot: ParkingLotDto,
    onToggleOpen: (Boolean) -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onAssignOperator: () -> Unit,
    onClick: () -> Unit
) {
    val extended = ParkeoTheme.colors

    ParkeoCard(
        modifier = Modifier.fillMaxWidth(),
        accentBorder = lot.isOpen && lot.isActive
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.spacingLg)
        ) {
            // Encabezado: nombre, dirección y badges de estado.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lot.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = extended.textPrimary
                    )
                    // El distrito es opcional; si existe se agrega precedido
                    // de coma, sin dejar separadores vacíos cuando es nulo.
                    Text(
                        text = "${lot.address}${lot.district?.let { ", $it" } ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textSecondary
                    )
                }

                // Badges de estado. Las etiquetas se sobrescriben para mostrar
                // vocabulario administrativo ("Abierto"/"Activo") en lugar del
                // texto por defecto del componente. `ParkeoBadgeStatus` es un
                // enum de presentación: sus valores no son estados de negocio
                // ni permisos, solo el estilo visual del badge.
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ParkeoBadge(
                        status = if (lot.isOpen) ParkeoBadgeStatus.Available else ParkeoBadgeStatus.Closed,
                        labelOverride = if (lot.isOpen) "Abierto" else "Cerrado"
                    )
                    ParkeoBadge(
                        status = if (lot.isActive) ParkeoBadgeStatus.Admin else ParkeoBadgeStatus.Occupied,
                        labelOverride = if (lot.isActive) "Activo" else "Inactivo"
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Capacidad y disponibilidad. `totalCapacity` y `availableSpaces`
            // son propiedades calculadas del DTO; no se editan desde aquí.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = extended.textTertiary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Capacidad: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textSecondary
                    )
                    Text(
                        text = "${lot.totalCapacity}",
                        style = Typography.MonospaceTechnical,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = extended.textPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = extended.accent
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Disponibles: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textSecondary
                    )
                    Text(
                        text = "${lot.availableSpaces}",
                        style = Typography.MonospaceTechnical,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = extended.accent
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Operador asignado. La franja completa abre el diálogo. Si no hay
            // operador se usa un borde y texto de advertencia para resaltarlo.
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAssignOperator),
                color = extended.surface2,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    Dimens.borderHairline,
                    if (lot.operatorName != null) extended.borderSubtle else extended.signalAmber.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Badge,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (lot.operatorName != null) extended.accent else extended.signalAmber
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        // `operatorName` es nullable: el texto y el color
                        // comunican si existe una asignación vigente.
                        Text(
                            text = if (lot.operatorName != null) "Operador: ${lot.operatorName}" else "Sin operador asignado",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (lot.operatorName != null) extended.textPrimary else extended.signalAmber,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (lot.operatorName != null) "Toca para reasignar" else "Toca para asignar operador",
                            style = MaterialTheme.typography.labelSmall,
                            color = extended.textTertiary,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = if (lot.operatorName != null) "Cambiar" else "Asignar",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = extended.accent
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = extended.accent
                    )
                }
            }

            HorizontalDivider(
                color = extended.borderSubtle,
                thickness = Dimens.borderHairline,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Controles de estado. Cada switch refleja el valor actual y envía
            // el valor nuevo al callback; la persistencia la realiza el backend.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Abierto:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = extended.textPrimary
                    )
                    Switch(
                        checked = lot.isOpen,
                        onCheckedChange = { onToggleOpen(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = extended.onAccent,
                            checkedTrackColor = extended.accent,
                            uncheckedThumbColor = extended.textTertiary,
                            uncheckedTrackColor = extended.surface3
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Activo en red:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = extended.textPrimary
                    )
                    Switch(
                        checked = lot.isActive,
                        onCheckedChange = { onToggleActive(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = extended.onAccent,
                            checkedTrackColor = extended.accent,
                            uncheckedThumbColor = extended.textTertiary,
                            uncheckedTrackColor = extended.surface3
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * Diálogo modal para asignar o reasignar un operador a un estacionamiento.
 *
 * La selección inicial prioriza al operador ya asignado; si no existe, usa el
 * primer operador disponible. Si la lista está vacía, el identificador queda en
 * `0L`, el botón de confirmación se deshabilita y se muestra un aviso. El
 * diálogo no conoce el repositorio: solo entrega el `id` elegido mediante
 * `onConfirm`, y el llamador decide cómo persistirlo.
 *
 * Los operadores recibidos ya vienen filtrados por rol desde la pantalla. Esa
 * lista es una ayuda de interfaz: el backend debe verificar que el usuario
 * exista, esté activo, tenga rol de operador y que el administrador esté
 * autorizado para asignarlo. Ocultar o filtrar opciones en Compose no impide
 * que un cliente manipulado envíe otro identificador.
 *
 * @param lot Estacionamiento destino; se usa para el nombre y para preseleccionar
 *   su operador actual.
 * @param operators Usuarios candidatos con rol de operador.
 * @param onDismiss Acción al cancelar o tocar fuera del diálogo.
 * @param onConfirm Acción con el identificador del operador seleccionado.
 */
@Composable
private fun AssignOperatorDialog(
    lot: ParkingLotDto,
    operators: List<UserDto>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val extended = ParkeoTheme.colors
    // Selección recordada mientras el diálogo permanezca en composición. Se
    // usa el operador actual, luego el primero disponible y, por último, 0L
    // como centinela de "sin selección".
    var selectedOperatorId by remember {
        mutableStateOf(lot.operatorId ?: operators.firstOrNull()?.id ?: 0L)
    }

    // Diálogo: título, contenido seleccionable y acciones.
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extended.surface1,
        titleContentColor = extended.textPrimary,
        textContentColor = extended.textSecondary,
        title = {
            Text(
                text = "Asignar Operador",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Texto de contexto: explica el efecto de la asignación.
                Text(
                    text = "Selecciona el operador encargado de gestionar las entradas y salidas de \"${lot.name}\":",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textSecondary
                )

                if (operators.isEmpty()) {
                    // Estado vacío del diálogo: no hay candidatos disponibles.
                    Text(
                        text = "No hay operadores registrados en el sistema.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.signalAmber
                    )
                } else {
                    // Lista de operadores con altura máxima para que el diálogo
                    // no desborde la pantalla; se desplaza internamente.
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(operators, key = { it.id }) { operator ->
                            val isSelected = operator.id == selectedOperatorId
                            // Opción seleccionable. El estado visual se deriva
                            // de `selectedOperatorId`; tocar la fila o el radio
                            // actualiza la misma fuente de verdad.
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedOperatorId = operator.id },
                                color = if (isSelected) extended.accent.copy(alpha = 0.15f) else extended.surface2,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    Dimens.borderHairline,
                                    if (isSelected) extended.accent else extended.borderSubtle
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        // Nombre completo; `trim()` evita un
                                        // espacio final si falta un apellido.
                                        Text(
                                            text = "${operator.firstName} ${operator.lastName}".trim(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = extended.textPrimary
                                        )
                                        Text(
                                            text = operator.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = extended.textTertiary
                                        )
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedOperatorId = operator.id },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = extended.accent,
                                            unselectedColor = extended.textTertiary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            // Confirmar solo se habilita con un identificador válido distinto
            // de 0L; la comprobación también protege el callback.
            ParkeoButton(
                text = "Asignar",
                onClick = {
                    if (selectedOperatorId != 0L) {
                        onConfirm(selectedOperatorId)
                    }
                },
                enabled = selectedOperatorId != 0L
            )
        },
        dismissButton = {
            // Cancelar cierra sin llamar al ViewModel ni modificar datos.
            ParkeoButton(
                text = "Cancelar",
                onClick = onDismiss,
                style = ParkeoButtonStyle.Ghost
            )
        }
    )
}

