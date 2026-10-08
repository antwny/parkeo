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

@Composable
fun AdminParkingLotsScreen(
    viewModel: AdminViewModel,
    onNavigateBack: (() -> Unit)? = null,
    onSelectLotDetail: ((Long) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    var lotToAssignOperator by remember { mutableStateOf<ParkingLotDto?>(null) }

    val filteredLots = remember(uiState.parkingLots, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.parkingLots
        } else {
            uiState.parkingLots.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.address.contains(searchQuery, ignoreCase = true) ||
                        (it.district ?: "").contains(searchQuery, ignoreCase = true)
            }
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
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
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
            // Search field
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

            when {
                uiState.isLoading && uiState.parkingLots.isEmpty() -> {
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
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(Dimens.spacingMd),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                    ) {
                        item {
                            Text(
                                text = "${filteredLots.size} ESTACIONAMIENTOS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = extended.textTertiary
                            )
                        }

                        items(filteredLots, key = { it.id }) { lot ->
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

        lotToAssignOperator?.let { lot ->
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
                    Text(
                        text = "${lot.address}${lot.district?.let { ", $it" } ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textSecondary
                    )
                }

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

            // Capacity details in Monospace
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

            // Assigned Operator Status with interactive assignment button
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

            // Switch controls
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

@Composable
private fun AssignOperatorDialog(
    lot: ParkingLotDto,
    operators: List<UserDto>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val extended = ParkeoTheme.colors
    var selectedOperatorId by remember {
        mutableStateOf(lot.operatorId ?: operators.firstOrNull()?.id ?: 0L)
    }

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
                Text(
                    text = "Selecciona el operador encargado de gestionar las entradas y salidas de \"${lot.name}\":",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textSecondary
                )

                if (operators.isEmpty()) {
                    Text(
                        text = "No hay operadores registrados en el sistema.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.signalAmber
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(operators, key = { it.id }) { operator ->
                            val isSelected = operator.id == selectedOperatorId
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
            ParkeoButton(
                text = "Cancelar",
                onClick = onDismiss,
                style = ParkeoButtonStyle.Ghost
            )
        }
    )
}

