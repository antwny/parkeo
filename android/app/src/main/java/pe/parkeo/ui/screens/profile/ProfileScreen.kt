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
    val userProfile by viewModel.userProfile.collectAsState()
    val extended = ParkeoTheme.colors
    var showLogoutDialog by remember { mutableStateOf(false) }

    val roleUpper = (userProfile?.role ?: "").uppercase()
    val isAdmin = roleUpper.contains("ADMIN")
    val isOperator = roleUpper.contains("OPERAT")

    val roleBadgeStatus = when {
        isAdmin -> ParkeoBadgeStatus.Admin
        isOperator -> ParkeoBadgeStatus.Operator
        else -> ParkeoBadgeStatus.Client
    }

    val avatarIcon = when {
        isAdmin -> Icons.Filled.AdminPanelSettings
        isOperator -> Icons.Filled.Badge
        else -> Icons.Filled.Person
    }

    Scaffold(
        containerColor = extended.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
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
            // Identity Hero Card
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

            // Banner contextual por rol
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

            // Secciones operativas específicas por Rol
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

                        ProfileOptionTile(
                            icon = Icons.Filled.Dns,
                            title = "Arquitectura del Sistema",
                            subtitle = "Spring Boot 3.2.0 • MySQL 8.0 • JWT Auth",
                            onClick = {}
                        )

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

                        ProfileOptionTile(
                            icon = Icons.Filled.NotificationsActive,
                            title = "Recordatorios de reserva",
                            subtitle = "Alertas locales 15 min antes de tu llegada",
                            onClick = {}
                        )

                        ProfileOptionTile(
                            icon = Icons.Filled.Security,
                            title = "Seguridad y privacidad",
                            subtitle = "Tus datos personales y vehículos protegidos",
                            onClick = {}
                        )

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

            // Logout Button
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
