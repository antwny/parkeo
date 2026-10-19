/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/ui/screens/parking/ParkingDetailScreen.kt
 * Capa: presentación (Jetpack Compose).
 *
 * Propósito:
 * Mostrar el detalle de un estacionamiento seleccionado: disponibilidad agregada,
 * estado abierto/cerrado, ubicación, contacto, tarifas y servicios. Desde la barra
 * inferior se puede volver o iniciar el flujo de reserva.
 *
 * Conceptos clave:
 * - Compose declarativo y estado derivado del `StateFlow` del ViewModel.
 * - `collectAsState`: observa `ParkingDetailUiState` y dispara recomposición.
 * - `LaunchedEffect(parkingId)`: efecto de carga inicial y recarga cuando cambia el id.
 * - `Scaffold` con `topBar` y `bottomBar`; `ParkeoTopBar` encapsula el `TopAppBar`.
 * - `Column` con `verticalScroll(rememberScrollState())`: el detalle es desplazable,
 *   pero no usa `LazyColumn` porque su contenido es fijo y de tamaño moderado.
 * - `Modifier`: cadena inmutable que declara tamaño, relleno, scroll, alineación y
 *   demás propiedades visuales de cada elemento.
 * - `LinearProgressIndicator` para representar la ocupación calculada.
 * - Navegación por callbacks (`onNavigateBack`, `onNavigateToReservation`).
 *
 * Flujo de usuario y datos:
 * 1. `parkingId` llega como argumento desde el grafo de navegación.
 * 2. Al entrar (o cambiar el id) se llama a `viewModel.loadParkingLot(parkingId)`.
 * 3. El ViewModel consulta el repositorio y publica carga, error o el DTO detallado.
 * 4. La pantalla muestra una de esas tres ramas y, si hay datos, habilita "Reservar
 *    espacio" solo cuando el lote está abierto y tiene espacios disponibles.
 * 5. El botón de reserva navega con el id; la selección de espacio, fecha y vehículo
 *    ocurre en la pantalla de creación de reserva, no aquí.
 *
 * Disponibilidad y horarios:
 * - No se cargan espacios individuales: `availableSpaces` y `totalCapacity` son
 *   agregados que envía el backend. La pantalla calcula el porcentaje de ocupación.
 * - El DTO incluye `schedules`, pero esta versión no dibuja una sección de horarios;
 *   el estado abierto/cerrado se toma directamente de `parking.isOpen`.
 * - El DTO incluye `description` e `imageUrl`, que tampoco se muestran actualmente.
 *
 * Relación con otras capas:
 * - `ParkingDetailViewModel` expone `ParkingDetailUiState` y `loadParkingLot`.
 * - `ParkingLotDetailDto`, `TariffDto` y `ParkingServiceDto` son el contrato de red.
 * - `ParkeoCard`, `ParkeoBadge`, `ParkeoButton`, estados y `DirectionsButton` son
 *   componentes reutilizables de `ui/components` y `util`.
 *
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.ui.screens.parking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.ParkingDetailViewModel
import pe.parkeo.util.DirectionsButton

/**
 * Pantalla de detalle de un estacionamiento.
 *
 * Observa el estado del ViewModel y presenta:
 * - una tarjeta principal con disponibilidad y ocupación;
 * - ubicación, ruta y teléfono;
 * - tabla de tarifas por tipo de vehículo;
 * - servicios incluidos.
 *
 * La barra inferior solo aparece cuando el detalle ya se cargó. El botón de reserva
 * se deshabilita si el estacionamiento está cerrado o no tiene espacios disponibles.
 *
 * @param parkingId identificador recibido desde la navegación. Es la clave del
 *   `LaunchedEffect` y el argumento de `viewModel.loadParkingLot`.
 * @param viewModel fuente del detalle y de las acciones de recarga.
 * @param onNavigateBack callback para volver atrás (barra superior y botón inferior).
 * @param onNavigateToReservation callback que recibe el id del estacionamiento para
 *   abrir la pantalla de creación de reserva.
 */
@Composable
fun ParkingDetailScreen(
    parkingId: Long,
    viewModel: ParkingDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReservation: (Long) -> Unit
) {
    // Estado observable del detalle. `ParkingDetailUiState` agrupa `isLoading`,
    // `parkingLot` y `error`. `collectAsState` integra el `StateFlow` con el ciclo de
    // vida de la composición y provoca recomposición ante cada emisión.
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors

    // Efecto de carga. La clave `parkingId` hace que, si el grafo reutiliza la misma
    // composición con otro id, se vuelva a pedir el detalle correcto. No se lanza desde
    // el cuerpo de la función para evitar peticiones en cada recomposición.
    LaunchedEffect(parkingId) {
        viewModel.loadParkingLot(parkingId)
    }

    // `Scaffold` organiza barra superior, contenido y barra inferior. El padding que
    // entrega se aplica a cada rama para que ninguna quede debajo de las barras.
    Scaffold(
        containerColor = extended.background,
        topBar = {
            // El título es dinámico: usa el nombre cuando ya llegó el DTO y un texto
            // genérico mientras carga o si hay error. La flecha vuelve atrás.
            ParkeoTopBar(
                title = uiState.parkingLot?.name ?: "Detalle de cochera",
                onNavigationClick = onNavigateBack
            )
        },
        bottomBar = {
            // La barra inferior solo existe con datos cargados; así no se muestra un
            // botón de reserva sin id válido. `navigationBarsPadding` evita que los
            // botones queden bajo la barra de gestos del sistema.
            uiState.parkingLot?.let { parking ->
                Surface(
                    color = extended.surface1,
                    border = androidx.compose.foundation.BorderStroke(
                        Dimens.borderHairline,
                        extended.borderSubtle
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(Dimens.spacingMd),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Acción secundaria: vuelve a la pantalla anterior. Duplica la
                        // flecha de la barra superior para reforzar la salida en pantallas
                        // largas con scroll.
                        ParkeoButton(
                            text = "Regresar",
                            onClick = onNavigateBack,
                            leadingIcon = Icons.Filled.ArrowBack,
                            style = ParkeoButtonStyle.Secondary,
                            modifier = Modifier
                                .weight(0.9f)
                                .height(Dimens.buttonHeightDefault)
                        )

                        // Acción principal: solo habilitada si el lote está abierto y
                        // tiene al menos un espacio disponible. La validación definitiva
                        // y el cálculo del precio corresponden al backend.
                        ParkeoButton(
                            text = "Reservar espacio",
                            onClick = { onNavigateToReservation(parking.id) },
                            leadingIcon = Icons.Filled.BookmarkAdd,
                            enabled = parking.isOpen && parking.availableSpaces > 0,
                            style = ParkeoButtonStyle.Primary,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(Dimens.buttonHeightDefault)
                        )
                    }
                }
            }
        }
    ) { padding ->
        // Prioridad de estados: carga, error y contenido. Mientras se recarga, la
        // pantalla oculta el detalle anterior y muestra el indicador. No hay `Snackbar`
        // ni `AlertDialog` en esta pantalla; los fallos se resuelven con reintento.
        when {
            // Estado de carga inicial o recarga.
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    ParkeoLoadingView(message = "Cargando detalles...")
                }
            }

            // Estado de error. El botón vuelve a pedir el mismo `parkingId`.
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    ParkeoErrorView(
                        message = uiState.error ?: "Error al cargar la información",
                        onRetry = { viewModel.loadParkingLot(parkingId) }
                    )
                }
            }

            // Contenido. La comprobación de nulidad seguida de `!!` es necesaria porque
            // `parkingLot` es una propiedad de un `StateFlow` (no una variable local) y
            // el compilador no puede hacer smart cast dentro del lambda. Se copia a
            // `parking` para usarla de forma segura en todo el bloque.
            uiState.parkingLot != null -> {
                val parking = uiState.parkingLot!!
                // Columna desplazable verticalmente. `rememberScrollState` conserva la
                // posición de scroll entre recomposiciones; internamente usa el mecanismo
                // de `rememberSaveable`, por lo que también puede restaurarla tras una
                // recreación de la Activity.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(Dimens.spacingMd),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                ) {
                    // Sección: tarjeta principal de disponibilidad. El borde de acento
                    // se activa cuando hay al menos un espacio libre.
                    ParkeoCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentBorder = parking.availableSpaces > 0
                    ) {
                        Column(
                            modifier = Modifier.padding(Dimens.spacingLg),
                            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                        ) {
                            // Encabezado: cantidad de espacios libres, capacidad total
                            // y badge de estado. El número cambia a rojo cuando no hay
                            // disponibilidad; el badge prioriza "cerrado" sobre "ocupado".
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "DISPONIBILIDAD EN TIEMPO REAL",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = extended.textTertiary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "${parking.availableSpaces}",
                                            style = Typography.MonospaceTechnical,
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (parking.availableSpaces > 0) extended.accent else extended.signalRed
                                        )
                                        Text(
                                            text = " / ${parking.totalCapacity} espacios",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = extended.textSecondary,
                                            modifier = Modifier.padding(bottom = 6.dp, start = 6.dp)
                                        )
                                    }
                                }

                                // `ParkeoBadgeStatus` es un alias de `BadgeVariant`.
                                // Se elige según el mismo criterio de precedencia.
                                ParkeoBadge(
                                    status = if (!parking.isOpen)
                                        ParkeoBadgeStatus.Closed
                                    else if (parking.availableSpaces > 0)
                                        ParkeoBadgeStatus.Available
                                    else
                                        ParkeoBadgeStatus.Occupied
                                )
                            }

                            // Sección: barra de ocupación calculada localmente.
                            // `occupancyRatio = 1 - disponibles / capacidad`. Si la
                            // capacidad es cero o negativa se usa 0f para evitar una
                            // división por cero. No se limita el rango, por lo que un
                            // dato inconsistente del backend podría salir de 0..1.
                            val occupancyRatio = if (parking.totalCapacity > 0) {
                                1f - (parking.availableSpaces.toFloat() / parking.totalCapacity)
                            } else 0f

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                // La sobrecarga con lambda `progress = { ... }` permite
                                // que Material 3 anime el avance de forma eficiente.
                                LinearProgressIndicator(
                                    progress = { occupancyRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp),
                                    color = if (parking.availableSpaces > 0) extended.accent else extended.signalRed,
                                    trackColor = extended.surface3
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // `toInt()` trunca los decimales; el porcentaje es
                                    // solo informativo. El estado textual usa `isOpen`.
                                    Text(
                                        text = "Ocupación: ${(occupancyRatio * 100).toInt()}%",
                                        style = Typography.MonospaceTechnical,
                                        fontSize = 11.sp,
                                        color = extended.textTertiary
                                    )
                                    Text(
                                        text = if (parking.isOpen) "Abierto" else "Cerrado temporalmente",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (parking.isOpen) extended.signalGreen else extended.signalRed
                                    )
                                }
                            }
                        }
                    }

                    // Sección: ubicación y contacto. La dirección es obligatoria en el
                    // DTO; el distrito es opcional y se completa con ", Lima". El botón
                    // de ruta abre Google Maps con las coordenadas.
                    ParkeoCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(Dimens.spacingLg),
                            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
                        ) {
                            Text(
                                text = "UBICACIÓN Y CONTACTO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = extended.textTertiary
                            )

                            // Dirección principal con icono de ubicación.
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = extended.accent,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = parking.address,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = extended.textPrimary
                                    )
                                    parking.district?.let {
                                        Text(
                                            text = "$it, Lima",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = extended.textSecondary
                                        )
                                    }
                                }
                            }

                            // Ruta hasta la cochera (Google Maps). En el DTO actual
                            // `latitude` y `longitude` son `Double` no nulos con valor
                            // predeterminado 0.0, por lo que esta comprobación de nulidad
                            // siempre es verdadera; se conserva tal cual. Un (0, 0) podría
                            // abrir una ruta incorrecta si el backend no enviara coordenadas.
                            val lat = parking.latitude
                            val lng = parking.longitude
                            if (lat != null && lng != null) {
                                DirectionsButton(latitude = lat, longitude = lng)
                            }

                            // Teléfono opcional: si no existe, la fila se omite.
                            parking.phone?.let { phone ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Phone,
                                        contentDescription = null,
                                        tint = extended.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = phone,
                                        style = Typography.MonospaceTechnical,
                                        fontSize = 13.sp,
                                        color = extended.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Sección: tabla de tarifas. Solo se dibuja si el backend envió al
                    // menos una tarifa. Cada fila muestra tipo de vehículo, tipo de tarifa
                    // y precio. No se muestran horarios ni la duración mínima.
                    if (parking.tariffs.isNotEmpty()) {
                        ParkeoCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(Dimens.spacingLg),
                                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                            ) {
                                Text(
                                    text = "TABLA DE TARIFAS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = extended.textTertiary
                                )

                                Spacer(Modifier.height(4.dp))

                                // Recorrido simple con `forEach`: la lista es corta y no
                                // necesita virtualización. El icono se elige por coincidencia
                                // textual en el nombre del tipo; si no coincide, usa auto.
                                parking.tariffs.forEach { tariff ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = when {
                                                    tariff.vehicleType.contains("MOTO", ignoreCase = true) -> Icons.Filled.TwoWheeler
                                                    tariff.vehicleType.contains("CAMION", ignoreCase = true) -> Icons.Filled.LocalShipping
                                                    else -> Icons.Filled.DirectionsCar
                                                },
                                                contentDescription = null,
                                                tint = extended.textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "${tariff.vehicleType} • ${tariff.tariffType}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = extended.textPrimary
                                            )
                                        }

                                        // Precio formateado con dos decimales y prefijo
                                        // "S/". `String.format` usa la configuración
                                        // regional del dispositivo para el separador.
                                        Text(
                                            text = "S/ ${String.format("%.2f", tariff.price)}",
                                            style = Typography.MonospaceTechnical,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = extended.accent
                                        )
                                    }
                                    // Separador después de cada tarifa, incluida la última.
                                    HorizontalDivider(
                                        color = extended.borderSubtle,
                                        thickness = Dimens.borderHairline
                                    )
                                }
                            }
                        }
                    }

                    // Sección: servicios incluidos. También es opcional; cada servicio
                    // se representa con un check y su nombre. No se usa el campo `icon`
                    // del DTO en esta versión.
                    if (parking.services.isNotEmpty()) {
                        ParkeoCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(Dimens.spacingLg),
                                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                            ) {
                                Text(
                                    text = "SERVICIOS INCLUIDOS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = extended.textTertiary
                                )

                                Spacer(Modifier.height(4.dp))

                                parking.services.forEach { service ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = extended.accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = service.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = extended.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Espacio final para que el último contenido no quede pegado a la
                    // barra inferior ni al borde de la pantalla.
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}