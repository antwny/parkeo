/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/ui/screens/home/HomeScreen.kt
 * Capa: presentación (Jetpack Compose).
 *
 * Propósito:
 * Pantalla principal de descubrimiento. Muestra un mapa interactivo o una lista de
 * estacionamientos, permite buscar por nombre o distrito, aplicar filtros rápidos y
 * abrir el detalle para reservar. El estado de negocio vive en `HomeViewModel`; esta
 * pantalla solo presenta y delega las acciones.
 *
 * Conceptos clave:
 * - Compose declarativo: la interfaz es una función del estado observado.
 * - State hoisting: los eventos suben mediante callbacks (`onNavigateTo...`) y las
 *   acciones de datos bajan al `ViewModel`.
 * - `collectAsState`: convierte el `StateFlow` del ViewModel en estado Compose.
 * - `remember`: conserva estado local entre recomposiciones. No sobrevive a la
 *   recreación de la Activity; para eso existiría `rememberSaveable`, que aquí no se
 *   usa. Por eso la búsqueda, el modo de vista y el filtro se reinician si el sistema
 *   recrea la pantalla.
 * - `LaunchedEffect`: ejecuta un efecto suspendido cuando cambia su clave.
 * - `LazyColumn` y `LazyRow`: listas que componen únicamente los elementos visibles.
 * - `Scaffold` y `ParkeoTopBar` (esta última encapsula un `TopAppBar`).
 * - `Modifier`: cadena inmutable que declara tamaño, relleno, alineación, fondo,
 *   interacción y otras propiedades visuales de cada elemento.
 * - Google Maps Compose: `GoogleMap`, `Marker`, `CameraPositionState` y utilidades de
 *   cámara.
 * - Permisos de ubicación en ejecución y API de ubicación fusionada de Google Play
 *   Services.
 * - Búsqueda, filtros y disponibilidad derivada de los DTO.
 *
 * Flujo de usuario y datos:
 * 1. El `HomeViewModel` ya cargó los estacionamientos al construirse.
 * 2. La pantalla solicita permiso de ubicación una vez; si se concede, intenta leer la
 *    última ubicación conocida y pide al ViewModel cocheras cercanas.
 * 3. El texto de búsqueda se envía al ViewModel, que consulta el backend.
 * 4. Los chips de filtro se aplican localmente sobre `uiState.parkingLots`.
 * 5. El mapa colorea los marcadores según disponibilidad; al pulsar uno se muestra
 *    una tarjeta inferior. Esa tarjeta y cada `ParkingCard` llaman a
 *    `onNavigateToParkingDetail(id)`.
 * 6. El icono de marcadores navega a reservas y el icono de cuenta al perfil.
 *
 * Paginación y refresco:
 * Esta versión no implementa paginación incremental ni gesto de "pull to refresh".
 * La lista completa llega en `uiState.parkingLots`; el botón de reintento del estado
 * de error vuelve a invocar `loadParkingLots()`. La búsqueda y los filtros tampoco
 * paginan: trabajan sobre la respuesta completa del backend.
 *
 * Relación con otras capas:
 * - `HomeViewModel` expone `HomeUiState` y las acciones `loadParkingLots`,
 *   `loadNearbyParkingLots` y `search`.
 * - `ParkingLotDto` es el contrato de red con el backend.
 * - `ParkingCard`, `ParkeoTextField`, `ParkeoBadge`, botones y estados viven en
 *   `ui/components` y mantienen la consistencia visual.
 * - La navegación se inyecta como callbacks desde `ParkeoNavGraph`.
 *
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.ui.screens.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import pe.parkeo.data.remote.dto.ParkingLotDto
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.HomeViewModel

/**
 * Modos de presentación del contenido principal.
 *
 * `MAP` muestra el mapa interactivo con marcadores; `LIST` muestra una `LazyColumn`
 * con tarjetas. Es estado local de interfaz y no se persiste ni se envía al backend.
 */
enum class HomeViewMode {
    MAP, LIST
}

/**
 * Filtros rápidos disponibles sobre la lista de estacionamientos ya cargada.
 *
 * @property label texto visible en el chip correspondiente.
 *
 * - `ALL`: no filtra, conserva todos los elementos.
 * - `OPEN`: conserva los que tienen `isOpen == true`.
 * - `AVAILABLE`: conserva los que tienen `availableSpaces > 0`.
 * - `HIGH_CAPACITY`: conserva los que tienen al menos cinco espacios disponibles.
 *
 * El filtrado ocurre en memoria; no genera una nueva petición HTTP.
 */
enum class HomeFilter(val label: String) {
    ALL("Todos"),
    OPEN("Abiertos"),
    AVAILABLE("Con espacio"),
    HIGH_CAPACITY("Alta capacidad")
}

/**
 * Pantalla de inicio para descubrir estacionamientos.
 *
 * Presenta un encabezado con búsqueda, selector mapa/lista y filtros rápidos. El
 * contenido principal alterna entre `GoogleMap` y una lista de `ParkingCard`. Los
 * estados de carga, error y vacío solo se dibujan en modo lista; el mapa permanece
 * visible y puede quedar sin marcadores mientras los datos se actualizan.
 *
 * @param viewModel fuente de verdad: `StateFlow<HomeUiState>` y acciones de datos.
 * @param onNavigateToParkingDetail callback con el identificador del estacionamiento;
 *   lo usan la tarjeta de vista previa del mapa y cada tarjeta de la lista.
 * @param onNavigateToProfile callback para abrir el perfil desde la barra superior.
 * @param onNavigateToReservations callback para abrir "Mis reservas".
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToParkingDetail: (Long) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToReservations: () -> Unit
) {
    // Observa el `StateFlow` del ViewModel. `HomeUiState` agrupa `isLoading`,
    // `parkingLots`, `error` y `searchQuery`. Cada emisión provoca una recomposición de
    // las partes que leen `uiState`; no se copia el estado a variables mutables, de modo
    // que el ViewModel sigue siendo la única fuente de verdad.
    val uiState by viewModel.uiState.collectAsState()

    // Paleta extendida del tema (colores semánticos) y accesos al contexto Android.
    val extended = ParkeoTheme.colors
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // Scope ligado al ciclo de vida de la composición para lanzar animaciones de cámara
    // desde callbacks no suspendidos (por ejemplo, un clic en un marcador).
    val coroutineScope = rememberCoroutineScope()

    // Coordenadas iniciales de respaldo: Miraflores, Lima (-12.1215, -77.0298).
    // Se usan cuando aún no hay ubicación del usuario o el permiso fue denegado.
    val defaultLocation = LatLng(-12.1215, -77.0298)

    // `rememberCameraPositionState` conserva la cámara del mapa entre recomposiciones.
    // La posición inicial se fija una sola vez con un zoom de 13.5f.
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 13.5f)
    }

    // Estado local de la pantalla. `remember` evita reiniciarlo en cada recomposición,
    // pero no lo conserva si Android recrea la Activity. `rememberSaveable` sería la
    // alternativa si se quisiera sobrevivir a rotaciones o muerte del proceso.
    var locationPermissionGranted by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf(HomeViewMode.MAP) }
    var selectedLot by remember { mutableStateOf<ParkingLotDto?>(null) }
    var activeFilter by remember { mutableStateOf(HomeFilter.ALL) }

    // Lista derivada que aplica el filtro activo. `remember` con claves vuelve a calcular
    // el resultado solo cuando cambian `uiState.parkingLots` o `activeFilter`. Es una
    // alternativa sencilla a `derivedStateOf`, que se usaría si el cálculo dependiera de
    // lecturas de estado que cambian con frecuencia y se quisiera memoizar la derivación.
    // El umbral de "alta capacidad" es una decisión de presentación: al menos 5 libres.
    val displayedLots = remember(uiState.parkingLots, activeFilter) {
        when (activeFilter) {
            HomeFilter.ALL -> uiState.parkingLots
            HomeFilter.OPEN -> uiState.parkingLots.filter { it.isOpen }
            HomeFilter.AVAILABLE -> uiState.parkingLots.filter { it.availableSpaces > 0 }
            HomeFilter.HIGH_CAPACITY -> uiState.parkingLots.filter { it.availableSpaces >= 5 }
        }
    }

    // Lanzador de permisos de ubicación. Android puede conceder precisión fina, solo
    // aproximada o ninguna; se acepta cualquiera de las dos primeras. El resultado se
    // guarda en estado local para habilitar la capa "mi ubicación" del mapa.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationPermissionGranted) {
            // `lastLocation` puede ser `null` si el dispositivo nunca tuvo una lectura;
            // en ese caso no se mueve la cámara ni se consultan cocheras cercanas. El
            // `try/catch` de `SecurityException` cubre una revocación del permiso entre
            // la comprobación y la llamada; en ese caso se conserva la vista por defecto.
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val userLatLng = LatLng(it.latitude, it.longitude)
                        coroutineScope.launch {
                            // Animación asíncrona de la cámara; no bloquea el hilo de UI.
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 14.5f))
                        }
                        // Se delega la consulta al ViewModel, que reemplaza la lista con
                        // los estacionamientos cercanos devueltos por el backend.
                        viewModel.loadNearbyParkingLots(it.latitude, it.longitude)
                    }
                }
            } catch (_: SecurityException) { }
        }
    }

    // `LaunchedEffect(Unit)` se ejecuta una vez al entrar en composición (y de nuevo si
    // la pantalla se recrea). Solicita ambos permisos; si ya estaban concedidos, el
    // sistema responde sin mostrar diálogo. No hay `Snackbar` aquí: los errores de datos
    // se muestran con `ParkeoErrorView` en modo lista.
    LaunchedEffect(Unit) {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // `Scaffold` aporta la estructura Material 3: barra superior y padding de contenido.
    // Se anulan los insets de ventana porque la barra y el mapa gestionan su propio
    // espacio; el padding calculado se aplica a la columna interior.
    Scaffold(
        containerColor = extended.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            // Barra superior de marca. Los iconos de acción son state hoisting puro:
            // notifican al host de navegación mediante los callbacks recibidos.
            ParkeoTopBar(
                title = "Parkeo",
                isBrandTitle = true,
                actions = {
                    IconButton(onClick = onNavigateToReservations) {
                        Icon(
                            imageVector = Icons.Filled.BookmarkBorder,
                            contentDescription = "Mis reservas",
                            tint = extended.textSecondary
                        )
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Perfil",
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
            // Encabezado de búsqueda y filtros. Se mantiene fijo sobre el contenido
            // para que estas acciones estén siempre accesibles, tanto en mapa como en
            // lista. Usa el color de superficie 1 para separarse visualmente.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(extended.surface1)
                    .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSm),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
            ) {
                // Barra de búsqueda. Cada pulsación actualiza el estado local y delega
                // en `viewModel.search`, que consulta el backend. No se aplica `debounce`
                // aquí: la frecuencia de peticiones queda determinada por el ViewModel.
                ParkeoTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.search(it)
                    },
                    placeholder = "Buscar cochera por nombre o distrito...",
                    leadingIcon = Icons.Filled.Search,
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = {
                                // Limpiar restaura la lista completa pidiéndola de nuevo
                                // al ViewModel, en lugar de filtrar solo en memoria.
                                searchQuery = ""
                                viewModel.search("")
                            }) {
                                Icon(Icons.Filled.Close, "Limpiar búsqueda", tint = extended.textTertiary)
                            }
                        }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )

                // Selector segmentado mapa/lista. Son dos `Surface` clicables con el mismo
                // peso para simular un control segmentado. Al cambiar de modo se quita el
                // foco del buscador para cerrar el teclado.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Opción "Mapa interactivo": resaltada con el color de acento cuando
                    // `viewMode` es `MAP`. El borde y el fondo cambian según selección.
                    val isMapSelected = viewMode == HomeViewMode.MAP
                    Surface(
                        onClick = {
                            focusManager.clearFocus()
                            viewMode = HomeViewMode.MAP
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        color = if (isMapSelected) extended.accent.copy(alpha = 0.15f) else extended.surface2,
                        shape = ParkeoPillShape,
                        border = BorderStroke(
                            Dimens.borderHairline,
                            if (isMapSelected) extended.accent else extended.borderSubtle
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Map,
                                contentDescription = null,
                                tint = if (isMapSelected) extended.accent else extended.textTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Mapa interactivo",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isMapSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isMapSelected) extended.accent else extended.textSecondary
                            )
                        }
                    }

                    // Opción "Lista": muestra entre paréntesis la cantidad de elementos
                    // después de aplicar el filtro activo, no el total del backend.
                    val isListSelected = viewMode == HomeViewMode.LIST
                    Surface(
                        onClick = {
                            focusManager.clearFocus()
                            viewMode = HomeViewMode.LIST
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        color = if (isListSelected) extended.accent.copy(alpha = 0.15f) else extended.surface2,
                        shape = ParkeoPillShape,
                        border = BorderStroke(
                            Dimens.borderHairline,
                            if (isListSelected) extended.accent else extended.borderSubtle
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FormatListBulleted,
                                contentDescription = null,
                                tint = if (isListSelected) extended.accent else extended.textTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Lista (${displayedLots.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isListSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isListSelected) extended.accent else extended.textSecondary
                            )
                        }
                    }
                }

                // Barra horizontal de filtros rápidos. `LazyRow` solo compone los chips
                // visibles. `items` recibe la lista de valores del enum; al no declarar
                // `key`, Compose usa la posición como identidad, aceptable para una lista
                // fija y corta como esta.
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(HomeFilter.entries.toList()) { filter ->
                        // Cada chip alterna `activeFilter`; el cambio recalcula
                        // `displayedLots` y actualiza ambas vistas.
                        val isSelected = activeFilter == filter
                        Surface(
                            onClick = { activeFilter = filter },
                            color = if (isSelected) extended.accent else extended.surface2,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                Dimens.borderHairline,
                                if (isSelected) extended.accent else extended.borderSubtle
                            )
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) extended.onAccent else extended.textSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Contenido principal a pantalla completa. `when` es exhaustivo sobre el
            // enum: no hay rama por defecto y cualquier nuevo modo obligaría a decidir
            // cómo se representa.
            when (viewMode) {
                HomeViewMode.MAP -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Mapa de Google. La cámara se conserva en `cameraPositionState`;
                        // `properties.isMyLocationEnabled` solo se activa con permiso para
                        // evitar la excepción de seguridad del proveedor de ubicación.
                        // Los controles nativos se desactivan porque la pantalla dibuja
                        // sus propios botones flotantes.
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(
                                isMyLocationEnabled = locationPermissionGranted
                            ),
                            uiSettings = MapUiSettings(
                                myLocationButtonEnabled = false,
                                zoomControlsEnabled = false,
                                compassEnabled = true
                            ),
                            onMapClick = {
                                // Un clic en el mapa cierra el teclado y oculta la
                                // tarjeta de vista previa seleccionada.
                                focusManager.clearFocus()
                                selectedLot = null
                            }
                        ) {
                            // Un marcador por cada elemento de la lista filtrada. El color
                            // resume la disponibilidad: rojo si está cerrado o lleno,
                            // naranja si quedan tres o menos espacios y verde en el resto.
                            // Es una regla visual local, no un estado del backend.
                            displayedLots.forEach { parking ->
                                val markerHue = when {
                                    !parking.isOpen || parking.availableSpaces == 0 -> BitmapDescriptorFactory.HUE_RED
                                    parking.availableSpaces <= 3 -> BitmapDescriptorFactory.HUE_ORANGE
                                    else -> BitmapDescriptorFactory.HUE_GREEN
                                }

                                // `MarkerState` se crea con las coordenadas del DTO.
                                // Al pulsar se selecciona el lote para la tarjeta inferior
                                // y se anima la cámara hacia él con más zoom. Devolver
                                // `true` indica que el evento fue consumido y evita que el
                                // mapa abra su propia ventana de información.
                                Marker(
                                    state = MarkerState(LatLng(parking.latitude, parking.longitude)),
                                    title = parking.name,
                                    snippet = "${parking.availableSpaces} espacios disponibles",
                                    icon = BitmapDescriptorFactory.defaultMarker(markerHue),
                                    onClick = {
                                        selectedLot = parking
                                        coroutineScope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(parking.latitude, parking.longitude),
                                                    15.5f
                                                )
                                            )
                                        }
                                        true
                                    }
                                )
                            }
                        }

                        // Controles flotantes del mapa: reencuadrar la vista general y,
                        // si hay permiso, centrar en la ubicación del usuario. Ambos son
                        // `Surface` clicables con elevación y fondo semitransparente.
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(Dimens.spacingMd),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Botón "ver todas": vuelve a la posición y zoom iniciales.
                            // No consulta datos; solo mueve la cámara.
                            Surface(
                                onClick = {
                                    coroutineScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(defaultLocation, 13.5f)
                                        )
                                    }
                                },
                                modifier = Modifier.size(42.dp),
                                color = extended.surface1.copy(alpha = 0.92f),
                                shape = ParkeoCardShape,
                                border = BorderStroke(Dimens.borderHairline, extended.borderSubtle),
                                shadowElevation = 4.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.ZoomOutMap,
                                        contentDescription = "Ver todas las cocheras",
                                        tint = extended.accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Botón "mi ubicación": solo existe si el permiso está
                            // concedido. Vuelve a pedir `lastLocation` porque la posición
                            // pudo cambiar desde la primera consulta. La lectura se
                            // protege con `try/catch` por si el permiso fue revocado.
                            if (locationPermissionGranted) {
                                Surface(
                                    onClick = {
                                        try {
                                            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                                location?.let {
                                                    val userLatLng = LatLng(it.latitude, it.longitude)
                                                    coroutineScope.launch {
                                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                                                    }
                                                }
                                            }
                                        } catch (_: SecurityException) { }
                                    },
                                    modifier = Modifier.size(42.dp),
                                    color = extended.surface1.copy(alpha = 0.92f),
                                    shape = ParkeoCardShape,
                                    border = BorderStroke(Dimens.borderHairline, extended.borderSubtle),
                                    shadowElevation = 4.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.MyLocation,
                                            contentDescription = "Mi ubicación",
                                            tint = extended.accent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Tarjeta de vista previa del marcador seleccionado. Se anima
                        // desde/hacia abajo con `AnimatedVisibility`. `selectedLot` es
                        // nullable: la tarjeta solo existe si hay un lote elegido y el
                        // `let` garantiza un valor no nulo dentro del bloque.
                        androidx.compose.animation.AnimatedVisibility(
                            visible = selectedLot != null,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(Dimens.spacingMd)
                        ) {
                            selectedLot?.let { lot ->
                                // El badge resume el estado con precedencia:
                                // cerrado > completo > pocos espacios (reservado)
                                // > disponible. "Pocos" significa tres o menos.
                                val (badgeText, badgeVariant) = when {
                                    !lot.isOpen -> "Cerrado" to BadgeVariant.Closed
                                    lot.availableSpaces == 0 -> "Completo" to BadgeVariant.Occupied
                                    lot.availableSpaces <= 3 -> "${lot.availableSpaces} libres" to BadgeVariant.Reserved
                                    else -> "${lot.availableSpaces} libres" to BadgeVariant.Available
                                }

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = ParkeoCardShape,
                                    color = extended.surface1,
                                    border = BorderStroke(Dimens.borderHairline, extended.border),
                                    shadowElevation = 8.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Nombre, distrito (o dirección como respaldo) y
                                        // badge con el botón de cierre.
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                Text(
                                                    text = lot.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = extended.textPrimary
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = lot.district ?: lot.address,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = extended.textSecondary
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                ParkeoBadge(text = badgeText, variant = badgeVariant)
                                                Spacer(Modifier.width(8.dp))
                                                IconButton(
                                                    onClick = { selectedLot = null },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Close,
                                                        contentDescription = "Cerrar",
                                                        tint = extended.textTertiary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Capacidad total, distancia opcional y acción
                                        // principal. `lot.distance` es nullable: si el
                                        // backend no lo envía, el texto se omite. El
                                        // formato usa la configuración regional del
                                        // dispositivo para el separador decimal.
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Filled.LocalParking,
                                                        contentDescription = null,
                                                        tint = extended.accent,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = "${lot.totalCapacity} capacidad",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = extended.textSecondary,
                                                        fontSize = 12.sp
                                                    )
                                                }

                                                lot.distance?.let { dist ->
                                                    Text(
                                                        text = "•  %.1f km".format(dist),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = extended.textSecondary,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }

                                            // Navegación al detalle: se pasa solo el id.
                                            // La pantalla de detalle se encarga de pedir
                                            // los datos completos al ViewModel.
                                            ParkeoButton(
                                                text = "Ver detalles y reservar",
                                                onClick = { onNavigateToParkingDetail(lot.id) },
                                                style = ParkeoButtonStyle.Primary,
                                                modifier = Modifier.height(38.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HomeViewMode.LIST -> {
                    // La lista resuelve sus estados en este orden de prioridad:
                    // carga, error, vacío y contenido. Mientras `isLoading` es verdadero
                    // se oculta la lista anterior; no se hace "stale-while-revalidate".
                    when {
                        // Estado de carga: indicador centrado con mensaje contextual.
                        uiState.isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                ParkeoLoadingView(message = "Buscando cocheras disponibles...")
                            }
                        }

                        // Estado de error: mensaje del ViewModel y botón para reintentar
                        // la carga completa. No se usa `Snackbar` en esta pantalla.
                        uiState.error != null -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                ParkeoErrorView(
                                    message = uiState.error ?: "Error al cargar cocheras",
                                    onRetry = { viewModel.loadParkingLots() }
                                )
                            }
                        }

                        // Estado vacío: distingue entre una búsqueda sin resultados y un
                        // filtro sin coincidencias. El texto entre comillas se compone
                        // con el valor actual del estado local.
                        displayedLots.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                ParkeoEmptyState(
                                    title = "No encontramos cocheras",
                                    subtitle = if (searchQuery.isNotBlank())
                                        "No hay resultados para \"$searchQuery\" con los filtros aplicados"
                                    else
                                        "No hay cocheras que coincidan con el filtro \"${activeFilter.label}\"",
                                    icon = Icons.Filled.SearchOff
                                )
                            }
                        }

                        else -> {
                            // Lista principal. `LazyColumn` solo compone lo visible y
                            // recicla sus elementos; `contentPadding` separa del borde.
                            // `items(..., key = { it.id })` usa el id estable del backend,
                            // lo que permite reutilizar composiciones al cambiar la lista.
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(Dimens.spacingMd),
                                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                            ) {
                                // Encabezado de la lista: etiqueta fija y conteo real de
                                // elementos mostrados después del filtrado local.
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = Dimens.spacingXs),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "COCHERAS DISPONIBLES",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            color = extended.textTertiary
                                        )
                                        Text(
                                            text = "${displayedLots.size} en total",
                                            style = Typography.MonospaceTechnical,
                                            fontSize = 12.sp,
                                            color = extended.accent
                                        )
                                    }
                                }

                                // Cada tarjeta es un componente reutilizable que concentra
                                // el badge de disponibilidad, la capacidad y la distancia.
                                // El clic navega al detalle usando el id del lote.
                                items(displayedLots, key = { it.id }) { parking ->
                                    ParkingCard(
                                        parking = parking,
                                        onClick = { onNavigateToParkingDetail(parking.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
