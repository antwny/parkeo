/*
 * ============================================================================
 * PARKeo · Android
 * Archivo: ReservationCreateScreen.kt
 * Capa: presentación (Jetpack Compose + Material 3)
 *
 * Propósito:
 *   Implementar la creación de una reserva para un estacionamiento concreto.
 *   La pantalla recibe el `parkingId` desde la navegación, carga el detalle del
 *   estacionamiento y los vehículos del usuario, permite elegir la hora de
 *   ingreso, la duración y el vehículo, muestra un precio estimado y, tras una
 *   confirmación explícita, delega la creación en `ReservationViewModel`.
 *
 * Estructura de la experiencia:
 *   No es un asistente con pasos bloqueantes, sino una sola columna desplazable
 *   con tarjetas consecutivas: resumen del estacionamiento, hora de ingreso,
 *   duración de la estadía, vehículo asociado, resumen de precio y CTA final.
 *   Los diálogos aparecen al final: confirmación, éxito y alta rápida de
 *   vehículo. Cada tarjeta está separada por `Dimens.spacingMd` y toda la
 *   columna usa `verticalScroll`, por lo que el contenido no se recicla como en
 *   una `LazyColumn`.
 *
 * Conceptos clave:
 *   - Reserva: relación entre usuario, vehículo propio, estacionamiento, espacio
 *     y ventana de tiempo. El backend calcula el precio y asigna el espacio.
 *   - Estados de reserva: `PENDING`, `CONFIRMED`, `ACTIVE`, `COMPLETED`,
 *     `CANCELLED` y `NO_SHOW`. La creación normalmente devuelve un estado
 *     inicial (pendiente o confirmado según la regla del servidor).
 *   - Disponibilidad: el estacionamiento muestra `availableSpaces`, un dato
 *     agregado del lote. Esta pantalla no elige un espacio concreto ni consulta
 *     disponibilidad por intervalo; envía `parkingSpaceId = null` y el backend
 *     bloquea/elige un espacio libre compatible.
 *   - Selección de vehículo: solo se listan vehículos del usuario autenticado
 *     (`getMyVehicles`). La placa se usa como identificador visible y el tipo de
 *     vehículo participa en la búsqueda de tarifa.
 *   - Fechas y precio: se trabaja con `LocalDateTime` en la zona
 *     `America/Lima`, se serializa como ISO `yyyy-MM-dd'T'HH:mm:ss` y el total
 *     mostrado es una estimación (`tarifa por hora x horas`). El backend
 *     revalida fechas, tarifa, disponibilidad y solapamiento, y su total es el
 *     autoritativo.
 *   - `StateFlow`: se observan tres ViewModels (`ParkingDetailViewModel`,
 *     `VehicleViewModel` y `ReservationViewModel`); cada uno publica su propio
 *     estado y la pantalla combina las partes que necesita.
 *   - `remember`, `LaunchedEffect` y estado derivado: `remember` conserva
 *     selecciones locales; `LaunchedEffect` dispara cargas y efectos
 *     secundarios; los valores calculados (vehículo, tarifa, total, fechas) se
 *     recalculan durante la composición a partir de ese estado. Esta pantalla
 *     no usa `derivedStateOf`: al ser cálculos simples, se evalúan directamente
 *     en cada recomposición.
 *   - Carga, error y éxito: la creación muestra `isLoading` en los botones,
 *     `error` en una franja dentro del formulario y el éxito en un diálogo con
 *     código y espacio. La carga del estacionamiento o de los vehículos no tiene
 *     indicador propio; sus tarjetas aparecen cuando los datos llegan.
 *   - Diálogos: `AlertDialog` para confirmar y celebrar, más un alta de vehículo
 *     embebida con formulario y `ExposedDropdownMenuBox`.
 *   - Recordatorios: al crearse la reserva se programa un aviso local con
 *     `ReminderScheduler`; en Android 13+ se solicita permiso de notificaciones.
 *
 * Reglas de negocio visibles y límites del cliente:
 *   - No reservar en el pasado: las opciones rápidas son "ahora + 5 min" o
 *     desplazamientos positivos (30, 60, 120 minutos). La opción manual ajusta
 *     en pasos de 30 minutos y su control de resta puede llegar a 0, por lo que
 *     la interfaz por sí sola no garantiza un intervalo estrictamente futuro.
 *     Al confirmar se vuelve a calcular la hora con `LocalDateTime.now(limaZone)`
 *     y se eliminan segundos/nanosegundos; aun así, la validación definitiva de
 *     fecha pasada la realiza el backend.
 *   - Duración: la UI ofrece 1, 2, 3, 4, 8 o 12 horas. No conoce el mínimo ni el
 *     máximo configurado por el estacionamiento; esas reglas las aplica el
 *     backend, que puede rechazar la solicitud.
 *   - Espacio disponible: la pantalla muestra espacios libres del lote, pero no
 *     reserva uno específico. El backend elige un espacio compatible y evita
 *     dobles asignaciones dentro de una transacción.
 *   - Vehículo propio: la lista proviene de los vehículos del usuario. El
 *     servidor vuelve a comprobar pertenencia y estado del vehículo.
 *   - Solapamiento: no se calcula en el cliente. El backend compara intervalos
 *     semiabiertos `[inicio, fin)` y bloquea el espacio antes de confirmar.
 *   - Precio: el cliente envía solo fechas, estacionamiento y vehículo; no envía
 *     el total. El servidor calcula la tarifa desde sus propias tablas.
 *   - Por qué revalidar en backend: el APK puede modificarse, el reloj del
 *     teléfono no es confiable y dos usuarios pueden competir por el mismo
 *     espacio. Una validación solo en Android no garantiza consistencia.
 *
 * Flujo de usuario:
 *   1. Se carga el detalle del estacionamiento y los vehículos/tipos.
 *   2. El usuario elige una hora de llegada y una duración.
 *   3. Elige uno de sus vehículos o registra uno nuevo en el diálogo.
 *   4. Revisa el resumen estimado y pulsa "Revisar y confirmar reserva".
 *   5. Confirma en el diálogo; el ViewModel crea la reserva.
 *   6. Si tiene éxito, se programa el recordatorio y se muestra el código y el
 *      espacio asignado. "Ver mis reservas" navega con `onReservationCreated`.
 *
 * Flujo de datos:
 *   `parkingId` (argumento de navegación)
 *     -> `ParkingDetailViewModel.loadParkingLot`
 *     -> `ParkingLotDetailDto` (nombre, dirección, espacios, tarifas)
 *   `VehicleViewModel` -> lista de vehículos y tipos
 *   selecciones locales -> cálculos de fecha/tarifa/total
 *   `ReservationViewModel.createReservation(...)`
 *     -> `ReservationUiState.currentReservation` y `reservationCreated`
 *
 * Relación con otras capas:
 *   - Navegación: `onNavigateBack` y `onReservationCreated` son callbacks; la
 *     ruta y el `parkingId` se resuelven en `ParkeoNavGraph.kt`/`NavRoutes.kt`.
 *   - Repositorios/API: el ViewModel llama a `ReservationRepository`,
 *     `VehicleRepository` y `ParkingRepository`, que usan Retrofit.
 *   - Backend: `ReservationController` y `ReservationService` validan y crean la
 *     reserva; el precio y el espacio asignado vuelven en `ReservationDto`.
 *   - Componentes: `ParkeoTopBar`, `ParkeoCard`, `ParkeoButton` y
 *     `ParkeoTextField` viven en `pe.parkeo.ui.components`; `ParkeoCardShape`
 *     pertenece a `pe.parkeo.ui.theme`.
 *
 * Guía de estudio:
 *   docs/GUIA_MAESTRA_ESTUDIANTE.md, en especial la sección 11
 *   "Regla crítica: reservar sin conflictos".
 * ============================================================================
 */
package pe.parkeo.ui.screens.reservation

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.data.remote.dto.VehicleTypeDto
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.ParkingDetailViewModel
import pe.parkeo.ui.viewmodel.ReservationViewModel
import pe.parkeo.ui.viewmodel.VehicleViewModel
import pe.parkeo.util.ReminderScheduler
import pe.parkeo.util.RequestNotificationPermission
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Opciones rápidas de hora de llegada.
 *
 * Cada valor encapsula el texto visible (`title`), una pista corta (`subtitle`)
 * y el desplazamiento en minutos desde "ahora" (`minutesOffset`) que se suma a
 * la hora de Lima. `CUSTOM` no tiene desplazamiento propio: la pantalla usa
 * `customMinutesOffset`, un valor manual ajustable en pasos de 30 minutos.
 *
 * Las opciones rápidas usan desplazamientos positivos; `CUSTOM` vale 0 porque su
 * desplazamiento real vive en el estado de la pantalla. El backend, de todos
 * modos, revalida que la fecha sea futura.
 *
 * @property title Texto principal del chip.
 * @property subtitle Texto secundario corto.
 * @property minutesOffset Minutos a sumar a la hora actual; `CUSTOM` usa 0
 *   porque su valor real vive en el estado de la pantalla.
 */
enum class ArrivalOption(val title: String, val subtitle: String, val minutesOffset: Long) {
    NOW("Inmediato", "+5 min", 5),
    IN_30("En 30 min", "+30 min", 30),
    IN_60("En 1 hora", "+60 min", 60),
    IN_120("En 2 horas", "+2h", 120),
    CUSTOM("Otra hora", "Manual", 0)
}

/**
 * Pantalla de creación de reserva para un estacionamiento.
 *
 * Combina tres fuentes de estado: detalle del estacionamiento, vehículos del
 * usuario y resultado de la creación. La pantalla no crea el objeto de dominio
 * directamente: arma los parámetros (estacionamiento, vehículo, inicio y fin) y
 * llama al ViewModel. El backend completa la operación, asigna espacio y
 * devuelve la reserva persistida.
 *
 * La confirmación se hace en dos fases: primero un diálogo con el resumen y
 * luego el envío. El botón principal solo se habilita si hay un vehículo
 * seleccionado, la lista no está vacía y no hay una creación en curso.
 *
 * @param parkingId Identificador del estacionamiento recibido por navegación;
 *   se usa para cargar el detalle y para enviar `parkingLotId`.
 * @param parkingViewModel ViewModel que carga `ParkingLotDetailDto` (nombre,
 *   dirección, espacios disponibles, coordenadas y tarifas).
 * @param vehicleViewModel ViewModel que expone vehículos del usuario, tipos de
 *   vehículo y la acción de alta rápida.
 * @param reservationViewModel ViewModel que crea la reserva y publica
 *   `currentReservation`, `reservationCreated`, `isLoading` y `error`.
 * @param onNavigateBack Callback para volver atrás desde la barra superior.
 * @param onReservationCreated Callback para abandonar la pantalla después del
 *   éxito y navegar, por ejemplo, a "Mis reservas".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationCreateScreen(
    parkingId: Long,
    parkingViewModel: ParkingDetailViewModel,
    vehicleViewModel: VehicleViewModel,
    reservationViewModel: ReservationViewModel,
    onNavigateBack: () -> Unit,
    onReservationCreated: () -> Unit
) {
    // Estado observable de los tres ViewModels implicados. Al leerlos con
    // `collectAsState`, cualquier emisión relevante recompone las secciones que
    // dependen de ellos. No se crea estado duplicado para datos del servidor.
    val parkingState by parkingViewModel.uiState.collectAsState()
    val vehicleState by vehicleViewModel.uiState.collectAsState()
    val reservationState by reservationViewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors
    val context = LocalContext.current

    // Formateadores creados una sola vez por composición:
    // - `limaZone`: PARKeo opera en hora de Lima; se evita depender de la zona
    //   del dispositivo.
    // - `formatter`: formato ISO local sin offset que espera el backend
    //   (`yyyy-MM-dd'T'HH:mm:ss`).
    // - `displayFormatter`: formato abreviado para la interfaz (`dd/MM HH:mm`).
    val limaZone = remember { ZoneId.of("America/Lima") }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss") }
    val displayFormatter = remember { DateTimeFormatter.ofPattern("dd/MM HH:mm") }

    // Solicitar permiso de notificaciones (Android 13+). Es necesario para que
    // `ReminderScheduler` pueda mostrar el recordatorio de la reserva.
    RequestNotificationPermission()

    // Estado local del formulario. `remember` lo conserva durante las
    // recomposiciones y se reinicia al salir de la pantalla:
    // - vehículo elegido (null hasta que carga la lista o el usuario elige);
    // - duración en horas, con 2 h como valor inicial;
    // - opción de llegada, con "Inmediato" por defecto;
    // - desplazamiento manual de 180 min (3 h) para la opción personalizada.
    var selectedVehicleId by remember { mutableStateOf<Long?>(null) }
    var durationHours by remember { mutableIntStateOf(2) }
    var arrivalOption by remember { mutableStateOf(ArrivalOption.NOW) }
    var customMinutesOffset by remember { mutableLongStateOf(180L) } // 3h a futuro por defecto si es custom

    // Visibilidad de los tres diálogos. Mantenerlos como estado booleano hace
    // explícita la transición y permite cerrarlos sin perder el formulario.
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Horas para visualización. Se calculan a partir de "ahora" en Lima, se
    // redondean a minutos exactos (sin segundos ni nanosegundos) y se congelan
    // con `remember` hasta que cambie la opción de llegada o el ajuste manual.
    // La duración se suma fuera del `remember`, por lo que el fin sí se
    // actualiza al cambiar las horas. El valor mostrado es una estimación: el
    // diálogo de confirmación y el envío recalculan el inicio con la hora actual
    // para reducir el riesgo de enviar una hora vencida. La comprobación
    // definitiva de fechas pasadas corresponde al backend.
    val plannedStartTime = remember(arrivalOption, customMinutesOffset) {
        val offset = if (arrivalOption == ArrivalOption.CUSTOM) customMinutesOffset else arrivalOption.minutesOffset
        LocalDateTime.now(limaZone).plusMinutes(offset).withSecond(0).withNano(0)
    }
    val plannedEndTime = plannedStartTime.plusHours(durationHours.toLong())

    // Vehículo seleccionado: se busca en la lista cargada por el ViewModel. Si
    // el usuario aún no eligió, puede ser null y el CTA queda deshabilitado.
    val selectedVehicle = vehicleState.vehicles.find { it.id == selectedVehicleId }

    // Búsqueda de tarifa: se intenta una coincidencia flexible entre el nombre
    // del tipo de vehículo y la tarifa del estacionamiento (ignorando mayúsculas
    // y aceptando contenidos parciales). Si no hay coincidencia, se usa la
    // primera tarifa disponible como aproximación. El backend siempre recalcula
    // el precio con su propia configuración; este valor es solo referencial.
    val matchingTariff = parkingState.parkingLot?.tariffs?.firstOrNull { tariff ->
        selectedVehicle != null && (
            tariff.vehicleType.equals(selectedVehicle.vehicleType.name, ignoreCase = true) ||
            tariff.vehicleType.contains(selectedVehicle.vehicleType.name, ignoreCase = true) ||
            selectedVehicle.vehicleType.name.contains(tariff.vehicleType, ignoreCase = true)
        )
    } ?: parkingState.parkingLot?.tariffs?.firstOrNull()

    // Tarifa horaria de respaldo (S/ 5.00) si el lote no tiene tarifas. El total
    // estimado es una multiplicación simple; no incluye fracciones, mínimos ni
    // descuentos que el servidor sí puede aplicar.
    val hourlyRate = matchingTariff?.price ?: 5.0
    val estimatedTotal = hourlyRate * durationHours

    // Carga inicial: detalle del estacionamiento y catálogo de vehículos/tipos.
    // La clave `parkingId` hace que el efecto se repita si la pantalla se
    // reutiliza para otro lote. `VehicleViewModel` también carga en su `init`,
    // pero esta recarga explícita garantiza datos frescos al entrar.
    LaunchedEffect(parkingId) {
        parkingViewModel.loadParkingLot(parkingId)
        vehicleViewModel.loadVehicles()
        vehicleViewModel.loadVehicleTypes()
    }

    // Autoselección: cuando llega la lista y aún no hay vehículo elegido, se
    // toma el primero. Si el usuario ya eligió uno, no se pisa su decisión.
    LaunchedEffect(vehicleState.vehicles) {
        if (selectedVehicleId == null && vehicleState.vehicles.isNotEmpty()) {
            selectedVehicleId = vehicleState.vehicles.first().id
        }
    }

    // Efecto de éxito: cuando el ViewModel marca `reservationCreated`, se
    // programa el recordatorio local con los datos devueltos por el backend y
    // se cambia del diálogo de confirmación al de éxito. La reserva creada es
    // la fuente del id, código, espacio y fechas definitivas.
    LaunchedEffect(reservationState.reservationCreated) {
        if (reservationState.reservationCreated) {
            reservationState.currentReservation?.let { r ->
                ReminderScheduler.schedule(
                    context = context,
                    reservationId = r.id,
                    parkingLotName = r.parkingLotName,
                    startIso = r.startTime,
                    endIso = r.endTime
                )
            }
            showConfirmDialog = false
            showSuccessDialog = true
        }
    }

    // Contenedor con barra superior y una única columna desplazable. No se usa
    // `LazyColumn` porque el formulario tiene una cantidad fija de secciones y
    // se beneficia de mantener todo compuesto mientras el usuario interactúa.
    Scaffold(
        containerColor = extended.background,
        topBar = {
            ParkeoTopBar(
                title = "Nueva reserva",
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
            // Estacionamiento seleccionado: se muestra solo cuando el detalle
            // terminó de cargar. El lote ya viene fijado por `parkingId`; esta
            // tarjeta lo confirma visualmente y muestra disponibilidad agregada.
            // Si el detalle falla, no hay tarjeta ni mensaje de error propio en
            // esta pantalla; el error queda en `parkingState`.
            parkingState.parkingLot?.let { parking ->
                ParkeoCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(Dimens.spacingLg),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(extended.accent.copy(alpha = 0.12f), CircleShape)
                                .border(Dimens.borderHairline, extended.accent.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalParking,
                                contentDescription = null,
                                tint = extended.accent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = parking.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = extended.textPrimary
                            )
                            Text(
                                text = "${parking.address}${parking.district?.let { " • $it" } ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.textSecondary
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = extended.signalGreen.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${parking.availableSpaces} espacios libres",
                                        style = Typography.MonospaceTechnical,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = extended.signalGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Hora de ingreso: chips de llegada rápida y ajuste manual. La hora
            // mostrada se deriva de `plannedStartTime` y usa el formateador de
            // presentación. El texto "HORA DE INGRESO" es una etiqueta de sección,
            // no un campo de formulario libre.
            ParkeoCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(Dimens.spacingLg),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HORA DE INGRESO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = extended.textTertiary
                        )
                        Text(
                            text = plannedStartTime.format(displayFormatter),
                            style = Typography.MonospaceTechnical,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = extended.accent
                        )
                    }

                    // Opciones de llegada: se generan desde `ArrivalOption.values()`
                    // para que agregar un valor al enum no obligue a duplicar UI.
                    // Cada chip ocupa el mismo ancho y muestra título y pista.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ArrivalOption.values().forEach { opt ->
                            val isSelected = arrivalOption == opt
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(ParkeoCardShape)
                                    .clickable { arrivalOption = opt },
                                color = if (isSelected) extended.accent else extended.surface2,
                                shape = ParkeoCardShape,
                                border = androidx.compose.foundation.BorderStroke(
                                    Dimens.borderHairline,
                                    if (isSelected) extended.accent else extended.borderSubtle
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = opt.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) extended.onAccent else extended.textPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = opt.subtitle,
                                        style = Typography.MonospaceTechnical,
                                        fontSize = 10.sp,
                                        color = if (isSelected) extended.onAccent.copy(alpha = 0.8f) else extended.textTertiary
                                    )
                                }
                            }
                        }
                    }

                    // Selector personalizado: solo visible para `CUSTOM`. Ajusta
                    // en pasos de 30 minutos. La resta solo se aplica mientras el
                    // valor supere 15, pero el último paso puede dejarlo en 0;
                    // por eso el cliente no garantiza por sí solo una llegada
                    // futura. El texto se descompone en horas y minutos para que
                    // sea legible.
                    if (arrivalOption == ArrivalOption.CUSTOM) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(extended.surface2, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Llegar en +${customMinutesOffset / 60}h ${customMinutesOffset % 60}m",
                                style = Typography.MonospaceTechnical,
                                fontSize = 12.sp,
                                color = extended.textPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { if (customMinutesOffset > 15) customMinutesOffset -= 30 },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, "-30m", tint = extended.accent)
                                }
                                IconButton(
                                    onClick = { customMinutesOffset += 30 },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Add, "+30m", tint = extended.accent)
                                }
                            }
                        }
                    }
                }
            }

            // Duración de la estadía: opciones fijas de horas. La lista visible
            // no cubre necesariamente el mínimo/máximo del estacionamiento; el
            // backend valida la ventana final. El bloque inferior muestra inicio
            // y salida prevista para que el usuario entienda el intervalo.
            ParkeoCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(Dimens.spacingLg),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                ) {
                    Text(
                        text = "TIEMPO DE ESTADÍA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = extended.textTertiary
                    )

                    // Lista de duraciones ofrecidas. Se recorre en el mismo orden
                    // visual y cada opción ocupa un peso igual. Son valores de UI;
                    // las restricciones reales del estacionamiento las aplica el
                    // backend al validar `startTime`/`endTime`.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3, 4, 8, 12).forEach { hours ->
                            val isSelected = durationHours == hours
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(ParkeoCardShape)
                                    .clickable { durationHours = hours },
                                color = if (isSelected) extended.accent else extended.surface2,
                                shape = ParkeoCardShape,
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
                                        text = "${hours}h",
                                        style = Typography.MonospaceTechnical,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) extended.onAccent else extended.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(extended.surface2, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "INGRESO ESTIMADO",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = extended.textTertiary
                            )
                            Text(
                                text = plannedStartTime.format(displayFormatter),
                                style = Typography.MonospaceTechnical,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = extended.textPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "SALIDA PREVISTA",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = extended.textTertiary
                            )
                            Text(
                                text = plannedEndTime.format(displayFormatter),
                                style = Typography.MonospaceTechnical,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = extended.accent
                            )
                        }
                    }
                }
            }

            // Vehículo: lista de vehículos propios y acceso al alta rápida. El
            // listado viene del backend y no permite elegir vehículos de otros
            // usuarios. Si no hay ninguno, se ofrece registrarlo sin salir del
            // flujo de reserva.
            ParkeoCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(Dimens.spacingLg),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "VEHÍCULO ASOCIADO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = extended.textTertiary
                        )

                        TextButton(
                            onClick = {
                                vehicleViewModel.clearMessages()
                                showAddVehicleDialog = true
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = extended.accent
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Agregar",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = extended.accent
                            )
                        }
                    }

                    // Estado vacío de vehículos: en lugar de dejar el CTA muerto,
                    // se explica la situación y se abre el mismo diálogo de alta.
                    if (vehicleState.vehicles.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = extended.surface2,
                            shape = ParkeoCardShape,
                            border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, extended.borderSubtle)
                        ) {
                            Column(
                                modifier = Modifier.padding(Dimens.spacingMd),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Aún no tienes vehículos registrados",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.textSecondary
                                )
                                Spacer(Modifier.height(8.dp))
                                ParkeoButton(
                                    text = "Registrar vehículo ahora",
                                    onClick = {
                                        vehicleViewModel.clearMessages()
                                        showAddVehicleDialog = true
                                    },
                                    style = ParkeoButtonStyle.Primary,
                                    compact = true
                                )
                            }
                        }
                    } else {
                        // Lista de vehículos: cada fila es seleccionable en toda
                        // su superficie. Se muestra placa en monoespaciada, marca
                        // y modelo opcionales, y el tipo que participa en la
                        // tarifa. El `RadioButton` refleja la misma selección.
                        vehicleState.vehicles.forEach { vehicle ->
                            val isSelected = selectedVehicleId == vehicle.id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(ParkeoCardShape)
                                    .clickable { selectedVehicleId = vehicle.id },
                                color = if (isSelected) extended.accent.copy(alpha = 0.08f) else extended.surface2,
                                shape = ParkeoCardShape,
                                border = androidx.compose.foundation.BorderStroke(
                                    Dimens.borderHairline,
                                    if (isSelected) extended.accent else extended.borderSubtle
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedVehicleId = vehicle.id },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = extended.accent,
                                            unselectedColor = extended.textTertiary
                                        )
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = vehicle.licensePlate,
                                            style = Typography.MonospaceTechnical,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = extended.textPrimary
                                        )
                                        Text(
                                            text = "${vehicle.brand ?: ""} ${vehicle.model ?: ""}".trim().ifBlank { "Sin marca especificada" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = extended.textSecondary
                                        )
                                    }
                                    Surface(
                                        color = extended.surface3,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = vehicle.vehicleType.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = extended.accent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Resumen de precio: el total estimado es informativo y se calcula
            // como tarifa horaria por duración. No se envía al backend. El texto
            // "PAGO AL SALIR" aclara que la reserva no cobra en este paso.
            // Si la tarifa no coincide con el tipo de vehículo, se usa la primera
            // tarifa del lote como aproximación.
            ParkeoCard(
                modifier = Modifier.fillMaxWidth(),
                accentBorder = true
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.spacingLg),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL ESTIMADO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = extended.textTertiary
                            )
                            Text(
                                text = "S/ ${String.format("%.2f", estimatedTotal)}",
                                style = Typography.MonospaceTechnical,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                                color = extended.accent
                            )
                            Text(
                                text = "Tarifa: S/ ${String.format("%.2f", hourlyRate)}/h (${selectedVehicle?.vehicleType?.name ?: "General"})",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = extended.textSecondary
                            )
                        }

                        Surface(
                            color = extended.surface2,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                Dimens.borderHairline,
                                extended.borderSubtle
                            )
                        ) {
                            Text(
                                text = "PAGO AL SALIR",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = extended.textSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Error de creación: se muestra en línea para no perder los datos del
            // formulario. Puede provenir de validaciones del backend (fechas,
            // solapamiento, vehículo, disponibilidad) o de la red. Si el error
            // ocurre con el diálogo de confirmación abierto, queda detrás de él
            // hasta que el usuario lo cierre o reintente.
            reservationState.error?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ParkeoCardShape,
                    color = extended.signalRed.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        Dimens.borderHairline,
                        extended.signalRed.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = extended.signalRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = error,
                            color = extended.signalRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(Dimens.spacingSm))

            // CTA de confirmación: no crea la reserva directamente; abre el
            // diálogo de revisión. Se deshabilita si no hay vehículo seleccionado,
            // si la lista está vacía o si ya hay una operación en curso. Esto es
            // una validación de UI mínima, no la regla de negocio completa.
            ParkeoButton(
                text = "Revisar y confirmar reserva",
                onClick = {
                    showConfirmDialog = true
                },
                isLoading = reservationState.isLoading,
                enabled = selectedVehicleId != null && vehicleState.vehicles.isNotEmpty() && !reservationState.isLoading,
                leadingIcon = Icons.Filled.BookmarkAdd,
                style = ParkeoButtonStyle.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeightLarge)
            )

            Spacer(Modifier.height(Dimens.spacingLg))
        }
    }

    // Diálogo de confirmación previo al envío. Recalcula inicio y fin con la
    // hora actual cada vez que se recompone mientras está abierto: así el
    // resumen coincide con lo que se enviará y se evita arrastrar una hora
    // vieja si el usuario dejó el diálogo abierto. La fecha pasada la rechaza
    // el backend.
    if (showConfirmDialog) {
        val currentStart = if (arrivalOption == ArrivalOption.CUSTOM) {
            LocalDateTime.now(limaZone).plusMinutes(customMinutesOffset).withSecond(0).withNano(0)
        } else {
            LocalDateTime.now(limaZone).plusMinutes(arrivalOption.minutesOffset).withSecond(0).withNano(0)
        }
        val currentEnd = currentStart.plusHours(durationHours.toLong())

        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = extended.surface1,
            title = {
                Text(
                    text = "Confirmar reserva",
                    fontWeight = FontWeight.Bold,
                    color = extended.textPrimary
                )
            },
            text = {
                // Resumen previo: vehículo, ingreso, salida y monto. Es la última
                // oportunidad de revisar antes de que el ViewModel envíe la
                // solicitud; el backend puede corregir el monto final.
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "¿Deseas confirmar tu espacio en ${parkingState.parkingLot?.name ?: "la cochera"}?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = extended.textSecondary
                    )
                    Divider(color = extended.borderSubtle, thickness = 1.dp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vehículo:", color = extended.textTertiary, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "${selectedVehicle?.licensePlate} (${selectedVehicle?.vehicleType?.name})",
                            color = extended.textPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ingreso estimado:", color = extended.textTertiary, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = currentStart.format(displayFormatter),
                            color = extended.textPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Salida estimada:", color = extended.textTertiary, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = currentEnd.format(displayFormatter),
                            color = extended.textPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Monto estimado:", color = extended.textTertiary, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = "S/ ${String.format("%.2f", estimatedTotal)}",
                            color = extended.accent,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                ParkeoButton(
                    text = "Confirmar",
                    onClick = {
                        selectedVehicleId?.let { vId ->
                            // Cálculo exacto al confirmar: se vuelve a leer el
                            // reloj para reflejar la hora actual aunque el
                            // usuario haya dejado el diálogo abierto. Se eliminan
                            // segundos y nanosegundos para respetar el formato
                            // ISO esperado por el backend. El servidor sigue
                            // siendo quien rechaza una fecha pasada.
                            val freshStart = if (arrivalOption == ArrivalOption.CUSTOM) {
                                LocalDateTime.now(limaZone).plusMinutes(customMinutesOffset).withSecond(0).withNano(0)
                            } else {
                                LocalDateTime.now(limaZone).plusMinutes(arrivalOption.minutesOffset).withSecond(0).withNano(0)
                            }
                            val freshEnd = freshStart.plusHours(durationHours.toLong())

                            reservationViewModel.createReservation(
                                parkingLotId = parkingId,
                                vehicleId = vId,
                                startTime = formatter.format(freshStart),
                                endTime = formatter.format(freshEnd)
                            )
                        }
                    },
                    isLoading = reservationState.isLoading,
                    style = ParkeoButtonStyle.Primary
                )
            },
            dismissButton = {
                ParkeoButton(
                    text = "Atrás",
                    onClick = { showConfirmDialog = false },
                    style = ParkeoButtonStyle.Ghost
                )
            }
        )
    }

    // Diálogo de éxito: muestra el código y el espacio asignado por el backend.
    // El cierre por toque fuera y el botón "Ver mis reservas" limpian los
    // mensajes del ViewModel y navegan con `onReservationCreated`; así no se
    // reabre el diálogo al volver a componer la pantalla. El botón secundario
    // "Cómo llegar" abre mapas sin cerrar el diálogo.
    if (showSuccessDialog) {
        val reservation = reservationState.currentReservation
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                reservationViewModel.clearMessages()
                onReservationCreated()
            },
            containerColor = extended.surface1,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = extended.accent,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "¡Reserva Confirmada!",
                        fontWeight = FontWeight.Black,
                        color = extended.textPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Tu lugar está reservado en ${parkingState.parkingLot?.name ?: "la cochera"}.",
                        color = extended.textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Código de reserva destacado: se usa el código del backend;
                    // si no llegó, se muestra un respaldo local con el id para no
                    // dejar el espacio vacío. El espacio asignado solo aparece si
                    // el servidor lo devolvió y no está en blanco.
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = extended.surface2,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, extended.accent.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "CÓDIGO DE RESERVA",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                letterSpacing = 1.sp,
                                color = extended.textTertiary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = reservation?.confirmationCode ?: "PKO-${reservation?.id ?: "OK"}",
                                style = Typography.MonospaceTechnical,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = extended.accent
                            )
                            reservation?.spaceNumber?.let { space ->
                                if (space.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Espacio asignado: $space",
                                        style = Typography.MonospaceTechnical,
                                        fontSize = 12.sp,
                                        color = extended.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Muestra este código al operador cuando llegues a la cochera.",
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.textTertiary
                    )
                }
            },
            confirmButton = {
                ParkeoButton(
                    text = "Ver mis reservas",
                    onClick = {
                        showSuccessDialog = false
                        reservationViewModel.clearMessages()
                        onReservationCreated()
                    },
                    style = ParkeoButtonStyle.Primary
                )
            },
            dismissButton = {
                // Acción secundaria opcional: abre la ubicación del lote en una
                // app de mapas mediante un intent `geo:`. Solo se muestra cuando
                // el detalle del estacionamiento ya está cargado.
                parkingState.parkingLot?.let { lot ->
                    ParkeoButton(
                        text = "Cómo llegar",
                        leadingIcon = Icons.Filled.Directions,
                        onClick = {
                            val uri = Uri.parse("geo:${lot.latitude},${lot.longitude}?q=${Uri.encode(lot.name)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(mapIntent)
                        },
                        style = ParkeoButtonStyle.Secondary
                    )
                }
            }
        )
    }

    // Diálogo de alta rápida de vehículo. Recibe el catálogo de tipos y el estado
    // de carga/error del `VehicleViewModel`; al guardar, el ViewModel recarga la
    // lista. Si no había un vehículo seleccionado, la autoselección tomará el
    // primero de la lista recargada; si ya había uno, se respeta esa elección.
    if (showAddVehicleDialog) {
        InlineAddVehicleDialog(
            vehicleTypes = vehicleState.vehicleTypes,
            isLoading = vehicleState.isLoading,
            errorMessage = vehicleState.error,
            onDismiss = {
                vehicleViewModel.clearMessages()
                showAddVehicleDialog = false
            },
            onAdd = { plate, brand, model, color, typeId ->
                vehicleViewModel.addVehicle(plate, brand, model, color, typeId)
                showAddVehicleDialog = false
            }
        )
    }
}

/**
 * Diálogo para registrar un vehículo sin abandonar la creación de la reserva.
 *
 * Mantiene un formulario local con placa, marca, modelo y tipo. La placa se
 * normaliza a mayúsculas, se limpia de caracteres no alfanuméricos y se agrupa
 * como `ABC-123`. El tipo se elige de un desplegable alimentado por el backend y
 * se preselecciona el primero disponible. El botón "Guardar" solo se habilita
 * cuando el texto formateado de la placa tiene al menos seis caracteres
 * (incluido el guion), hay un tipo seleccionado y no hay una operación en curso.
 *
 * Nota: existe un estado `color` porque el callback lo admite, pero en esta
 * versión no se renderiza un campo para editarlo; al guardar siempre se envía
 * `null`. Se documenta para no confundirlo con un olvido de la UI.
 *
 * @param vehicleTypes Tipos de vehículo disponibles, cargados por
 *   `VehicleViewModel`.
 * @param isLoading Indica si el alta está en curso; deshabilita el botón.
 * @param errorMessage Error del ViewModel que se muestra dentro del diálogo.
 * @param onDismiss Callback para cerrar y limpiar mensajes.
 * @param onAdd Callback con placa, marca, modelo, color y tipo elegido. Marca,
 *   modelo y color se envían como `null` si están vacíos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InlineAddVehicleDialog(
    vehicleTypes: List<VehicleTypeDto>,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onAdd: (String, String?, String?, String?, Long) -> Unit
) {
    val extended = ParkeoTheme.colors

    // Campos controlados del formulario. `remember` conserva lo escrito mientras
    // el diálogo permanece compuesto; al cerrarlo se descarta el estado.
    var plate by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }

    // Tipo preseleccionado: se inicializa con el primero del catálogo y se
    // reinicia si cambia la lista de tipos.
    var selectedTypeId by remember(vehicleTypes) {
        mutableStateOf<Long?>(vehicleTypes.firstOrNull()?.id)
    }
    // Controla la expansión del menú desplegable de tipos.
    var expanded by remember { mutableStateOf(false) }

    // Salvaguarda para listas que llegan después de la primera composición.
    LaunchedEffect(vehicleTypes) {
        if (selectedTypeId == null && vehicleTypes.isNotEmpty()) {
            selectedTypeId = vehicleTypes.first().id
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extended.surface1,
        title = {
            Text(
                text = "Registrar vehículo",
                fontWeight = FontWeight.Bold,
                color = extended.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
            ) {
                // Error del alta: se muestra dentro del diálogo para que el
                // usuario corrija sin perder el contexto del formulario.
                errorMessage?.let { errorText ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ParkeoCardShape,
                        color = extended.signalRed.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            Dimens.borderHairline,
                            extended.signalRed.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodySmall,
                            color = extended.signalRed,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Placa: se normaliza mientras se escribe. Se eliminan guiones y
                // símbolos, se pasa a mayúsculas y se inserta un guion después de
                // los tres primeros caracteres (formato peruano `ABC-123`). El
                // límite efectivo es de seis caracteres alfanuméricos.
                ParkeoTextField(
                    value = plate,
                    onValueChange = { input ->
                        val clean = input.uppercase().replace(Regex("[^A-Z0-9]"), "")
                        plate = if (clean.length > 3) "${clean.take(3)}-${clean.drop(3).take(3)}" else clean.take(6)
                    },
                    label = "Placa *",
                    placeholder = "ABC-123",
                    modifier = Modifier.fillMaxWidth()
                )

                ParkeoTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = "Marca",
                    placeholder = "Ej. Toyota",
                    modifier = Modifier.fillMaxWidth()
                )

                ParkeoTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = "Modelo",
                    placeholder = "Ej. Yaris",
                    modifier = Modifier.fillMaxWidth()
                )

                // Tipo de vehículo: campo de solo lectura que abre un menú
                // expuesto. Se usa `menuAnchor()` para asociar el ancla del menú
                // con el campo y `TrailingIcon` para indicar que es desplegable.
                // El tipo determina qué tarifa del lote puede aplicar.
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    ParkeoTextField(
                        value = vehicleTypes.firstOrNull { it.id == selectedTypeId }?.name ?: "Seleccionar tipo",
                        onValueChange = {},
                        readOnly = true,
                        label = "Tipo de vehículo",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        vehicleTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedTypeId = type.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            // Validación local mínima: al menos seis caracteres en el texto
            // formateado (incluido el guion), tipo elegido y sin carga en curso.
            // La unicidad de la placa, la pertenencia y el formato definitivo los
            // valida el backend.
            ParkeoButton(
                text = "Guardar",
                isLoading = isLoading,
                onClick = {
                    selectedTypeId?.let { typeId ->
                        onAdd(
                            plate.trim(),
                            brand.trim().ifBlank { null },
                            model.trim().ifBlank { null },
                            color.trim().ifBlank { null },
                            typeId
                        )
                    }
                },
                enabled = plate.length >= 6 && selectedTypeId != null && !isLoading,
                style = ParkeoButtonStyle.Primary
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