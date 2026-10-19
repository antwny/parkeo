/*
 * PARKeo · Android
 * Archivo: `android/app/src/main/java/pe/parkeo/ui/screens/operator/OperatorSpacesScreen.kt`
 * Capa: presentación (Jetpack Compose). Rol: OPERADOR.
 *
 * Propósito
 * ---------
 * Esta pantalla muestra el mapa operativo de espacios del estacionamiento
 * asignado al operador. Permite consultar la ocupación, filtrar por estado y
 * cambiar manualmente el estado de una plaza. Al igual que la pantalla de
 * reservas, no accede a la red ni a la base de datos: observa el `StateFlow`
 * de `OperatorViewModel` y delega cada mutación en sus métodos.
 *
 * Conceptos clave
 * ---------------
 * - Rol OPERADOR: cuenta de trabajo encargada de la operación diaria de una
 *   sede. No administra la plataforma completa ni las cuentas de clientes.
 * - Estacionamiento asignado: el backend debe limitar la consulta a las sedes
 *   vinculadas al operador mediante `operator_parking_assignments`. La sede
 *   elegida (`selectedLot`) determina qué espacios se muestran.
 * - Estados de espacio:
 *   - `AVAILABLE`: libre; se pinta con `signalGreen` y la etiqueta "Libre".
 *   - `OCCUPIED`: un vehículo está físicamente en la plaza; `signalRed`,
 *     "Ocupado".
 *   - `RESERVED`: plaza apartada para una reserva; `signalAmber`, "Reserva".
 *   - `MAINTENANCE`: bloqueada por reparaciones o limpieza; gris
 *     (`textTertiary`), "Mtto.".
 *   Cualquier valor desconocido cae en la rama gris y usa el badge `Closed`.
 * - Ocupación: no es un campo separado. Se deriva de los estados de los
 *   espacios. Los contadores de la cabecera resumen cuántos están disponibles,
 *   ocupados y reservados; el mantenimiento se puede filtrar, pero no tiene
 *   tarjeta de resumen propia.
 * - Jetpack Compose: la cuadrícula y los contadores se recomponen cuando cambia
 *   `uiState.spaces`; no se manipulan vistas imperativamente.
 * - StateFlow: `OperatorViewModel.uiState` publica el estado de presentación y
 *   la pantalla lo recoge con `collectAsState()`.
 * - `LazyVerticalGrid`: cuadrícula perezosa con columnas adaptativas de mínimo
 *   100 dp; en pantallas angostas produce dos columnas y en tablets más.
 * - Diálogo: el cambio de estado se elige en un `AlertDialog`. Cada opción
 *   ejecuta la mutación de inmediato; el diálogo no tiene botón de guardar.
 * - Snackbar: informa el resultado de la actualización o el error devuelto por
 *   el ViewModel/repositorio.
 *
 * Flujo de usuario y datos
 * ------------------------
 * 1. Al entrar, `LaunchedEffect(Unit)` llama a `loadMyParkingLots()`. El
 *    ViewModel obtiene las sedes asignadas, conserva la selección previa si
 *    sigue existiendo (o elige la primera) y carga sus espacios y reservas.
 * 2. Si hay más de una sede, aparece un selector horizontal. Al tocar una sede
 *    se actualiza `selectedLot` y se recargan los espacios.
 * 3. La cabecera resume disponibles, ocupados y reservados. Los chips permiten
 *    filtrar por todos, libres, ocupados, reservados o mantenimiento. El
 *    filtrado es local; no genera una petición nueva.
 * 4. La cuadrícula muestra cada espacio con su número, un icono según el tipo
 *    de vehículo (moto, camión o auto) y un badge de estado.
 * 5. Al tocar una tarjeta se abre el diálogo de cambio de estado. Al elegir
 *    una opción se invoca `updateSpaceStatus(spaceId, newStatus)`, se cierra el
 *    diálogo y el ViewModel reemplaza el espacio devuelto por el backend.
 * 6. Si la actualización falla, el Snackbar muestra el error y el estado local
 *    no se modifica. Si tiene éxito, se muestra un mensaje y los contadores se
 *    recalculan automáticamente.
 *
 * Relación con ViewModel, navegación, backend y seguridad
 * -------------------------------------------------------
 * - `OperatorViewModel` es la fuente de verdad para sedes y espacios; la
 *   pantalla solo conserva estado efímero (sede ya seleccionada en el VM,
 *   filtro activo y espacio cuyo diálogo está abierto).
 * - El parámetro `onNavigateToReservations` está declarado para conectar esta
 *   vista con la pantalla de reservas, pero la implementación actual no lo
 *   invoca. Es un punto de integración pendiente, no un botón existente.
 * - El icono de refresco llama a `loadMyParkingLots(refresh = true)` y vuelve a
 *   consultar la asignación y los espacios. No hay actualización en tiempo real
 *   por WebSocket; la frescura depende de refrescar o volver a entrar.
 * - El backend debe validar que el operador autenticado tenga asignada la sede,
 *   que el espacio pertenezca a esa sede y que el cambio de estado sea legal.
 *   Ocultar opciones o filtrar tarjetas en Compose no protege el endpoint: un
 *   cliente modificado puede enviar la petición directamente. Sin esa
 *   validación, un operador podría alterar espacios de otra sede o marcar como
 *   libre una plaza ocupada. La autorización horizontal y las transiciones
 *   coherentes con reservas y check-in/check-out corresponden al servidor.
 * - Marcar manualmente `OCCUPIED` o `RESERVED` no crea ni asocia una reserva:
 *   solo cambia la condición operativa del espacio. La relación con clientes y
 *   horarios se gestiona en el flujo de reservas, y el backend debe evitar
 *   inconsistencias entre ambos conceptos.
 *
 * Guía de estudio: `docs/GUIA_MAESTRA_ESTUDIANTE.md`.
 */
package pe.parkeo.ui.screens.operator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import pe.parkeo.data.remote.dto.ParkingSpaceDto
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.OperatorViewModel

@Composable
/**
 * Pantalla de control de espacios del estacionamiento asignado al operador.
 *
 * Presenta un resumen de ocupación, filtros por estado y una cuadrícula de
 * plazas. Al tocar una plaza se abre un diálogo para cambiarla de estado. Toda
 * lectura y escritura pasa por `OperatorViewModel`; la pantalla no conoce
 * Retrofit ni el contrato HTTP.
 *
 * @param viewModel ViewModel de operador que expone `uiState`, carga las sedes
 *   asignadas y espacios, y ejecuta `updateSpaceStatus`.
 * @param onNavigateToReservations Callback opcional previsto para navegar a la
 *   pantalla de reservas. Actualmente la implementación no lo invoca; se
 *   conserva como punto de integración de navegación.
 */
fun OperatorSpacesScreen(
    viewModel: OperatorViewModel,
    onNavigateToReservations: (() -> Unit)? = null
) {
    // Estado de presentación observado desde el ViewModel. Cada emisión
    // actualiza sedes, espacios, indicadores de carga y mensajes.
    val uiState by viewModel.uiState.collectAsState()
    // Paleta extendida del tema PARKeo para estados y superficies.
    val extended = ParkeoTheme.colors
    // Cola de Snackbars para éxito y error.
    val snackbarHostState = remember { SnackbarHostState() }

    // Carga inicial de sedes asignadas. El ViewModel elige la sede seleccionada
    // (conserva la anterior si sigue disponible) y luego solicita sus espacios.
    // `Unit` como clave garantiza una sola ejecución por entrada a composición.
    LaunchedEffect(Unit) {
        viewModel.loadMyParkingLots()
    }

    // Espacio cuyo diálogo de cambio de estado está abierto. `null` significa
    // que no hay diálogo visible.
    var spaceToEdit by remember { mutableStateOf<ParkingSpaceDto?>(null) }
    // Filtro local de la cuadrícula: "TODOS" o uno de los estados del espacio.
    var selectedFilterStatus by remember { mutableStateOf("TODOS") }

    // Filtrado en memoria por estado y por texto. La pantalla no muestra un
    // campo de búsqueda propio, pero respeta `uiState.searchQuery` si otra
    // parte del flujo lo establece; además compara número de espacio y tipo de
    // vehículo. No se vuelve a consultar el backend al filtrar.
    val filteredSpaces = remember(uiState.spaces, selectedFilterStatus, uiState.searchQuery) {
        uiState.spaces.filter { space ->
            val matchesFilter = when (selectedFilterStatus) {
                "AVAILABLE" -> space.status == "AVAILABLE"
                "OCCUPIED" -> space.status == "OCCUPIED"
                "RESERVED" -> space.status == "RESERVED"
                "MAINTENANCE" -> space.status == "MAINTENANCE"
                else -> true
            }
            val matchesSearch = uiState.searchQuery.isBlank() ||
                    space.spaceNumber.contains(uiState.searchQuery, ignoreCase = true) ||
                    space.vehicleType.contains(uiState.searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    // Traduce mensajes del ViewModel a Snackbar y limpia el estado después de
    // mostrarlos para evitar repeticiones durante la recomposición.
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

    // Contenedor principal de Material 3. El Snackbar se aloja aquí para
    // sobrevivir a los cambios de la lista y de los filtros.
    Scaffold(
        containerColor = extended.background,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Encabezado: título, nombre de la sede activa y refresco manual.
            ParkeoTopBar(
                title = "Control de Espacios",
                subtitle = uiState.selectedLot?.name,
                actions = {
                    // Refresco completo: vuelve a pedir las sedes asignadas y,
                    // con ellas, los espacios de la sede seleccionada. Sirve
                    // para reflejar cambios hechos por check-in/check-out u
                    // otros operadores; no hay sondeo automático.
                    IconButton(onClick = { viewModel.loadMyParkingLots(refresh = true) }) {
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
        // Cuerpo vertical: selector de sede, resumen, filtros y cuadrícula.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Selector de sede: se muestra solo cuando el operador tiene más de
            // una cochera asignada. Cada chip cambia `selectedLot` y dispara la
            // carga de espacios correspondiente.
            if (uiState.parkingLots.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.spacingMd, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.parkingLots, key = { it.id }) { lot ->
                        val isSelected = uiState.selectedLot?.id == lot.id
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewModel.selectParkingLot(lot) },
                            color = if (isSelected) extended.accent else extended.surface2,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                Dimens.borderHairline,
                                if (isSelected) extended.accent else extended.borderSubtle
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Storefront,
                                    contentDescription = null,
                                    tint = if (isSelected) extended.onAccent else extended.accent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = lot.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) extended.onAccent else extended.textPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Resumen rápido de ocupación. Los conteos se derivan de la lista
            // completa de espacios, no de `filteredSpaces`, para que los chips
            // de filtro no alteren las cifras globales de la sede.
            val available = uiState.spaces.count { it.status == "AVAILABLE" }
            val occupied = uiState.spaces.count { it.status == "OCCUPIED" }
            val reserved = uiState.spaces.count { it.status == "RESERVED" }

            // Tres tarjetas: disponibles, ocupados y reservados. El estado
            // `MAINTENANCE` no se resume aquí, pero sí puede filtrarse abajo.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingMd, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusMiniCard(
                    modifier = Modifier.weight(1f),
                    label = "Disponibles",
                    count = available,
                    color = extended.signalGreen
                )
                StatusMiniCard(
                    modifier = Modifier.weight(1f),
                    label = "Ocupados",
                    count = occupied,
                    color = extended.signalRed
                )
                StatusMiniCard(
                    modifier = Modifier.weight(1f),
                    label = "Reservados",
                    count = reserved,
                    color = extended.signalAmber
                )
            }

            // Filtros por estado: "TODOS", libres, ocupados, reservados y
            // mantenimiento. Son estado local de UI y no modifican el backend;
            // solo determinan qué subconjunto se pinta en la cuadrícula.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.spacingMd, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "TODOS" to "Todos",
                    "AVAILABLE" to "Libres",
                    "OCCUPIED" to "Ocupados",
                    "RESERVED" to "Reservados",
                    "MAINTENANCE" to "Mtto."
                ).forEach { (key, label) ->
                    val isSelected = selectedFilterStatus == key
                    Surface(
                        onClick = { selectedFilterStatus = key },
                        color = if (isSelected) extended.accent else extended.surface2,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            Dimens.borderHairline,
                            if (isSelected) extended.accent else extended.borderSubtle
                        )
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) extended.onAccent else extended.textPrimary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Estados de contenido: carga inicial, resultado vacío o datos.
            // Un error de red se comunica por Snackbar; no reemplaza la lista.
            when {
                uiState.isLoading && uiState.spaces.isEmpty() -> {
                    // Solo se bloquea la vista si aún no hay espacios. Durante
                    // un refresco con datos, la cuadrícula permanece visible.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        ParkeoLoadingView(message = "Cargando espacios...")
                    }
                }

                filteredSpaces.isEmpty() -> {
                    // Vacío por filtro o porque la sede no tiene espacios. El
                    // mensaje no distingue ambos casos en esta versión.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        ParkeoEmptyState(
                            title = "Sin espacios",
                            subtitle = "No hay espacios con el filtro seleccionado",
                            icon = Icons.Filled.GridView
                        )
                    }
                }

                else -> {
                    // Cuadrícula perezosa: columnas adaptativas según el ancho
                    // disponible (mínimo 100 dp por celda) y contenido visible
                    // únicamente. La clave por `id` conserva posiciones y
                    // evita recrear celdas cuando cambia el estado de una plaza.
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 100.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(horizontal = Dimens.spacingMd),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        items(filteredSpaces, key = { it.id }) { space ->
                            // Al tocar, no se muta nada: solo se abre el
                            // diálogo con el espacio seleccionado.
                            ParkingSpaceTile(
                                space = space,
                                onClick = { spaceToEdit = space }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de cambio de estado. La mutación se envía al ViewModel al elegir
    // una opción y el diálogo se cierra de inmediato: no hay botón "Guardar"
    // adicional. Si el backend rechaza el cambio, el error llega por Snackbar
    // y el estado del espacio no se modifica en la lista.
    spaceToEdit?.let { space ->
        ChangeSpaceStatusDialog(
            space = space,
            onDismiss = { spaceToEdit = null },
            onStatusSelected = { newStatus ->
                viewModel.updateSpaceStatus(space.id, newStatus)
                spaceToEdit = null
            }
        )
    }
}

@Composable
/**
 * Tarjeta compacta de resumen para un estado de espacio.
 *
 * Muestra un número grande y una etiqueta breve. Es puramente presentacional:
 * recibe los valores ya calculados por la pantalla y un color de acento.
 *
 * @param modifier Modificador externo; la pantalla lo usa con `weight(1f)`
 *   para repartir el ancho de la fila.
 * @param label Texto corto bajo el número, por ejemplo "Disponibles".
 * @param count Cantidad de espacios en ese estado.
 * @param color Color de señal asociado al estado (verde, rojo o ámbar).
 */
private fun StatusMiniCard(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    color: Color
) {
    val extended = ParkeoTheme.colors
    // Tarjeta del sistema visual compartido; interior centrado.
    ParkeoCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = Typography.MonospaceTechnical,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = extended.textSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
/**
 * Celda de la cuadrícula que representa un espacio de estacionamiento.
 *
 * Muestra el número de plaza, un icono según el tipo de vehículo admitido y un
 * badge con el estado. La celda es clicable para abrir el diálogo de cambio de
 * estado; no muta datos por sí sola.
 *
 * @param space DTO del espacio. `spaceNumber` y `vehicleType` vienen del
 *   contrato remoto y pueden estar vacíos si el backend no los envía.
 * @param onClick Callback que solicita abrir el diálogo de cambio de estado.
 */
private fun ParkingSpaceTile(
    space: ParkingSpaceDto,
    onClick: () -> Unit
) {
    val extended = ParkeoTheme.colors
    // Leyenda visual del estado: color de acento y variante del badge
    // compartido. `MAINTENANCE` y valores desconocidos comparten la rama gris
    // con el badge `Closed`.
    val (statusColor, badgeStatus) = when (space.status) {
        "AVAILABLE" -> Pair(extended.signalGreen, ParkeoBadgeStatus.Available)
        "OCCUPIED" -> Pair(extended.signalRed, ParkeoBadgeStatus.Occupied)
        "RESERVED" -> Pair(extended.signalAmber, ParkeoBadgeStatus.Reserved)
        else -> Pair(extended.textTertiary, ParkeoBadgeStatus.Closed)
    }

    // Tarjeta clicable. El borde de acento solo se aplica a espacios libres,
    // para que las plazas utilizables resalten a simple vista.
    ParkeoCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp),
        accentBorder = space.status == "AVAILABLE"
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Número o código visible de la plaza.
            Text(
                text = space.spaceNumber,
                style = Typography.MonospaceTechnical,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = extended.textPrimary
            )

            // Icono del tipo de vehículo. La comparación por subcadena y sin
            // distinguir mayúsculas tolera variantes del catálogo del backend
            // ("MOTO", "Motos", "CAMION", etc.). Un tipo ausente o extraño se
            // representa como auto.
            Icon(
                imageVector = when {
                    space.vehicleType.contains("MOTO", ignoreCase = true) -> Icons.Filled.TwoWheeler
                    space.vehicleType.contains("CAMION", ignoreCase = true) -> Icons.Filled.LocalShipping
                    else -> Icons.Filled.DirectionsCar
                },
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(24.dp)
            )

            // Badge de estado con etiqueta corta en español.
            ParkeoBadge(
                status = badgeStatus,
                labelOverride = when (space.status) {
                    "AVAILABLE" -> "Libre"
                    "OCCUPIED" -> "Ocupado"
                    "RESERVED" -> "Reserva"
                    else -> "Mtto."
                }
            )
        }
    }
}

@Composable
/**
 * Diálogo para elegir el nuevo estado de un espacio.
 *
 * Lista las cuatro opciones posibles con su color, descripción y un indicador
 * de selección cuando coinciden con el estado actual. Cada opción es una acción
 * inmediata: al pulsarla se invoca `onStatusSelected`, la pantalla envía la
 * mutación al ViewModel y cierra el diálogo. No existe botón de confirmación
 * aparte; el botón visible solo cancela.
 *
 * @param space Espacio cuyo estado se va a cambiar. Se usa para el título y
 *   para marcar visualmente la opción vigente.
 * @param onDismiss Callback para cerrar el diálogo sin cambiar nada.
 * @param onStatusSelected Callback con el nuevo estado elegido como cadena del
 *   contrato (`"AVAILABLE"`, `"OCCUPIED"`, `"RESERVED"` o `"MAINTENANCE"`).
 */
private fun ChangeSpaceStatusDialog(
    space: ParkingSpaceDto,
    onDismiss: () -> Unit,
    onStatusSelected: (String) -> Unit
) {
    val extended = ParkeoTheme.colors

    // Diálogo Material 3 con superficie propia del tema extendido.
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extended.surface1,
        title = {
            // Título con el número de plaza en tipografía monoespaciada.
            Text(
                text = "Espacio ${space.spaceNumber}",
                style = Typography.MonospaceTechnical,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = extended.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Selecciona el nuevo estado del espacio:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.textSecondary
                )
                Spacer(Modifier.height(4.dp))

                // Opciones de estado. `isSelected` solo resalta el estado
                // actual; sigue siendo posible pulsarlo, pero normalmente no
                // produce un cambio visible. La validación de transiciones
                // permitidas corresponde al backend.
                StatusOptionButton(
                    title = "Disponible (Libre)",
                    subtitle = "Listo para estacionar o reservar",
                    color = extended.signalGreen,
                    icon = Icons.Filled.CheckCircle,
                    isSelected = space.status == "AVAILABLE",
                    onClick = { onStatusSelected("AVAILABLE") }
                )

                // Marca operativa de vehículo estacionado; no crea una reserva.
                StatusOptionButton(
                    title = "Ocupado",
                    subtitle = "Vehículo físicamente estacionado",
                    color = extended.signalRed,
                    icon = Icons.Filled.DirectionsCar,
                    isSelected = space.status == "OCCUPIED",
                    onClick = { onStatusSelected("OCCUPIED") }
                )

                // Apartado manual para un cliente; el vínculo con la reserva se
                // gestiona en el backend, no en esta pantalla.
                StatusOptionButton(
                    title = "Reservado",
                    subtitle = "Separado para un cliente",
                    color = extended.signalAmber,
                    icon = Icons.Filled.Bookmark,
                    isSelected = space.status == "RESERVED",
                    onClick = { onStatusSelected("RESERVED") }
                )

                // Bloqueo por reparación o limpieza; evita nuevas reservas o
                // ingresos sobre esa plaza.
                StatusOptionButton(
                    title = "Mantenimiento",
                    subtitle = "Bloqueado por reparaciones",
                    color = extended.textTertiary,
                    icon = Icons.Filled.Build,
                    isSelected = space.status == "MAINTENANCE",
                    onClick = { onStatusSelected("MAINTENANCE") }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = extended.textSecondary)
            }
        }
    )
}

@Composable
/**
 * Opción seleccionable del diálogo de cambio de estado.
 *
 * Representa un estado con su color, icono, título y descripción. Cuando es el
 * estado vigente, se resalta el fondo, el borde, el texto y aparece una marca
 * de verificación.
 *
 * @param title Nombre visible del estado, por ejemplo "Disponible (Libre)".
 * @param subtitle Explicación breve de cuándo corresponde ese estado.
 * @param color Color de señal asociado al estado.
 * @param icon Icono representativo del estado.
 * @param isSelected `true` si coincide con el estado actual del espacio.
 * @param onClick Callback que notifica la elección de esta opción.
 */
private fun StatusOptionButton(
    title: String,
    subtitle: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val extended = ParkeoTheme.colors
    // Superficie clicable que cambia de color cuando está seleccionada.
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) color.copy(alpha = 0.12f) else extended.surface2,
        border = androidx.compose.foundation.BorderStroke(
            Dimens.borderHairline,
            if (isSelected) color else extended.borderSubtle
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icono del estado a la izquierda del texto.
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            // Título y explicación; ocupan el espacio central de la fila.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) color else extended.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textSecondary
                )
            }
            if (isSelected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
        }
    }
}
