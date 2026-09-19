package pe.parkeo.ui.screens.admin

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.data.remote.dto.UserDto
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(
    viewModel: AdminViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Usuarios del Sistema", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Regresar")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboardData(refresh = true) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar por nombre, email o teléfono...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Role filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("TODOS", "ADMIN", "OPERADOR", "CLIENTE").forEach { role ->
                    FilterChip(
                        selected = selectedRoleFilter == role,
                        onClick = { selectedRoleFilter = role },
                        label = { Text(role, fontSize = 12.sp) }
                    )
                }
            }

            if (uiState.isLoading && uiState.users.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.PeopleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No se encontraron usuarios",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "${filteredUsers.size} usuarios encontrados",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
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

                    item {
                        Spacer(Modifier.height(16.dp))
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
    val roleUpper = user.role.uppercase()
    val isAdmin = roleUpper.contains("ADMIN")
    val isOperator = roleUpper.contains("OPERAT") || roleUpper.contains("OPERADOR")

    val roleBg = when {
        isAdmin -> Color(0xFF7C3AED).copy(alpha = 0.15f)
        isOperator -> ParkeoAmber500.copy(alpha = 0.15f)
        else -> ParkeoCyan500.copy(alpha = 0.15f)
    }

    val roleColor = when {
        isAdmin -> Color(0xFF7C3AED)
        isOperator -> Color(0xFFD97706)
        else -> ParkeoCyan500
    }

    val roleLabel = when {
        isAdmin -> "ADMIN"
        isOperator -> "OPERADOR"
        else -> "CLIENTE"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(roleBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isAdmin -> Icons.Filled.AdminPanelSettings
                        isOperator -> Icons.Filled.Badge
                        else -> Icons.Filled.Person
                    },
                    contentDescription = null,
                    tint = roleColor,
                    modifier = Modifier.size(24.dp)
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
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = roleBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = roleLabel,
                            color = roleColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!user.phone.isNullOrBlank()) {
                    Text(
                        text = "Tel: ${user.phone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
                    modifier = Modifier.height(28.dp)
                )
                Text(
                    text = if (user.isActive) "Activo" else "Inactivo",
                    fontSize = 10.sp,
                    color = if (user.isActive) ParkeoGreen500 else ParkeoRed500,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
