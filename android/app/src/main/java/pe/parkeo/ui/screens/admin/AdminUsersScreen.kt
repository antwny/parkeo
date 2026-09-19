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

@Composable
fun AdminUsersScreen(
    viewModel: AdminViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("TODOS") }
    val snackbarHostState = remember { SnackbarHostState() }

    val filteredUsers = remember(uiState.users, searchQuery, selectedRoleFilter) {
        uiState.users.filter { user ->
            val matchesQuery = searchQuery.isBlank() ||
                    user.firstName.contains(searchQuery, ignoreCase = true) ||
                    user.lastName.contains(searchQuery, ignoreCase = true) ||
                    user.email.contains(searchQuery, ignoreCase = true) ||
                    (user.phone ?: "").contains(searchQuery)

            val matchesRole = when (selectedRoleFilter) {
                "ADMIN" -> user.role.contains("ADMIN", ignoreCase = true)
                "OPERADOR" -> user.role.contains("OPERAT", ignoreCase = true) || user.role.contains("OPERADOR", ignoreCase = true)
                "CLIENTE" -> user.role.contains("CLIENTE", ignoreCase = true) || user.role.contains("USER", ignoreCase = true)
                else -> true
            }

            matchesQuery && matchesRole
        }
    }

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

    Scaffold(
        containerColor = extended.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
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
            // Search input
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

            // Role filter chips
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

            when {
                uiState.isLoading && uiState.users.isEmpty() -> {
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
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(Dimens.spacingMd),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                    ) {
                        item {
                            Text(
                                text = "${filteredUsers.size} USUARIOS ENCONTRADOS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = extended.textTertiary
                            )
                        }

                        items(filteredUsers, key = { it.id }) { user ->
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

@Composable
private fun AdminUserCard(
    user: UserDto,
    onToggleStatus: () -> Unit
) {
    val extended = ParkeoTheme.colors
    val roleUpper = user.role.uppercase()
    val isAdmin = roleUpper.contains("ADMIN")
    val isOperator = roleUpper.contains("OPERAT") || roleUpper.contains("OPERADOR")

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
            // Avatar Vessel
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

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                if (!user.phone.isNullOrBlank()) {
                    Text(
                        text = "Tel: ${user.phone}",
                        style = Typography.MonospaceTechnical,
                        fontSize = 11.sp,
                        color = extended.textTertiary
                    )
                }
            }

            // Toggle active status
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
