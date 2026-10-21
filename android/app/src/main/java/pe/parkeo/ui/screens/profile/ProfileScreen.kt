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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pe.parkeo.ParkeoApplication
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
    onNavigateToAdminReservations: () -> Unit = {},
    onLogout: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val extended = ParkeoTheme.colors
    var showLogoutDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Apariencia (persistida en DataStore, compartida con ParkeoApp)
    val context = LocalContext.current.applicationContext as ParkeoApplication
    val themeMode by context.container.sessionDataStore.themeMode.collectAsState(initial = "system")

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
            // ─── Tarjeta de identidad ──────────────────────────────────────────
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

            // ─── Acciones del rol ──────────────────────────────────────────────
            when {
                isAdmin -> {
                    ProfileSectionTitle("GESTIÓN DE LA PLATAFORMA")

                    ProfileOptionTile(
                        icon = Icons.Filled.Analytics,
                        title = "Dashboard de métricas",
                        subtitle = "Ocupación de la red y reservas Top",
                        onClick = onNavigateToDashboard
                    )

                    ProfileOptionTile(
                        icon = Icons.Filled.ConfirmationNumber,
                        title = "Reservas de la red",
                        subtitle = "Todas las reservas del sistema por estado",
                        onClick = onNavigateToAdminReservations
                    )

                    ProfileOptionTile(
                        icon = Icons.Filled.Storefront,
                        title = "Estacionamientos",
                        subtitle = "Horarios, capacidad y operadores asignados",
                        onClick = onNavigateToParkingLots
                    )

                    ProfileOptionTile(
                        icon = Icons.Filled.People,
                        title = "Usuarios y roles",
                        subtitle = "Conductores, operadores y administradores",
                        onClick = onNavigateToUsers
                    )

                    ProfileOptionTile(
                        icon = Icons.Filled.Explore,
                        title = "Explorar la red",
                        subtitle = "Ver el mapa público de cocheras",
                        onClick = onNavigateToExplore
                    )
                }

                isOperator -> {
                    ProfileSectionTitle("OPERACIÓN DE COCHERA")

                    ProfileOptionTile(
                        icon = Icons.Filled.GridView,
                        title = "Matriz de espacios",
                        subtitle = "Libres, ocupados y reservados en tiempo real",
                        onClick = onNavigateToSpaces
                    )

                    ProfileOptionTile(
                        icon = Icons.Filled.ConfirmationNumber,
                        title = "Control de reservas",
                        subtitle = "Buscar por código PKO y registrar Check-In / Check-Out",
                        onClick = onNavigateToReservations
                    )

                    ProfileOptionTile(
                        icon = Icons.Filled.Explore,
                        title = "Explorar la red",
                        subtitle = "Consultar tarifas y disponibilidad de otras sedes",
                        onClick = onNavigateToExplore
                    )
                }

                else -> {
                    ProfileSectionTitle("MI CUENTA")

                    ProfileOptionTile(
                        icon = Icons.Filled.DirectionsCar,
                        title = "Mis vehículos",
                        subtitle = "Placas y tipos que reservas a tu nombre",
                        onClick = onNavigateToVehicles
                    )

                    ProfileOptionTile(
                        icon = Icons.Filled.BookmarkBorder,
                        title = "Mis reservas",
                        subtitle = "Próximas, en curso e historial con QR",
                        onClick = onNavigateToReservations
                    )

                    ProfileOptionTile(
                        icon = Icons.Filled.Explore,
                        title = "Explorar cocheras",
                        subtitle = "Encuentra estacionamientos cerca de ti",
                        onClick = onNavigateToExplore
                    )
                }
            }

            // ─── Apariencia (todos los roles) ──────────────────────────────────
            ProfileSectionTitle("APARIENCIA")

            ParkeoCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(Dimens.spacingLg),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = null,
                            tint = extended.accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Tema de la app",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = extended.textPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                    ) {
                        ThemeOptionPill(
                            icon = Icons.Outlined.BrightnessMedium,
                            label = "Sistema",
                            selected = themeMode == "system",
                            modifier = Modifier.weight(1f)
                        ) { scope.launch { context.container.sessionDataStore.setThemeMode("system") } }

                        ThemeOptionPill(
                            icon = Icons.Outlined.LightMode,
                            label = "Claro",
                            selected = themeMode == "light",
                            modifier = Modifier.weight(1f)
                        ) { scope.launch { context.container.sessionDataStore.setThemeMode("light") } }

                        ThemeOptionPill(
                            icon = Icons.Outlined.DarkMode,
                            label = "Oscuro",
                            selected = themeMode == "dark",
                            modifier = Modifier.weight(1f)
                        ) { scope.launch { context.container.sessionDataStore.setThemeMode("dark") } }
                    }
                }
            }

            // ─── Acerca de ─────────────────────────────────────────────────────
            ParkeoCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.spacingLg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = extended.textTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Parkeo v1.0",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = extended.textPrimary
                        )
                        Text(
                            text = "Reservas de cochera con QR y recordatorios",
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.textSecondary
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ─── Cerrar sesión ─────────────────────────────────────────────────
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
private fun ProfileSectionTitle(text: String) {
    val extended = ParkeoTheme.colors
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = extended.textTertiary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
private fun ThemeOptionPill(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val extended = ParkeoTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) extended.accent else extended.surface2,
        border = androidx.compose.foundation.BorderStroke(
            Dimens.borderHairline,
            if (selected) extended.accent else extended.borderSubtle
        )
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) extended.onAccent else extended.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) extended.onAccent else extended.textPrimary,
                maxLines = 1
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
