/*
 * ============================================================================
 * PARKeo · Android
 * Archivo: ReservationsScreen.kt
 * Capa: presentación (Jetpack Compose + Material 3)
 *
 * Propósito:
 *   Implementar la pantalla "Mis reservas". Presenta las reservas del usuario
 *   autenticado agrupadas por pestañas, resume cada reserva en una tarjeta y
 *   permite abrir el boarding pass digital o solicitar una cancelación. La
 *   pantalla es un componente de presentación: observa el estado publicado por
 *   `ReservationViewModel` y delega toda acción en el ViewModel o en callbacks
 *   de navegación; no llama directamente a Retrofit ni a la base de datos.
 *
 * Conceptos clave:
 *   - Reserva: vínculo entre un usuario, un vehículo propio, un estacionamiento,
 *     un espacio y un intervalo de tiempo. El backend es la fuente autoritativa
 *     del espacio asignado, del precio y del estado final.
 *   - Estados del ciclo de vida: `PENDING` (pendiente), `CONFIRMED`
 *     (confirmada), `ACTIVE` (en curso), `COMPLETED` (completada), `CANCELLED`
 *     (cancelada) y `NO_SHOW` (el usuario no se presentó). El estado del espacio
 *     (`AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE`) es una dimensión
 *     distinta del estado de la reserva.
 *   - Disponibilidad: cantidad de espacios libres y ventanas de tiempo sin
 *     solapamiento. La lista no calcula disponibilidad; solo muestra reservas
 *     ya creadas.
 *   - `StateFlow`: `ReservationViewModel.uiState` expone un flujo observable con
 *     carga, reservas, error, mensajes y banderas de éxito. La pantalla lo
 *     convierte en estado Compose con `collectAsState()`.
 *   - `LazyColumn`: lista eficiente que compone únicamente las tarjetas visibles
 *     y usa el `id` de cada reserva como clave estable.
 *   - Diálogos: `TicketBoardingPassDialog` (código QR y datos de acceso) y
 *     `CancelReservationDialog` (motivo y confirmación).
 *   - Snackbar: la app usa `ReservationUiState.successMessage`/`error`, pero esta
 *     pantalla concreta no muestra un `Snackbar`; los mensajes se limpian al
 *     procesar la cancelación. Se documenta para no confundir el estado del
 *     ViewModel con un elemento visual que aquí no existe.
 *
 * Flujo de usuario:
 *   1. Al entrar se consultan las reservas de la pestaña "Próximas"
 *      (`PENDING,CONFIRMED`).
 *   2. El usuario cambia entre "Próximas", "Activas" e "Historial"; cada cambio
 *      dispara una nueva consulta filtrada por estado.
 *   3. Cada tarjeta muestra estacionamiento, dirección, código, fecha, placa y
 *      precio; al tocarla se abre el boarding pass digital.
 *   4. Solo las reservas `PENDING` y `CONFIRMED` ofrecen el botón "Cancelar".
 *      La cancelación exige elegir un motivo en un diálogo.
 *   5. Volver atrás se delega en `onNavigateBack`; la navegación real vive en
 *      `ParkeoNavGraph.kt`, no en esta pantalla.
 *
 * Flujo de datos:
 *   `ReservationViewModel.uiState` (StateFlow)
 *     -> `collectAsState()`
 *     -> ramas de carga, lista vacía y lista con datos
 *     -> `ReservationItemCard` por cada `ReservationDto`.
 *   Las acciones suben como eventos: `onOpenBoardingPass`, `onCancelRequest`,
 *   `viewModel.cancelReservation(...)` y `viewModel.loadReservations(...)`.
 *
 * Relación con ViewModel, navegación, componentes y backend:
 *   - `ReservationViewModel.loadReservations(status)` consulta
 *     `GET /api/reservations` (a través del repositorio) con el filtro de estado.
 *   - `ReservationViewModel.cancelReservation(id, reason)` envía la cancelación;
 *     al tener éxito publica `cancelledReservationId`, y esta pantalla usa ese
 *     valor para cancelar los recordatorios locales con `ReminderScheduler`.
 *   - Refresco: no hay gesto de "pull to refresh"; la lista se vuelve a consultar
 *     al cambiar de pestaña y el ViewModel la recarga tras cancelar.
 *   - `ParkeoTopBar`, `ParkeoCard`, `ParkeoBadge`, `ParkeoButton`,
 *     `ParkeoEmptyState` y `ParkeoLoadingView` pertenecen a
 *     `pe.parkeo.ui.components`; el tema y las dimensiones vienen de
 *     `pe.parkeo.ui.theme`. `QrCodeMatrix` y `BoardingPassRow` se definen
 *     localmente en este archivo como helpers privados de presentación.
 *   - El check-in y el check-out no se ejecutan desde esta pantalla: son
 *     operaciones del flujo operativo (`OperatorReservationsScreen` y
 *     `ReservationService`) y requieren verificación de asignación en backend.
 *
 * Nota de implementación:
 *   `uiState.error` existe y se documenta, pero esta versión de la pantalla no
 *   lo renderiza; por eso el bloque `when` solo distingue carga, lista vacía y
 *   lista con datos. No se modificó la lógica, únicamente se dejó constancia.
 *
 * Guía de estudio:
 *   docs/GUIA_MAESTRA_ESTUDIANTE.md, en especial la sección 11
 *   "Regla crítica: reservar sin conflictos".
 * ============================================================================
 */
package pe.parkeo.ui.screens.reservation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import pe.parkeo.data.remote.dto.ReservationDto
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.ReservationViewModel
import pe.parkeo.util.DirectionsButton
import pe.parkeo.util.ReminderScheduler

/**
 * Punto de entrada de la pantalla "Mis reservas".
 *
 * Observa el `StateFlow` del ViewModel y construye un `Scaffold` con barra
 * superior, pestañas de filtrado y el contenido correspondiente al estado
 * actual. Mantiene en memoria de composición la pestaña seleccionada y la
 * reserva asociada a cada diálogo.
 *
 * Detalles didácticos:
 * - `collectAsState()` convierte el flujo del ViewModel en estado observable por
 *   Compose; cada emisión provoca recomposición de las partes que leen
 *   `uiState`.
 * - `remember { mutableIntStateOf(0) }` conserva la pestaña entre
 *   recomposiciones y sobrevive mientras el composable permanezca en la
 *   composición. `mutableIntStateOf` evita autoboxing para un entero.
 * - `remember { mutableStateOf<ReservationDto?>(null) }` guarda la reserva
 *   elegida para abrir un diálogo; `null` significa "diálogo cerrado".
 * - `LaunchedEffect(selectedTab)` ejecuta la carga cada vez que cambia la
 *   pestaña (incluida la primera composición). Es el puente entre un evento de
 *   UI y una función suspendida del ViewModel.
 * - `LaunchedEffect(uiState.cancelledReservationId)` reacciona a la confirmación
 *   del backend: cancela los recordatorios locales y limpia los mensajes del
 *   ViewModel para que el efecto no se repita.
 *
 * @param viewModel ViewModel de reservas que expone `uiState` y las acciones
 *   `loadReservations` y `cancelReservation`.
 * @param onNavigateBack Callback que sube el evento de volver atrás a la
 *   navegación; esta pantalla no conoce el grafo de navegación.
 */
@Composable
fun ReservationsScreen(
    viewModel: ReservationViewModel,
    onNavigateBack: () -> Unit
) {
    // Estado observable de la pantalla y tema extendido de PARKeo.
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors
    val context = LocalContext.current

    // Pestaña activa. Las listas `tabs` y `statusFilters` se relacionan por
    // índice: la pestaña 0 envía "PENDING,CONFIRMED", la 1 "ACTIVE" y la 2 el
    // histórico. Si se agrega una pestaña, debe agregarse su filtro en la misma
    // posición.
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Próximas", "Activas", "Historial")
    val statusFilters = listOf("PENDING,CONFIRMED", "ACTIVE", "COMPLETED,CANCELLED,NO_SHOW")

    // Reserva mostrada en cada diálogo. Un valor no nulo abre el diálogo; null
    // lo cierra. Mantener el objeto completo permite mostrar sus datos sin
    // volver a buscar en la lista.
    var boardingPassReservation by remember { mutableStateOf<ReservationDto?>(null) }
    var reservationToCancel by remember { mutableStateOf<ReservationDto?>(null) }

    // Carga inicial y recarga al cambiar de pestaña. La clave `selectedTab`
    // evita ejecutar el efecto en cada recomposición.
    LaunchedEffect(selectedTab) {
        viewModel.loadReservations(statusFilters[selectedTab])
    }

    // Cuando el ViewModel confirma una cancelación, se apagan sus recordatorios.
    // `cancelledReservationId` funciona como señal de éxito: el ViewModel ya
    // recargó la lista y aquí se completa el efecto secundario local.
    LaunchedEffect(uiState.cancelledReservationId) {
        uiState.cancelledReservationId?.let { id ->
            ReminderScheduler.cancel(context, id)
            viewModel.clearMessages()
        }
    }

    // Contenedor principal: barra superior con navegación y cuerpo con pestañas.
    // `contentWindowInsets = WindowInsets(0.dp)` evita dobles márgenes porque la
    // barra y el cuerpo gestionan sus propios espacios.
    Scaffold(
        containerColor = extended.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            ParkeoTopBar(
                title = "Mis reservas",
                onNavigationClick = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filtros: barra de pestañas fija. Cada pestaña es un `Surface`
            // clickeable que cambia `selectedTab`; el color y el borde indican
            // visualmente la selección. El peso `weight(1f)` reparte el ancho en
            // partes iguales.
            Surface(
                color = extended.surface1,
                border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, extended.borderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.spacingMd, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = index },
                            color = if (isSelected) extended.accent else extended.surface2,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                Dimens.borderHairline,
                                if (isSelected) extended.accent else extended.borderSubtle
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) extended.onAccent else extended.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Estados de contenido. El orden importa: mientras se carga se
            // muestra el indicador; si la lista está vacía se muestra el estado
            // vacío; en otro caso se pinta la lista. No hay rama para
            // `uiState.error` en esta versión: el error queda en el ViewModel.
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        ParkeoLoadingView(message = "Cargando tus reservas...")
                    }
                }

                uiState.reservations.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        // Estado vacío contextual: el subtítulo reutiliza el
                        // nombre de la pestaña activa para explicar el filtro.
                        ParkeoEmptyState(
                            title = "No hay reservas",
                            subtitle = "No encontramos reservas en la sección \"${tabs[selectedTab]}\"",
                            icon = Icons.Filled.BookmarkBorder
                        )
                    }
                }

                else -> {
                    // Lista: `key = { it.id }` da identidad estable a cada fila,
                    // lo que ayuda a conservar el estado y animar cambios sin
                    // reutilizar por posición. Cada tarjeta recibe callbacks que
                    // solo abren el diálogo correspondiente; la acción real se
                    // confirma dentro del diálogo.
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(Dimens.spacingMd),
                        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                    ) {
                        items(uiState.reservations, key = { it.id }) { reservation ->
                            ReservationItemCard(
                                reservation = reservation,
                                onOpenBoardingPass = { boardingPassReservation = reservation },
                                onCancelRequest = { reservationToCancel = reservation }
                            )
                        }
                    }
                }
            }
        }

        // Diálogo: boarding pass digital de la reserva seleccionada.
        boardingPassReservation?.let { reservation ->
            TicketBoardingPassDialog(
                reservation = reservation,
                onDismiss = { boardingPassReservation = null }
            )
        }

        // Diálogo: confirmación de cancelación. Al confirmar se envía el motivo
        // al ViewModel y se cierra el diálogo; la recarga de la lista y el
        // apagado de recordatorios ocurren cuando llega la respuesta del backend.
        reservationToCancel?.let { reservation ->
            CancelReservationDialog(
                reservation = reservation,
                onDismiss = { reservationToCancel = null },
                onConfirm = { reason ->
                    viewModel.cancelReservation(reservation.id, reason)
                    reservationToCancel = null
                }
            )
        }
    }
}

/**
 * Tarjeta resumen de una reserva dentro de la lista.
 *
 * Traduce el estado técnico del backend a una etiqueta en español y decide qué
 * acciones ofrecer. La tarjeta completa es clickeable para abrir el boarding
 * pass; el botón de cancelar solo aparece cuando la reserva todavía puede
 * cancelarse.
 *
 * Reglas visibles:
 * - `PENDING` y `CONFIRMED` se consideran reservas futuras y permiten cancelar.
 * - `ACTIVE` se resalta con borde de acento, pero no ofrece cancelación aquí.
 * - `COMPLETED`, `CANCELLED` y `NO_SHOW` son estados terminales o históricos;
 *   solo se consultan.
 * - El mapeo a `ParkeoBadgeStatus` es puramente visual: `NO_SHOW` cae en el
 *   `else` y usa el estilo cerrado, con etiqueta "No se presentó".
 *
 * @param reservation Datos de la reserva que se resumen.
 * @param onOpenBoardingPass Callback para mostrar el diálogo con código QR y
 *   detalles de acceso.
 * @param onCancelRequest Callback para abrir el diálogo de cancelación; solo se
 *   conecta cuando el estado permite cancelar.
 */
@Composable
private fun ReservationItemCard(
    reservation: ReservationDto,
    onOpenBoardingPass: () -> Unit,
    onCancelRequest: () -> Unit
) {
    val extended = ParkeoTheme.colors

    // Reglas de acción según estado. Cancelar solo tiene sentido antes de que la
    // estancia comience; una reserva activa debe gestionarse con check-out.
    val canCancel = reservation.status in listOf("PENDING", "CONFIRMED")
    val isActive = reservation.status == "ACTIVE"

    // El estado de la reserva se adapta a la paleta de badges existente. La
    // correspondencia no es semántica uno a uno: `Reserved` representa tanto
    // pendiente como confirmada, y `Occupied` se reutiliza para cancelada.
    val badgeStatus = when (reservation.status) {
        "PENDING" -> ParkeoBadgeStatus.Reserved
        "CONFIRMED" -> ParkeoBadgeStatus.Reserved
        "ACTIVE" -> ParkeoBadgeStatus.Available
        "COMPLETED" -> ParkeoBadgeStatus.Closed
        "CANCELLED" -> ParkeoBadgeStatus.Occupied
        else -> ParkeoBadgeStatus.Closed
    }

    ParkeoCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenBoardingPass),
        accentBorder = isActive
    ) {
        Column(
            modifier = Modifier.padding(Dimens.spacingLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
        ) {
            // Encabezado: nombre y dirección del estacionamiento a la izquierda,
            // badge de estado traducido a texto legible a la derecha. El estado
            // siempre proviene del backend; la UI no lo deduce de las fechas.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reservation.parkingLotName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = extended.textPrimary
                    )
                    Text(
                        text = reservation.parkingLotAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textSecondary
                    )
                }

                ParkeoBadge(
                    status = badgeStatus,
                    labelOverride = when (reservation.status) {
                        "PENDING" -> "Pendiente"
                        "CONFIRMED" -> "Confirmada"
                        "ACTIVE" -> "En curso"
                        "COMPLETED" -> "Completada"
                        "CANCELLED" -> "Cancelada"
                        "NO_SHOW" -> "No se presentó"
                        else -> reservation.status
                    }
                )
            }

            // Código de confirmación: etiqueta compacta en tipografía monoespaciada.
            // Se omite por completo si el backend no devolvió código; el boarding
            // pass tiene su propio valor de respaldo.
            reservation.confirmationCode?.let { code ->
                Surface(
                    color = extended.surface2,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, extended.accent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCode2,
                            contentDescription = null,
                            tint = extended.accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = code,
                            style = Typography.MonospaceTechnical,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = extended.accent
                        )
                    }
                }
            }

            HorizontalDivider(
                color = extended.borderSubtle,
                thickness = Dimens.borderHairline,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Detalles técnicos: fecha de inicio, placa del vehículo y precio.
            // `formatDisplayDate` transforma el ISO del backend a `dd/MM HH:mm`.
            // El precio solo se muestra si existe y se formatea con dos decimales
            // usando `Locale.US` para que el separador decimal sea siempre un
            // punto, independientemente de la configuración regional del equipo.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = extended.accent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = formatDisplayDate(reservation.startTime),
                            style = Typography.MonospaceTechnical,
                            fontSize = 12.sp,
                            color = extended.textPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsCar,
                            contentDescription = null,
                            tint = extended.textTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = reservation.vehicleLicensePlate,
                            style = Typography.MonospaceTechnical,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = extended.accent
                        )
                    }
                }

                reservation.totalPrice?.let { price ->
                    Text(
                        text = "S/ ${String.format(java.util.Locale.US, "%.2f", price)}",
                        style = Typography.MonospaceTechnical,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = extended.accent,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Acciones: abrir el boarding pass siempre está disponible; cancelar
            // se agrega condicionalmente. No hay botones de check-in/check-out
            // porque pertenecen al flujo operativo y requieren validación de
            // asignación en el backend.
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ParkeoButton(
                    text = "Ver Boarding Pass QR",
                    onClick = onOpenBoardingPass,
                    style = ParkeoButtonStyle.Secondary,
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                )

                if (canCancel) {
                    ParkeoButton(
                        text = "Cancelar",
                        onClick = onCancelRequest,
                        style = ParkeoButtonStyle.Destructive,
                        modifier = Modifier.height(38.dp)
                    )
                }
            }
        }
    }
}

/**
 * Diálogo de "boarding pass" digital de una reserva.
 *
 * Presenta un código visual, el código de reserva en texto, los datos de acceso
 * (estacionamiento, espacio, placa, inicio, salida y tarifa) y, si el backend
 * entregó coordenadas, un botón para abrir la ruta en mapas.
 *
 * El contenido se envuelve en una columna con desplazamiento vertical para que
 * quepa en pantallas pequeñas. El código visible usa `confirmationCode`; si no
 * existe, se genera un respaldo local `PKO-<id>` solo para presentación. El
 * backend sigue siendo la fuente autoritativa para validar el acceso.
 *
 * @param reservation Reserva cuyos datos se muestran en el pase.
 * @param onDismiss Callback para cerrar el diálogo.
 */
@Composable
private fun TicketBoardingPassDialog(
    reservation: ReservationDto,
    onDismiss: () -> Unit
) {
    val extended = ParkeoTheme.colors
    // Valor de presentación: nunca reemplaza al código real del backend.
    val code = reservation.confirmationCode ?: "PKO-${reservation.id}"

    // `Dialog` de Compose (no `AlertDialog`) para controlar por completo el
    // diseño tipo ticket. `onDismissRequest` cubre toque fuera y botón atrás.
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = extended.surface1,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, extended.accent.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.spacingLg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cabecera del pase: título fijo y botón de cierre. El ícono es
                // decorativo (`contentDescription = null`); el botón sí expone
                // "Cerrar" para accesibilidad.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ConfirmationNumber,
                            contentDescription = null,
                            tint = extended.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "BOARDING PASS DIGITAL",
                            style = Typography.MonospaceTechnical,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = extended.accent,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cerrar",
                            tint = extended.textTertiary
                        )
                    }
                }

                // Código QR: caja blanca de 190 dp para garantizar contraste. El
                // dibujo lo genera `QrCodeMatrix` a partir del código de la
                // reserva; es una representación visual determinista, no un QR
                // estándar escaneable.
                Surface(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        QrCodeMatrix(
                            content = code,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Código y explicación: se muestran en monoespaciada para que
                // todos los caracteres ocupen el mismo ancho y sea fácil dictarlo
                // o compararlo en puerta.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CÓDIGO DE RESERVA",
                        style = Typography.MonospaceTechnical,
                        fontSize = 10.sp,
                        color = extended.textTertiary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = code,
                        style = Typography.MonospaceTechnical,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = extended.accent,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Muestra este código al operador en puerta",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = extended.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                HorizontalDivider(
                    color = extended.borderSubtle,
                    thickness = Dimens.borderHairline
                )

                // Resumen: filas etiqueta-valor reutilizables. El espacio puede
                // venir vacío cuando el backend lo asigna recién en el acceso;
                // en ese caso se muestra "En puerta". Placa y espacio se
                // resaltan porque son los datos que el operador verifica.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BoardingPassRow(label = "Estacionamiento", value = reservation.parkingLotName)
                    BoardingPassRow(label = "Dirección", value = reservation.parkingLotAddress)
                    BoardingPassRow(
                        label = "Espacio asignado",
                        value = if (reservation.spaceNumber.isNotBlank()) reservation.spaceNumber else "En puerta",
                        isHighlight = true
                    )
                    BoardingPassRow(
                        label = "Placa del vehículo",
                        value = reservation.vehicleLicensePlate,
                        isHighlight = true
                    )
                    BoardingPassRow(label = "Inicio", value = formatDisplayDate(reservation.startTime))
                    BoardingPassRow(label = "Salida programada", value = formatDisplayDate(reservation.endTime))
                    reservation.totalPrice?.let { price ->
                        BoardingPassRow(
                            label = "Tarifa estimada",
                            value = "S/ ${String.format(java.util.Locale.US, "%.2f", price)}",
                            isHighlight = true
                        )
                    }
                }

                // Ruta opcional: solo se ofrece si el detalle de la reserva trae
                // ambas coordenadas. `DirectionsButton` encapsula la intención de
                // abrir una aplicación de mapas.
                val lat = reservation.parkingLotLatitude
                val lng = reservation.parkingLotLongitude
                if (lat != null && lng != null) {
                    Spacer(Modifier.height(4.dp))
                    DirectionsButton(latitude = lat, longitude = lng)
                }

                ParkeoButton(
                    text = "Cerrar",
                    onClick = onDismiss,
                    style = ParkeoButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Fila etiqueta-valor del boarding pass.
 *
 * Se usa para mantener alineados los datos del ticket. El valor se alinea a la
 * derecha y admite un modo resaltado con tipografía monoespaciada y color de
 * acento para los campos que el operador debe verificar (espacio, placa y
 * tarifa).
 *
 * @param label Texto descriptivo del campo, alineado a la izquierda.
 * @param value Valor proveniente de la reserva.
 * @param isHighlight Si es `true`, aplica estilo monoespaciado, negrita y color
 *   de acento. Por defecto es `false`.
 */
@Composable
private fun BoardingPassRow(
    label: String,
    value: String,
    isHighlight: Boolean = false
) {
    val extended = ParkeoTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = extended.textSecondary
        )
        Text(
            text = value,
            style = if (isHighlight) Typography.MonospaceTechnical else MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) extended.accent else extended.textPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

/**
 * Diálogo de confirmación de cancelación.
 *
 * Obliga a elegir un motivo antes de enviar la acción. Los motivos son
 * opciones de UI; el backend decide si la reserva puede cancelarse según su
 * estado y sus reglas. Al confirmar se entrega el motivo seleccionado al
 * callback, que a su vez llama a `ReservationViewModel.cancelReservation`.
 *
 * @param reservation Reserva que se intenta cancelar; se usa el nombre del
 *   estacionamiento en el mensaje.
 * @param onDismiss Callback para cerrar sin cancelar.
 * @param onConfirm Recibe el motivo elegido y ejecuta la cancelación real.
 */
@Composable
private fun CancelReservationDialog(
    reservation: ReservationDto,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val extended = ParkeoTheme.colors

    // Catálogo de motivos visibles. "Otro motivo" no abre un campo de texto en
    // esta versión; se envía la etiqueta tal cual al backend.
    val reasons = listOf(
        "Cambio de planes",
        "Encontré otro estacionamiento",
        "Error en la fecha u horario",
        "Otro motivo"
    )
    // `remember` conserva la selección mientras el diálogo permanece compuesto.
    var selectedReason by remember { mutableStateOf(reasons[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extended.surface1,
        titleContentColor = extended.textPrimary,
        textContentColor = extended.textSecondary,
        title = {
            Text(
                text = "¿Cancelar reserva?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Estás a punto de cancelar tu reserva en \"${reservation.parkingLotName}\". Selecciona el motivo:",
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textSecondary
                )

                // Motivos: cada opción es clickeable en toda su superficie. El
                // `RadioButton` también actualiza el estado; ambos caminos llevan
                // al mismo valor para que el toque sea cómodo.
                reasons.forEach { reason ->
                    val isSelected = reason == selectedReason
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedReason = reason },
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
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = extended.textPrimary
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedReason = reason },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = extended.accent,
                                    unselectedColor = extended.textTertiary
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            ParkeoButton(
                text = "Confirmar Cancelación",
                onClick = { onConfirm(selectedReason) },
                style = ParkeoButtonStyle.Destructive
            )
        },
        dismissButton = {
            ParkeoButton(
                text = "Regresar",
                onClick = onDismiss,
                style = ParkeoButtonStyle.Ghost
            )
        }
    )
}

/**
 * Dibuja una matriz pseudo-QR determinista a partir de un texto.
 *
 * Importante: no es un código QR conforme al estándar ni debe usarse para
 * validación real. Se construye una cuadrícula de 21x21 con patrones de
 * localización en tres esquinas, líneas de sincronización y celdas de "datos"
 * derivadas del `hashCode` del contenido. El mismo texto produce siempre la
 * misma imagen, lo que basta para la presentación del boarding pass. El acceso
 * se valida en el backend con el código de reserva, no escaneando este dibujo.
 *
 * `remember(content)` evita recalcular la matriz en cada recomposición y solo
 * la regenera si cambia el código. `Canvas` dibuja cada celda activa como un
 * rectángulo negro ligeramente menor que la celda para dejar separación.
 *
 * @param content Texto que alimenta la matriz (código de reserva).
 * @param modifier Modificador de tamaño y disposición del lienzo; por defecto
 *   vacío.
 */
@Composable
private fun QrCodeMatrix(
    content: String,
    modifier: Modifier = Modifier
) {
    // 21x21 es el tamaño de la versión 1 de un QR real; aquí se usa como grilla
    // visual fija.
    val gridSize = 21
    val bitMatrix = remember(content) {
        val matrix = Array(gridSize) { BooleanArray(gridSize) }

        // Patrones de localización ("finder patterns"): cuadrado de 7x7 con
        // borde y centro de 3x3 en tres esquinas, igual que un QR real.
        /**
         * Marca un patrón de localización de 7x7 cuya esquina superior izquierda
         * está en (`startR`, `startC`). El borde exterior y el centro de 3x3 se
         * activan; el resto queda apagado.
         *
         * @param startR Fila inicial del patrón dentro de la matriz.
         * @param startC Columna inicial del patrón dentro de la matriz.
         */
        fun markFinder(startR: Int, startC: Int) {
            for (r in 0 until 7) {
                for (c in 0 until 7) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    matrix[startR + r][startC + c] = isBorder || isCenter
                }
            }
        }
        markFinder(0, 0)
        markFinder(0, gridSize - 7)
        markFinder(gridSize - 7, 0)

        // Líneas de sincronización (timing patterns)
        for (i in 7 until gridSize - 7) {
            matrix[6][i] = (i % 2 == 0)
            matrix[i][6] = (i % 2 == 0)
        }

        // Celdas de datos: se generan de forma determinista a partir del hash
        // del código. Se excluyen las zonas de localización y sincronización
        // para no ensuciar los patrones visuales. No hay corrección de errores,
        // máscaras ni codificación real: es solo una representación gráfica.
        var hash = content.hashCode()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val inFinder1 = r < 8 && c < 8
                val inFinder2 = r < 8 && c >= gridSize - 8
                val inFinder3 = r >= gridSize - 8 && c < 8
                val inTiming = r == 6 || c == 6
                if (!inFinder1 && !inFinder2 && !inFinder3 && !inTiming) {
                    hash = (hash * 31 + r * 17 + c * 23)
                    matrix[r][c] = (hash and 1) == 1
                }
            }
        }
        matrix
    }

    // Dibujo: se usa el menor lado para que la grilla sea cuadrada y se centra
    // con un padding calculado. Cada celda activa se pinta al 95 % de su tamaño
    // para dejar una separación fina entre módulos.
    Canvas(modifier = modifier) {
        val cellSize = size.minDimension / gridSize
        val paddingX = (size.width - cellSize * gridSize) / 2f
        val paddingY = (size.height - cellSize * gridSize) / 2f

        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (bitMatrix[r][c]) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(paddingX + c * cellSize, paddingY + r * cellSize),
                        size = Size(cellSize * 0.95f, cellSize * 0.95f)
                    )
                }
            }
        }
    }
}

/**
 * Convierte una fecha ISO del backend a un texto corto para la interfaz.
 *
 * Entrada típica: `2026-10-19T18:30:00` (hora de Lima, sin zona explícita).
 * Se toman los primeros 16 caracteres, se reemplaza la `T` por un espacio y se
 * reordena la parte de fecha a `dd/MM`, conservando la hora: `19/10 18:30`.
 * Si el texto no tiene el formato esperado, se devuelve tal cual (limpio) para
 * no perder información ni lanzar excepciones. No se interpreta la zona
 * horaria; el backend ya envía la hora local de Lima.
 *
 * @param raw Fecha/hora en formato ISO recibida en `ReservationDto`.
 * @return Fecha legible `dd/MM HH:mm` o el texto original si no se puede
 *   transformar.
 */
private fun formatDisplayDate(raw: String): String {
    val clean = raw.take(16).replace("T", " ")
    return if (clean.length >= 10 && clean.contains("-")) {
        val parts = clean.split(" ")
        val ymd = parts[0].split("-")
        if (ymd.size == 3) {
            "${ymd[2]}/${ymd[1]} ${if (parts.size > 1) parts[1] else ""}".trim()
        } else clean
    } else clean
}