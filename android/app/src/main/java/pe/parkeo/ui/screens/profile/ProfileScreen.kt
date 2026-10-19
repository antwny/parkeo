/*
 * PARKeo · Android
 * Archivo: ProfileScreen.kt
 * Capa: presentación (perfil del usuario autenticado).
 * Propósito: mostrar identidad y rol, ofrecer accesos contextuales según ese rol y cerrar
 * sesión con confirmación.
 * Conceptos clave: Composable, StateFlow, collectAsState, ParkeoBadge, ParkeoCard,
 * ParkeoTopBar, ParkeoButton destructivo, AlertDialog, estado local con remember, callbacks
 * opcionales y logout delegado al ViewModel y al grafo.
 * Flujo: AuthViewModel entrega `userProfile`; la pantalla calcula el rol, elige insignia e
 * icono, muestra el banner y las secciones correspondientes. Los accesos invocan callbacks
 * que, dentro de MainScreen, cambian de pestaña; el cierre de sesión abre un diálogo y,
 * al confirmar, llama a `logout()` y a `onLogout`.
 * Relación con otros archivos: ParkeoNavGraph.kt conecta la variante de perfil como destino
 * independiente; MainScreen.kt lo usa como pestaña y pasa subconjuntos de callbacks;
 * AuthViewModel.kt expone el perfil y la acción de logout; ParkeoBadge y ParkeoCard
 * aportan la presentación.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones de Compose, estado y roles).
 */
package pe.parkeo.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.AuthViewModel

/**
 * Pantalla de perfil reutilizable por los tres roles.
 *
 * El perfil se observa desde `AuthViewModel`, que lo obtiene de la sesión persistida. Si
 * todavía no llegó, se usan textos de reserva ("Usuario Parkeo" y correo vacío) en lugar de
 * bloquear la pantalla; la insignia se calcula con el rol disponible. La pantalla no hace
 * llamadas de red propias: los datos y el logout pertenecen al ViewModel.
 *
 * Muchos callbacks tienen valor por defecto `{}` porque la pantalla se usa en dos contextos:
 * como pestaña de `MainScreen`, donde cambian el índice de pestaña, y como destino
 * independiente del grafo, donde los callbacks conectados navegan a rutas reales. Los
 * accesos informativos que aún no tienen flujo asociado usan `onClick = {}` y se documentan
 * como tal.
 *
 * @param viewModel ViewModel de autenticación con el perfil y la acción `logout`.
 * @param onNavigateBack vuelve a la pantalla o pestaña anterior.
 * @param onNavigateToVehicles abre la gestión de vehículos del cliente.
 * @param onNavigateToReservations abre las reservas del cliente u operador.
 * @param onNavigateToExplore abre la exploración de cocheras.
 * @param onNavigateToSpaces abre la matriz de espacios del operador.
 * @param onNavigateToDashboard abre el panel de métricas del administrador.
 * @param onNavigateToParkingLots abre la gestión de cocheras del administrador.
 * @param onNavigateToUsers abre la gestión de usuarios del administrador.
 * @param onLogout cierra la sesión y navega fuera del área autenticada.
 */
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToVehicles: () -> Unit = {},
    onNavigateToReservations: () -> Unit = {},
    onNavigateToExplore: () -> Unit = {},
    onNavigateToSpaces: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToParkingLots: () -> Unit = {},
    onNavigateToUsers: () -> Unit = {},
    onLogout: () -> Unit
) {
    // Perfil observable; puede ser null durante la primera lectura de la sesión.
    val userProfile by viewModel.userProfile.collectAsState()
    val extended = ParkeoTheme.colors

    // Estado local del diálogo de confirmación de cierre de sesión.
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Normalización y clasificación del rol para elegir insignia, avatar y secciones.
    val roleUpper = (userProfile?.role ?: "").uppercase()
    val isAdmin = roleUpper.contains("ADMIN")
    val isOperator = roleUpper.contains("OPERAT")

    // Insignia visual del rol. ParkeoBadge muestra el texto y los colores semánticos.
    val roleBadgeStatus = when {
        isAdmin -> ParkeoBadgeStatus.Admin
        isOperator -> ParkeoBadgeStatus.Operator
        else -> ParkeoBadgeStatus.Client
    }

    // Icono del avatar coherente con el rol.
    val avatarIcon = when {
        isAdmin -> Icons.Filled.AdminPanelSettings
        isOperator -> Icons.Filled.Badge
        else -> Icons.Filled.Person
    }

    Scaffold(
        containerColor = extended.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            // Barra superior con título y flecha de regreso.
            ParkeoTopBar(
                title = "Mi perfil",
                onNavigationClick = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.spacingMd),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
        ) {
            // Identidad: avatar, nombre, correo e insignia. Los campos se leen del perfil
            // persistido; si aún no hay nombre se usa un texto de reserva.
            ParkeoCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(Dimens.spacingLg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(extended.surface2)
                            .border(Dimens.borderHairline, extended.borderSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = avatarIcon,
                            contentDescription = null,
                            tint = extended.accent,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile?.name?.ifBlank { "Usuario Parkeo" } ?: "Usuario Parkeo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = extended.textPrimary
                        )

                        Text(
                            text = userProfile?.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textSecondary
                        )

                        Spacer(Modifier.height(6.dp))

                        ParkeoBadge(status = roleBadgeStatus)
                    }
                }
            }

            // Banner contextual: explica responsabilidades del rol. El cliente no tiene
            // banner porque su experiencia se describe en las secciones de cuenta.
            when {
                isAdmin -> {
                    ParkeoCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentBorder = true
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimens.spacingLg),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = extended.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Administración Central",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = extended.textPrimary
                                )
                            }
                            Text(
                                text = "Supervisión total de la plataforma: consulta métricas financieras, activa o suspende cocheras y gestiona los accesos de operadores y usuarios.",
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.textSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
                isOperator -> {
                    ParkeoCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentBorder = true
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimens.spacingLg),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocalParking,
                                    contentDescription = null,
                                    tint = extended.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Control de Cochera en Tiempo Real",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = extended.textPrimary
                                )
                            }
                            Text(
                                text = "Tienes a cargo la verificación de ingresos (Check-In) y salidas (Check-Out) de vehículos, además de la supervisión de la matriz de espacios libres y ocupados.",
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.textSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Secciones operativas por rol: cada bloque agrupa acciones relacionadas y
            // usa los callbacks recibidos para cambiar de pestaña o navegar.
            when {
                isAdmin -> {
                    // SECCIÓN ADMIN: GESTIÓN ADMINISTRATIVA
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                        Text(
                            text = "GESTIÓN ADMINISTRATIVA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = extended.textTertiary,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )

                        ProfileOptionTile(
                            icon = Icons.Filled.Analytics,
                            title = "Dashboard de Métricas",
                            subtitle = "Recaudación, ocupación y reservas globales",
                            onClick = onNavigateToDashboard
                        )

                        ProfileOptionTile(
                            icon = Icons.Filled.LocalParking,
                            title = "Estacionamientos y Cocheras",
                            subtitle = "Horarios, capacidad y operadores asignados",
                            onClick = onNavigateToParkingLots
                        )

                        ProfileOptionTile(
                            icon = Icons.Filled.People,
                            title = "Gestión de Usuarios y Roles",
                            subtitle = "Conductores, operadores y administradores",
                            onClick = onNavigateToUsers
                        )

                        ProfileOptionTile(
                            icon = Icons.Filled.Explore,
                            title = "Vista de Exploración de Cocheras",
                            subtitle = "Visualizar el mapa y tarifas públicas",
                            onClick = onNavigateToExplore
                        )
                    }

                    // SECCIÓN ADMIN: INFRAESTRUCTURA
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                        Text(
                            text = "INFRAESTRUCTURA Y SISTEMA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = extended.textTertiary,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )

                        // Elemento informativo: no navega en esta versión (onClick vacío).
                        ProfileOptionTile(
                            icon = Icons.Filled.Dns,
                            title = "Arquitectura del Sistema",
                            subtitle = "Spring Boot 3.2.0 • MySQL 8.0 • JWT Auth",
                            onClick = {}
                        )

                        // Elemento informativo: no abre una pantalla de auditoría.
                        ProfileOptionTile(
                            icon = Icons.Filled.Security,
                            title = "Auditoría y Políticas de Seguridad",
                            subtitle = "Registros de acceso y roles protegidos",
                            onClick = {}
                        )
                    }
                }

                isOperator -> {
                    // SECCIÓN OPERADOR: OPERACIÓN DE COCHERA
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                        Text(
                            text = "OPERACIONES DE COCHERA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = extended.textTertiary,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )

                        ProfileOptionTile(
                            icon = Icons.Filled.GridView,
                            title = "Matriz Visual de Espacios",
                            subtitle = "Monitorear espacios libres, ocupados y reservados",
                            onClick = onNavigateToSpaces
                        )

                        ProfileOptionTile(
                            icon = Icons.Filled.ConfirmationNumber,
                            title = "Control de Reservas (Ingresos y Salidas)",
                            subtitle = "Validar código PKO y registrar Check-In / Check-Out",
                            onClick = onNavigateToReservations
                        )

                        ProfileOptionTile(
                            icon = Icons.Filled.Explore,
                            title = "Explorar Cocheras de la Red",
                            subtitle = "Consultar tarifas y disponibilidad de otras sedes",
                            onClick = onNavigateToExplore
                        )
                    }

                    // SECCIÓN OPERADOR: COCHERAS ASIGNADAS Y HERRAMIENTAS
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                        Text(
                            text = "COCHERAS A TU CARGO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = extended.textTertiary,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )

                        ProfileOptionTile(
                            icon = Icons.Filled.Business,
                            title = "Cocheras Asignadas en Lima",
                            subtitle = "Miraflores, San Isidro, Surco, Barranco, Lince, San Borja",
                            onClick = onNavigateToSpaces
                        )

                        // Elemento informativo: no navega; solo comunica el estado de la
                        // sesión operativa.
                        ProfileOptionTile(
                            icon = Icons.Filled.Security,
                            title = "Seguridad de Acceso",
                            subtitle = "Sesión operativa encriptada",
                            onClick = {}
                        )
                    }
                }

                else -> {
                    // SECCIÓN CLIENTE: MI CUENTA Y VEHÍCULOS
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                        Text(
                            text = "MI CUENTA Y VEHÍCULOS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = extended.textTertiary,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )

                        ProfileOptionTile(
                            icon = Icons.Filled.DirectionsCar,
                            title = "Mis vehículos registrados",
                            subtitle = "Autos, motos y camionetas asociados",
                            onClick = onNavigateToVehicles
                        )

                        ProfileOptionTile(
                            icon = Icons.Filled.BookmarkBorder,
                            title = "Mis reservas y tickets",
                            subtitle = "Próximas reservas, historial y comprobantes",
                            onClick = onNavigateToReservations
                        )

                        ProfileOptionTile(
                            icon = Icons.Filled.Search,
                            title = "Buscar y reservar cochera",
                            subtitle = "Encuentra estacionamientos cercanos por GPS",
                            onClick = onNavigateToExplore
                        )
                    }

                    // SECCIÓN CLIENTE: PREFERENCIAS Y SOPORTE
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)) {
                        Text(
                            text = "PREFERENCIAS Y SOPORTE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = extended.textTertiary,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )

                        // Elemento informativo: la configuración de recordatorios aún no
                        // tiene pantalla propia; onClick vacío.
                        ProfileOptionTile(
                            icon = Icons.Filled.NotificationsActive,
                            title = "Recordatorios de reserva",
                            subtitle = "Alertas locales 15 min antes de tu llegada",
                            onClick = {}
                        )

                        // Elemento informativo: no abre un detalle de privacidad.
                        ProfileOptionTile(
                            icon = Icons.Filled.Security,
                            title = "Seguridad y privacidad",
                            subtitle = "Tus datos personales y vehículos protegidos",
                            onClick = {}
                        )

                        // Elemento informativo acerca de la versión; sin destino asociado.
                        ProfileOptionTile(
                            icon = Icons.Filled.Info,
                            title = "Acerca de Parkeo v1.0",
                            subtitle = "Movilidad urbana en tiempo real",
                            onClick = {}
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Acción destructiva: no cierra la sesión directamente; abre el diálogo para
            // evitar un logout accidental. El estilo rojo comunica irreversibilidad.
            ParkeoButton(
                text = "Cerrar sesión",
                onClick = { showLogoutDialog = true },
                leadingIcon = Icons.Filled.Logout,
                style = ParkeoButtonStyle.Destructive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeightLarge)
            )

            Spacer(Modifier.height(Dimens.spacingLg))
        }

        // Confirmación de cierre de sesión. "Cancelar" y el toque fuera cierran el diálogo;
        // "Cerrar sesión" dispara la limpieza en el ViewModel y la navegación del grafo.
        // `logout()` es asíncrono, pero `onLogout` se ejecuta de inmediato para no dejar al
        // usuario en una pantalla autenticada mientras termina la limpieza.
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                containerColor = extended.surface1,
                title = {
                    Text(
                        text = "Cerrar sesión",
                        fontWeight = FontWeight.Bold,
                        color = extended.textPrimary
                    )
                },
                text = {
                    Text(
                        text = "¿Estás seguro de que deseas cerrar tu sesión en Parkeo?",
                        color = extended.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        onLogout()
                    }) {
                        Text("Cerrar sesión", color = extended.signalRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancelar", color = extended.textSecondary)
                    }
                }
            )
        }
    }
}

/**
 * Tarjeta de acceso del perfil.
 *
 * Muestra un icono, un título, un subtítulo opcional y un chevron. Toda la tarjeta es
 * clicable a través de `ParkeoCard(onClick = ...)`. El chevron es decorativo y no se anuncia
 * por separado; el texto del título actúa como etiqueta accesible del control.
 *
 * @param icon icono representativo de la sección o acción.
 * @param title título visible del acceso.
 * @param subtitle explicación breve; si es nulo o está en blanco, no se muestra.
 * @param onClick acción que se ejecuta al pulsar la tarjeta.
 */
@Composable
private fun ProfileOptionTile(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val extended = ParkeoTheme.colors
    ParkeoCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacingLg, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = extended.accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = extended.textPrimary
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = extended.textTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
