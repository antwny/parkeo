/*
 * PARKeo · Android
 * Archivo: `android/app/src/main/java/pe/parkeo/ui/screens/operator/OperatorReservationsScreen.kt`
 * Capa: presentación (Jetpack Compose). Rol: OPERADOR.
 *
 * Propósito
 * ---------
 * Esta pantalla es el tablero operativo con el que una persona con rol OPERADOR
 * controla el ingreso y la salida de vehículos de un estacionamiento concreto.
 * No crea reservas ni administra usuarios: observa el estado que publica
 * `OperatorViewModel`, permite filtrar y buscar reservas, y delega en el
 * ViewModel las acciones de check-in y check-out. La pantalla no habla
 * directamente con Retrofit, con MySQL ni con el backend; solo invoca métodos
 * del ViewModel, que a su vez usan `OperatorRepository` y `OperatorApi`.
 *
 * Conceptos clave
 * ---------------
 * - Rol OPERADOR: cuenta de trabajo que gestiona la operación diaria de una o
 *   varias sedes. No debe confundirse con un administrador global ni con un
 *   cliente que reserva desde la app.
 * - Estacionamiento asignado: el backend solo debería devolver al operador las
 *   sedes vinculadas mediante `operator_parking_assignments`. `selectedLot`
 *   determina qué espacios y reservas se cargan en esta pantalla.
 * - Reservas vigentes y reservas activas: una reserva `PENDING` (pendiente) o
 *   `CONFIRMED` (confirmada) todavía no ha ingresado; una reserva `ACTIVE`
 *   (activa) ya está dentro de la cochera. Los estados terminales son
 *   `COMPLETED`, `CANCELLED` y `NO_SHOW`.
 * - Check-in: registra la entrada física del vehículo. En el flujo esperado la
 *   reserva pasa a `ACTIVE` y el espacio asociado pasa a `OCCUPIED`.
 * - Check-out: registra la salida física. En el flujo esperado la reserva pasa a
 *   `COMPLETED` y el espacio se libera a `AVAILABLE`. El backend es la
 *   autoridad final sobre estas transiciones; los textos de la interfaz
 *   describen la intención, no sustituyen la validación del servidor.
 * - Estados de espacio: `AVAILABLE` (libre), `OCCUPIED` (ocupado),
 *   `RESERVED` (apartado para una reserva) y `MAINTENANCE` (bloqueado por
 *   mantenimiento). Son una dimensión distinta del estado de la reserva.
 * - Jetpack Compose: la interfaz se describe como una función del estado. Al
 *   cambiar `uiState`, Compose recompone las secciones que leen ese estado.
 * - StateFlow: `OperatorViewModel.uiState` expone un flujo de solo lectura con
 *   el último valor disponible; aquí se observa con `collectAsState()`.
 * - `LazyColumn`: lista eficiente que solo compone los elementos visibles.
 * - Diálogos: `AlertDialog` se usa para confirmar una acción destructiva o
 *   relevante antes de enviarla al servidor.
 * - Snackbar: canal breve para confirmar éxitos y mostrar errores sin abandonar
 *   la pantalla. Los mensajes provienen de `successMessage` y `error` del
 *   ViewModel, y se limpian después de mostrarse para no repetirlos.
 *
 * Flujo de usuario y datos
 * ------------------------
 * 1. El usuario entra a la pantalla y observa `uiState`. Si `selectedLot` tiene
 *    un identificador, un `LaunchedEffect` solicita espacios y reservas de esa
 *    sede con `loadSpacesAndReservations(lotId)`.
 * 2. Si el operador tiene más de una sede asignada, aparece un selector de
 *    sedes. Al elegir una, `selectParkingLot` actualiza `selectedLot` y vuelve a
 *    cargar los datos.
 * 3. El buscador filtra localmente por placa, código de confirmación, número de
 *    espacio o nombre del estacionamiento. Las pestañas segmentan por estado:
 *    por ingresar, en cochera, finalizadas y todas. Los contadores se calculan
 *    a partir de la lista recibida, no se piden al backend.
 * 4. Cada tarjeta muestra placa, espacio, código, horario, monto y estado. El
 *    contrato `ReservationDto` no incluye el nombre del cliente; el operador
 *    identifica la reserva por la placa y el código de confirmación.
 * 5. Solo `PENDING` y `CONFIRMED` ofrecen "Registrar Ingreso"; solo `ACTIVE`
 *    ofrece "Registrar Salida". Las acciones abren un diálogo de confirmación.
 *    Al confirmar se invoca `viewModel.checkIn(id)` o `viewModel.checkOut(id)`.
 * 6. El ViewModel actualiza la reserva y recarga espacios para reflejar la
 *    ocupación. Los éxitos y errores llegan como Snackbar. El icono de refresco
 *    de la barra superior vuelve a consultar la sede seleccionada.
 *
 * Relación con ViewModel, navegación y backend
 * --------------------------------------------
 * - `OperatorViewModel` conserva el estado de presentación (`OperatorUiState`)
 *   y coordina las llamadas al repositorio. La pantalla no guarda la lista de
 *   reservas: solo conserva estado efímero de UI (pestaña, búsqueda y acción
 *   pendiente de confirmar).
 * - La navegación se eleva mediante el callback `onNavigateBack`; esta pantalla
 *   no conoce el grafo de navegación ni crea intents directamente.
 * - El backend recibe las operaciones a través de `OperatorApi` y debe validar
 *   que el operador autenticado tenga asignada la sede, que la reserva
 *   pertenezca a esa sede y que la transición de estado sea legal. Ocultar un
 *   botón en Compose mejora la experiencia, pero no es un control de seguridad:
 *   un cliente modificado podría llamar al endpoint igualmente. Por eso la
 *   autorización horizontal y las reglas de negocio viven en el servidor.
 *
 * Guía de estudio: `docs/GUIA_MAESTRA_ESTUDIANTE.md`.
 */
package pe.parkeo.ui.screens.operator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import pe.parkeo.data.remote.dto.ReservationDto
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.OperatorViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
/**
 * Pantalla principal del operador para controlar reservas y registrar
 * check-in/check-out.
 *
 * La función observa el `StateFlow` del ViewModel, construye un `Scaffold` con
 * barra superior, selector de sede, buscador, pestañas por estado y una lista
 * perezosa de reservas. Toda mutación se delega al ViewModel; la pantalla solo
 * decide qué mostrar y qué acción pedir.
 *
 * @param viewModel ViewModel de operador que expone `uiState` y las acciones
 *   `loadSpacesAndReservations`, `selectParkingLot`, `checkIn`, `checkOut` y
 *   `clearMessages`. Es la única fuente de verdad para sedes, espacios y
 *   reservas.
 * @param onNavigateBack Callback opcional para volver a la pantalla anterior.
 *   Se entrega a `ParkeoTopBar`; si es `null`, la barra no muestra navegación
 *   de retorno.
 */
fun OperatorReservationsScreen(
    viewModel: OperatorViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    // Estado observable del ViewModel. `collectAsState` convierte el StateFlow
    // en un State de Compose: cada emisión provoca recomposición de las partes
    // que leen `uiState`.
    val uiState by viewModel.uiState.collectAsState()
    // Colores extendidos del tema PARKeo (superficies, acentos, señales).
    val extended = ParkeoTheme.colors
    // Host del Snackbar: cola de mensajes efímeros de éxito o error.
    val snackbarHostState = remember { SnackbarHostState() }
    // Vibración corta al confirmar una operación; refuerza la acción sin
    // depender solo del cambio visual.
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    // Pestaña activa: 0 = por ingresar, 1 = en cochera, 2 = finalizadas,
    // 3 = todas. Es estado local de UI y se pierde si se recrea la pantalla.
    var selectedTab by remember { mutableIntStateOf(0) }
    // Contadores derivados de la lista actual. Se memorizan por
    // `uiState.reservations` para no recorrerla en cada recomposición.
    val countPorIngresar = remember(uiState.reservations) {
        uiState.reservations.count { it.status in listOf("PENDING", "CONFIRMED") }
    }
    val countEnCochera = remember(uiState.reservations) {
        uiState.reservations.count { it.status == "ACTIVE" }
    }
    val countFinalizadas = remember(uiState.reservations) {
        uiState.reservations.count { it.status in listOf("COMPLETED", "CANCELLED", "NO_SHOW") }
    }
    val countTodas = remember(uiState.reservations) {
        uiState.reservations.size
    }
    // Etiquetas de las pestañas; el contador se actualiza cuando cambia la
    // lista de reservas del ViewModel.
    val tabLabels = listOf(
        "Por Ingresar ($countPorIngresar)",
        "En Cochera ($countEnCochera)",
        "Finalizadas ($countFinalizadas)",
        "Todas ($countTodas)"
    )
    // Búsqueda local de esta pantalla (no usa `uiState.searchQuery`, que el
    // ViewModel reserva para otros filtros). Se compara en minúsculas.
    var searchQuery by remember { mutableStateOf("") }

    // Acción pendiente de confirmación: la reserva y el tipo de operación
    // (`"CHECK_IN"` o `"CHECK_OUT"`). Mientras sea `null`, no hay diálogo.
    var reservationToConfirmAction by remember { mutableStateOf<Pair<ReservationDto, String>?>(null) }

    // Al cambiar la sede seleccionada se piden sus espacios y reservas. El
    // ViewModel también dispara esta carga al elegir sede; este efecto cubre
    // la llegada inicial y cualquier cambio de `selectedLot` desde otra fuente.
    LaunchedEffect(uiState.selectedLot) {
        uiState.selectedLot?.id?.let { lotId ->
            viewModel.loadSpacesAndReservations(lotId)
        }
    }

    // Puente entre mensajes del ViewModel y el Snackbar. Después de mostrar un
    // mensaje se limpia el estado para que no se repita en la siguiente
    // recomposición o recreación.
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

    // Filtrado en memoria: primero por pestaña de estado y luego por texto.
    // El backend ya entregó solo las reservas de la sede seleccionada; aquí no
    // se vuelve a consultar la red.
    val filteredReservations = remember(uiState.reservations, selectedTab, searchQuery) {
        uiState.reservations.filter { r ->
            val matchesTab = when (selectedTab) {
                0 -> r.status in listOf("PENDING", "CONFIRMED")
                1 -> r.status == "ACTIVE"
                2 -> r.status in listOf("COMPLETED", "CANCELLED", "NO_SHOW")
                else -> true
            }
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                    (r.vehicleLicensePlate ?: "").lowercase().contains(q) ||
                    (r.confirmationCode ?: "").lowercase().contains(q) ||
                    (r.spaceNumber ?: "").lowercase().contains(q) ||
                    (r.parkingLotName ?: "").lowercase().contains(q)

            matchesTab && matchesQuery
        }
    }

    // Contenedor Material 3 de la pantalla. Se usa `contentWindowInsets = 0`
    // porque la barra superior y el padding del sistema se gestionan aquí.
    Scaffold(
        containerColor = extended.background,
        contentWindowInsets = WindowInsets(0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Encabezado: título fijo, nombre de la sede seleccionada y acción
            // de actualización manual. No hay botón de menú adicional.
            ParkeoTopBar(
                title = "Control de Reservas",
                subtitle = uiState.selectedLot?.name ?: "Gestión de ingresos y salidas",
                onNavigationClick = onNavigateBack,
                actions = {
                    // Refresco explícito: vuelve a pedir espacios y reservas de
                    // la sede actual. No hay WebSocket ni sondeo automático;
                    // la frescura depende de esta acción o de volver a entrar.
                    IconButton(onClick = {
                        uiState.selectedLot?.id?.let { viewModel.loadSpacesAndReservations(it) }
                    }) {
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
        // Cuerpo vertical: selector de sede, buscador, pestañas y lista.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Selector de sede: solo se muestra si el operador tiene más de una
            // cochera asignada. Al tocar un chip se actualiza `selectedLot` y el
            // ViewModel recarga espacios y reservas.
            if (uiState.parkingLots.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.spacingMd, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.parkingLots) { lot ->
                        val isSelected = uiState.selectedLot?.id == lot.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectParkingLot(lot) },
                            label = { Text(lot.name, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = extended.accent,
                                selectedLabelColor = extended.onAccent
                            )
                        )
                    }
                }
            }

            // Buscador local: placa, código PKO, número de espacio o nombre de
            // la sede. El icono de limpiar aparece solo cuando hay texto.
            Box(modifier = Modifier.padding(horizontal = Dimens.spacingMd, vertical = 6.dp)) {
                ParkeoTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Buscar placa, código PKO o espacio...",
                    leadingIcon = Icons.Filled.Search,
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Limpiar",
                                    tint = extended.textSecondary
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Filtros por estado: cuatro segmentos con contadores dinámicos.
            // `TabRow` es apropiado porque son vistas mutuamente excluyentes de
            // la misma lista, no acciones independientes.
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = extended.surface1,
                contentColor = extended.accent
            ) {
                tabLabels.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Lista de reservas con tres ramas: carga inicial, vacío y datos.
            // El cargador solo ocupa la pantalla si aún no hay reservas; durante
            // un refresco con datos se conserva la lista visible.
            if (uiState.isLoading && uiState.reservations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ParkeoLoadingView(message = "Cargando reservas...")
                }
            } else if (filteredReservations.isEmpty()) {
                // Estado vacío: distingue entre "no hay coincidencias" y "no
                // hay reservas en este estado para la sede". Un error de red no
                // cae aquí; se informa por Snackbar y la lista queda como esté.
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ParkeoEmptyState(
                        title = "No hay reservas",
                        description = if (searchQuery.isNotBlank()) "No se encontraron coincidencias para '$searchQuery'"
                        else "No hay reservas en este estado para la cochera seleccionada"
                    )
                }
            } else {
                // Lista perezosa: solo compone las tarjetas visibles. La clave
                // estable por `id` permite a Compose reutilizar elementos y
                // conservar el scroll al cambiar el filtro o la búsqueda.
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSm),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    items(filteredReservations, key = { it.id }) { reservation ->
                        OperatorReservationCard(
                            reservation = reservation,
                            // No se ejecuta la acción directamente: primero se
                            // guarda la reserva y el tipo de operación para que
                            // el diálogo pida confirmación.
                            onCheckIn = { reservationToConfirmAction = reservation to "CHECK_IN" },
                            onCheckOut = { reservationToConfirmAction = reservation to "CHECK_OUT" }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de confirmación para Check-In o Check-Out. Se abre solo cuando
    // hay una acción pendiente; al cerrar o confirmar se limpia el par para
    // evitar reaperturas. La operación real se envía al ViewModel en el botón
    // de confirmación, nunca al pulsar la tarjeta.
    reservationToConfirmAction?.let { (reservation, action) ->
        val isCheckIn = action == "CHECK_IN"
        // Diálogo: resume vehículo, espacio, código y consecuencia esperada.
        AlertDialog(
            onDismissRequest = { reservationToConfirmAction = null },
            title = {
                Text(if (isCheckIn) "¿Registrar ingreso del vehículo?" else "¿Registrar salida del vehículo?")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Vehículo: ${reservation.vehicleLicensePlate ?: "Sin placa"}",
                        fontWeight = FontWeight.Bold
                    )
                    Text("Espacio asignado: ${reservation.spaceNumber ?: "N/A"}")
                    Text("Código: ${reservation.confirmationCode ?: "PKO-${reservation.id}"}")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isCheckIn)
                            "El vehículo ingresará a la cochera y el espacio pasará a estado OCUPADO."
                        else
                            "El vehículo saldrá de la cochera y el espacio se liberará a DISPONIBLE.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textSecondary
                    )
                }
            },
            // Acción principal: cierra el diálogo, da retroalimentación háptica
            // y delega la transición al ViewModel. El color distingue ingreso
            // (acento) de salida (verde de señal).
            confirmButton = {
                Button(
                    onClick = {
                        val id = reservation.id
                        reservationToConfirmAction = null
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        // El backend valida de nuevo la asignación del operador,
                        // la sede de la reserva y la legalidad de la transición.
                        if (isCheckIn) {
                            viewModel.checkIn(id)
                        } else {
                            viewModel.checkOut(id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCheckIn) extended.accent else extended.signalGreen
                    )
                ) {
                    Text(
                        if (isCheckIn) "Confirmar Ingreso" else "Confirmar Salida",
                        color = if (isCheckIn) extended.onAccent else Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { reservationToConfirmAction = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
/**
 * Tarjeta resumen de una reserva dentro de la lista del operador.
 *
 * Muestra la identidad operativa (placa y código), el espacio, el horario y el
 * monto, además de un badge de estado. La acción disponible depende del estado:
 * ingreso para reservas pendientes/confirmadas, salida para reservas activas y
 * ninguna para estados terminales. La tarjeta no muta datos por sí misma;
 * únicamente notifica al padre mediante callbacks.
 *
 * @param reservation DTO de la reserva a presentar. Sus campos pueden faltar o
 *   ser nulos porque provienen de JSON; la tarjeta aplica valores de respaldo.
 * @param onCheckIn Callback que solicita registrar el ingreso del vehículo.
 * @param onCheckOut Callback que solicita registrar la salida del vehículo.
 */
private fun OperatorReservationCard(
    reservation: ReservationDto,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit
) {
    val extended = ParkeoTheme.colors
    // Estado de la reserva tal como lo entrega el backend. Se compara como
    // cadena porque el DTO de red no usa un enum Kotlin.
    val status = reservation.status

    // Color del badge según el ciclo de vida de la reserva:
    // verde = activa/dentro, acento = confirmada, ámbar = pendiente,
    // gris = completada, rojo = cancelada o no asistió.
    val statusBadgeColor = when (status) {
        "ACTIVE" -> extended.signalGreen
        "CONFIRMED" -> extended.accent
        "PENDING" -> extended.signalAmber
        "COMPLETED" -> extended.textTertiary
        "CANCELLED", "NO_SHOW" -> extended.signalRed
        else -> extended.textSecondary
    }

    // Etiqueta legible para el operador; el valor crudo solo se muestra si el
    // backend incorpora un estado nuevo que esta versión aún no conoce.
    val statusLabel = when (status) {
        "ACTIVE" -> "En Cochera"
        "CONFIRMED" -> "Confirmada"
        "PENDING" -> "Pendiente"
        "COMPLETED" -> "Completada"
        "CANCELLED" -> "Cancelada"
        "NO_SHOW" -> "No asistió"
        else -> status
    }

    ParkeoCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Dimens.spacingMd),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cabecera: identidad de la reserva a la izquierda (placa, espacio
            // y código) y badge de estado a la derecha.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge que imita una placa peruana. La placa es el dato
                    // principal para que el operador identifique el vehículo en
                    // la cochera; si el DTO no la trae, se muestra "SIN PLACA".
                    Surface(
                        color = extended.surface3,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, extended.borderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "PERÚ",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = extended.signalRed,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = reservation.vehicleLicensePlate ?: "SIN PLACA",
                                style = Typography.MonospaceTechnical,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = extended.textPrimary
                            )
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Espacio: ${reservation.spaceNumber ?: "Automático"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = extended.textPrimary
                        )
                        Text(
                            text = reservation.confirmationCode ?: "PKO-${reservation.id}",
                            style = Typography.MonospaceTechnical,
                            fontSize = 11.sp,
                            color = extended.textTertiary
                        )
                    }
                }

                // Badge de estado: color tenue de fondo y borde del mismo tono
                // para mantener legibilidad en tema claro y oscuro.
                Surface(
                    color = statusBadgeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = statusLabel,
                        color = statusBadgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Separador visual entre identidad y datos económicos.
            Divider(color = extended.borderSubtle, thickness = 0.5.dp)

            // Datos de horario y monto: el intervalo se muestra en hora local
            // abreviada y el total con dos decimales. `totalPrice` puede ser
            // nulo, en cuyo caso se presenta 0.00 como respaldo visual.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "PROGRAMADO",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = extended.textTertiary
                    )
                    Text(
                        text = "${formatIso(reservation.startTime)} - ${formatIso(reservation.endTime)}",
                        style = Typography.MonospaceTechnical,
                        fontSize = 12.sp,
                        color = extended.textPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "MONTO",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = extended.textTertiary
                    )
                    Text(
                        text = "S/ ${String.format(java.util.Locale.US, "%.2f", reservation.totalPrice ?: 0.0)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = extended.accent
                    )
                }
            }

            // Acciones permitidas según el estado de la reserva. La UI evita
            // ofrecer operaciones inválidas, pero la autorización y la
            // transición real se validan en el backend.
            when (status) {
                "PENDING", "CONFIRMED" -> {
                    // La reserva aún no ha ingresado: se ofrece check-in. El
                    // callback solo abre el diálogo de confirmación del padre.
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onCheckIn,
                        colors = ButtonDefaults.buttonColors(containerColor = extended.accent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Registrar Ingreso (Check-In)",
                            color = extended.onAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
                "ACTIVE" -> {
                    // El vehículo ya está dentro: la única acción operativa es
                    // registrar la salida (check-out).
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onCheckOut,
                        colors = ButtonDefaults.buttonColors(containerColor = extended.signalGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        Icon(Icons.Filled.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Registrar Salida (Check-Out)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Convierte una marca de tiempo ISO en una hora corta para la tarjeta.
 *
 * No se usa una zona horaria explícita: se interpreta el texto tal como llega
 * del contrato, quitando una posible `Z` final. Si el valor es nulo, vacío o no
 * se puede parsear, se devuelve un respaldo legible en lugar de propagar una
 * excepción a la interfaz.
 *
 * @param iso Fecha y hora en formato ISO-8601, por ejemplo
 *   `2026-10-19T14:30:00` o con sufijo `Z`. Puede ser `null`.
 * @return Hora en formato `HH:mm`; `"--:--"` si no hay dato; o los últimos
 *   caracteres del texto original si el parseo falla.
 */
private fun formatIso(iso: String?): String {
    if (iso.isNullOrBlank()) return "--:--"
    return try {
        val dt = LocalDateTime.parse(iso.replace("Z", ""))
        dt.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        // Respaldo defensivo: un contrato inesperado no debe romper la lista.
        // Se recorta el final del texto para intentar mostrar una hora útil.
        iso.takeLast(8).take(5)
    }
}
