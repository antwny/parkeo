/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/ui/screens/vehicles/VehiclesScreen.kt
 * Capa: presentación (Jetpack Compose).
 *
 * Propósito:
 * Listar los vehículos del usuario autenticado, registrar uno nuevo mediante un
 * diálogo con formulario y eliminar un vehículo con confirmación. Esta versión no
 * ofrece edición de vehículos: solo alta, consulta y baja.
 *
 * Conceptos clave:
 * - Compose declarativo: la lista y los diálogos dependen del estado observado.
 * - `collectAsState`: observa `VehicleUiState` (vehículos, tipos, carga y mensajes).
 * - `remember`: conserva el formulario del diálogo y la visibilidad de los diálogos
 *   mientras la composición viva. No se usa `rememberSaveable`, así que una rotación
 *   reinicia el formulario y cierra el diálogo.
 * - `LaunchedEffect(uiState.successMessage)`: reacciona al éxito del ViewModel para
 *   cerrar el diálogo y limpiar mensajes.
 * - `Scaffold`, `FloatingActionButton` y `ParkeoTopBar` (que encapsula `TopAppBar`).
 * - `Modifier`: cadena inmutable que declara tamaño, relleno, peso, alineación y
 *   demás propiedades visuales de cada elemento.
 * - `LazyColumn` con `items` y `key` para la lista; `AlertDialog` para alta y baja.
 * - `ExposedDropdownMenuBox` para seleccionar el tipo de vehículo.
 * - State hoisting: los callbacks `onAdd`, `onDelete` y `onDismiss` delegan en el
 *   ViewModel; la pantalla no habla con el repositorio ni con Retrofit.
 * - `derivedStateOf` no se usa aquí: los valores derivados (título del vehículo,
 *   subtítulo) se calculan directamente durante la composición porque son baratos.
 *
 * Flujo de usuario y datos:
 * 1. El `VehicleViewModel` carga vehículos y tipos al construirse.
 * 2. La pantalla muestra carga, estado vacío o la lista.
 * 3. El botón flotante (o la acción del estado vacío) abre `AddVehicleDialog`.
 * 4. El formulario valida localmente placa y tipo; al guardar llama a
 *    `viewModel.addVehicle(...)`.
 * 5. Cuando el ViewModel publica `successMessage`, el efecto cierra el diálogo y
 *    limpia los mensajes. La lista se recarga dentro del ViewModel.
 * 6. Eliminar abre un `AlertDialog` de confirmación; al aceptar se llama a
 *    `viewModel.deleteVehicle(id)` y el ViewModel recarga la lista.
 *
 * Manejo de errores y limitaciones:
 * - `uiState.error` se muestra dentro de `AddVehicleDialog`. Si el error ocurre al
 *   eliminar o cargar con el diálogo cerrado, esta pantalla no lo presenta con un
 *   `Snackbar` ni con otro aviso visible; es una limitación del diseño actual.
 * - El orden de las ramas muestra el estado vacío antes que un posible error de carga,
 *   por lo que una carga fallida sin vehículos se ve como "Sin vehículos".
 * - No hay paginación: se renderiza la lista completa recibida del backend.
 *
 * Relación con otras capas:
 * - `VehicleViewModel` expone `VehicleUiState`, `loadVehicles`, `loadVehicleTypes`,
 *   `addVehicle`, `deleteVehicle` y `clearMessages`.
 * - `VehicleDto` y `VehicleTypeDto` son el contrato de red.
 * - `ParkeoCard`, `ParkeoTextField`, `ParkeoButton`, `ParkeoEmptyState` y
 *   `ParkeoLoadingView` son componentes reutilizables de `ui/components`.
 *
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.ui.screens.vehicles

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.data.remote.dto.VehicleDto
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.VehicleViewModel

/**
 * Pantalla de gestión de vehículos del usuario.
 *
 * Muestra una lista de `VehicleCard` o un estado vacío con acción para agregar. El
 * botón flotante abre el formulario de alta. El diálogo se controla con estado local
 * (`showAddDialog`) y se cierra automáticamente cuando el ViewModel reporta éxito.
 *
 * @param viewModel fuente de verdad con vehículos, tipos, carga, error y éxito.
 * @param onNavigateBack callback para volver desde la barra superior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesScreen(
    viewModel: VehicleViewModel,
    onNavigateBack: () -> Unit
) {
    // Estado observable del ViewModel. `VehicleUiState` agrupa `isLoading`, `vehicles`,
    // `vehicleTypes`, `error` y `successMessage`. Cada emisión recompone las ramas que
    // lo leen.
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors

    // Estado local de presentación: visibilidad del diálogo de alta. Podría haberse
    // usado `rememberSaveable` para sobrevivir a rotaciones, pero aquí se reinicia.
    var showAddDialog by remember { mutableStateOf(false) }

    // El éxito de `addVehicle` se comunica mediante `successMessage`. Cuando llega un
    // valor no nulo se cierra el diálogo y se limpian los mensajes para que el efecto
    // no se repita. `clearMessages` vuelve a emitir estado, pero con `successMessage`
    // nulo la condición no se cumple y no hay bucle.
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            showAddDialog = false
            viewModel.clearMessages()
        }
    }

    // Estructura Material 3: barra superior, contenido y botón flotante. No se usa
    // `SnackbarHost` en esta pantalla.
    Scaffold(
        containerColor = extended.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            ParkeoTopBar(
                title = "Mis vehículos",
                onNavigationClick = onNavigateBack
            )
        },
        floatingActionButton = {
            // Abrir el diálogo limpia mensajes previos para no mostrar un error viejo
            // al iniciar un nuevo formulario.
            FloatingActionButton(
                onClick = {
                    viewModel.clearMessages()
                    showAddDialog = true
                },
                containerColor = extended.accent,
                contentColor = extended.onAccent,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, "Agregar vehículo")
            }
        }
    ) { padding ->
        // Prioridad de estados: carga, vacío y lista. No hay rama explícita de error;
        // el error de lectura se refleja como lista vacía y el error de alta se muestra
        // dentro del diálogo. La comprobación de vacío usa `isEmpty()` sobre la lista.
        when {
            // Carga inicial o recarga tras alta/baja.
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    ParkeoLoadingView(message = "Cargando vehículos...")
                }
            }

            // Estado vacío: invita a registrar el primer vehículo. La acción vuelve a
            // limpiar mensajes y abre el mismo diálogo que el botón flotante.
            uiState.vehicles.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    ParkeoEmptyState(
                        title = "Sin vehículos",
                        subtitle = "Agrega tu primer vehículo para comenzar a reservar en nuestra red",
                        icon = Icons.Filled.DirectionsCar,
                        actionLabel = "Agregar vehículo",
                        onAction = {
                            viewModel.clearMessages()
                            showAddDialog = true
                        }
                    )
                }
            }

            // Lista de vehículos. `items(..., key = { it.id })` usa el id del backend
            // como identidad estable; Compose puede reutilizar cada tarjeta al cambiar
            // la lista. La eliminación se delega al ViewModel pasando el id.
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(Dimens.spacingMd),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    items(uiState.vehicles, key = { it.id }) { vehicle ->
                        VehicleCard(
                            vehicle = vehicle,
                            onDelete = { viewModel.deleteVehicle(vehicle.id) }
                        )
                    }
                }
            }
        }

        // Diálogo de alta. Se compone fuera del `when` para superponerse al contenido.
        // Recibe el catálogo de tipos y el estado de carga/error del ViewModel. Los
        // callbacks cierran y limpian, o envían los datos al ViewModel.
        if (showAddDialog) {
            AddVehicleDialog(
                vehicleTypes = uiState.vehicleTypes,
                isLoading = uiState.isLoading,
                errorMessage = uiState.error,
                onDismiss = {
                    showAddDialog = false
                    viewModel.clearMessages()
                },
                onAdd = { plate, brand, model, color, typeId ->
                    viewModel.addVehicle(plate, brand, model, color, typeId)
                }
            )
        }
    }
}

/**
 * Tarjeta de un vehículo con su matrícula, descripción y acción de eliminación.
 *
 * El título combina marca y modelo; si ambos faltan o están en blanco, muestra
 * "Vehículo". El subtítulo combina color y nombre del tipo de vehículo separados por
 * un punto medio. La eliminación pide confirmación en un `AlertDialog` local.
 *
 * @param vehicle DTO con placa, marca, modelo, color y tipo.
 * @param onDelete callback que ejecuta la baja en el ViewModel; solo se invoca tras
 *   confirmar el diálogo.
 */
@Composable
private fun VehicleCard(vehicle: VehicleDto, onDelete: () -> Unit) {
    val extended = ParkeoTheme.colors

    // Estado local del diálogo de confirmación. Vive mientras esta tarjeta esté en
    // composición; cada tarjeta gestiona su propia confirmación.
    var showConfirmDialog by remember { mutableStateOf(false) }

    ParkeoCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.spacingLg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Insignia estilizada de matrícula peruana.
            LicensePlateBadge(plate = vehicle.licensePlate)

            Spacer(Modifier.width(14.dp))

            // Identificación del vehículo. `listOfNotNull` descarta nulos y `filter`
            // descarta cadenas vacías antes de unirlas; `ifBlank` cubre el caso de que
            // no quede ningún texto. `Modifier.weight(1f)` ocupa el espacio restante.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listOfNotNull(vehicle.brand, vehicle.model).filter { it.isNotBlank() }.joinToString(" ")
                        .ifBlank { "Vehículo" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = extended.textPrimary
                )

                Text(
                    text = listOfNotNull(vehicle.color, vehicle.vehicleType.name).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.textSecondary
                )
            }

            // Abre solo la confirmación; la baja real se delega al confirmar.
            IconButton(onClick = { showConfirmDialog = true }) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Eliminar",
                    tint = extended.signalRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Diálogo de confirmación de baja. Incluye la placa para que el usuario sepa qué
    // vehículo eliminará. "Cancelar" solo cierra; "Eliminar" invoca `onDelete` y luego
    // cierra. No se espera la respuesta del backend dentro del diálogo.
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = extended.surface1,
            title = {
                Text(
                    text = "Eliminar vehículo",
                    fontWeight = FontWeight.Bold,
                    color = extended.textPrimary
                )
            },
            text = {
                Text(
                    text = "¿Confirmas que deseas eliminar el vehículo con placa ${vehicle.licensePlate}?",
                    color = extended.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showConfirmDialog = false
                }) {
                    Text("Eliminar", color = extended.signalRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar", color = extended.textSecondary)
                }
            }
        )
    }
}

/**
 * Insignia visual que imita una placa peruana: rótulo "PERÚ" y matrícula en fuente
 * monoespaciada. Es solo presentación; no valida ni transforma la placa.
 *
 * @param plate texto de la matrícula tal como llegó en el DTO.
 */
@Composable
private fun LicensePlateBadge(plate: String) {
    val extended = ParkeoTheme.colors
    Surface(
        color = extended.surface3,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(Dimens.borderHairline, extended.accent.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PERÚ",
                fontSize = 8.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = extended.accent
            )
            Text(
                text = plate,
                style = Typography.MonospaceTechnical,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = extended.textPrimary
            )
        }
    }
}

/**
 * Normaliza una placa peruana mientras el usuario escribe.
 *
 * Pasos:
 * 1. Convierte a mayúsculas.
 * 2. Elimina todo carácter que no sea letra de A a Z o dígito.
 * 3. Si quedan más de tres caracteres, inserta un guion después del tercero y
 *    conserva como máximo tres más (formato `ABC-123`).
 * 4. Si quedan tres o menos, los devuelve sin guion.
 *
 * No valida que la placa sea real ni que la distribución de letras/dígitos cumpla una
 * norma específica; solo da formato. El límite de longitud se controla además en el
 * campo de texto.
 *
 * @param input texto crudo introducido por el usuario.
 * @return placa en mayúsculas, sin caracteres especiales y con guion si corresponde.
 */
private fun formatPeruvianPlate(input: String): String {
    val clean = input.uppercase().replace(Regex("[^A-Z0-9]"), "")
    return if (clean.length > 3) {
        "${clean.take(3)}-${clean.drop(3).take(3)}"
    } else {
        clean.take(6)
    }
}

/**
 * Diálogo de alta de vehículo con formulario controlado.
 *
 * Campos:
 * - Placa obligatoria, con formato `ABC-123` y longitud exacta de 7 caracteres.
 * - Marca, modelo y color opcionales; solo admiten letras, dígitos, espacios y
 *   caracteres acentuados de español.
 * - Tipo de vehículo obligatorio, elegido de `vehicleTypes` mediante un desplegable.
 *
 * El botón "Guardar" se habilita cuando la placa tiene 7 caracteres, hay un tipo
 * seleccionado y no hay una operación en curso. Los campos opcionales se envían como
 * `null` si quedan vacíos. El error del ViewModel se muestra como banner dentro del
 * diálogo. Al guardar no se cierra aquí: el cierre ocurre cuando llega el éxito.
 *
 * @param vehicleTypes catálogo de tipos disponibles; puede llegar vacío mientras carga.
 * @param isLoading indica una operación en curso; deshabilita Guardar y muestra spinner.
 * @param errorMessage mensaje de error del ViewModel, o `null` si no hay error.
 * @param onDismiss callback para cerrar sin guardar.
 * @param onAdd callback con placa, marca, modelo, color y id de tipo. Los opcionales
 *   se pasan como `String?`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddVehicleDialog(
    vehicleTypes: List<pe.parkeo.data.remote.dto.VehicleTypeDto>,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onAdd: (String, String?, String?, String?, Long) -> Unit
) {
    val extended = ParkeoTheme.colors

    // Estado local del formulario. Cada campo es un `String`; `selectedTypeId` es
    // nullable hasta que exista un catálogo. `remember` evita perder los valores en
    // recomposiciones, pero no sobrevive a la recreación de la Activity.
    var plate by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var selectedTypeId by remember(vehicleTypes) {
        mutableStateOf<Long?>(vehicleTypes.firstOrNull()?.id)
    }
    var expanded by remember { mutableStateOf(false) }

    // Regex que restringe caracteres especiales en marca, modelo y color. Permite
    // letras ASCII, vocales acentuadas, eñe, dígitos y espacios; no permite signos
    // de puntuación. La placa tiene su propio formateador.
    val alphaNumericRegex = Regex("^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ ]*$")

    // Respaldo por si el catálogo llega después de abrir el diálogo y el estado inicial
    // quedó en `null`. Si ya hay una selección, no la sobrescribe.
    LaunchedEffect(vehicleTypes) {
        if (selectedTypeId == null && vehicleTypes.isNotEmpty()) {
            selectedTypeId = vehicleTypes.first().id
        }
    }

    // `AlertDialog` de Material 3. El contenido es un formulario con banners de error,
    // campos de texto y un desplegable. El diálogo no se cierra solo al guardar.
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = extended.surface1,
        title = {
            Text(
                text = "Agregar vehículo",
                fontWeight = FontWeight.Bold,
                color = extended.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
            ) {
                // Banner de error del ViewModel. Solo existe si `errorMessage` no es
                // nulo; se coloca arriba del formulario para que sea visible sin hacer
                // scroll. No reemplaza las validaciones locales.
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
                        Row(
                            modifier = Modifier.padding(Dimens.spacingSm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = extended.signalRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(Dimens.spacingSm))
                            Text(
                                text = errorText,
                                style = MaterialTheme.typography.bodySmall,
                                color = extended.signalRed,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Campo de placa. Se limita la entrada cruda a 7 caracteres y se aplica
                // `formatPeruvianPlate` en cada cambio: mayúsculas, solo alfanuméricos y
                // guion tras el tercer carácter. El resultado `ABC-123` también mide 7.
                ParkeoTextField(
                    value = plate,
                    onValueChange = { input ->
                        if (input.length <= 7) {
                            plate = formatPeruvianPlate(input)
                        }
                    },
                    label = "Placa *",
                    placeholder = "ABC-123",
                    modifier = Modifier.fillMaxWidth()
                )

                // Campos opcionales. La validación local rechaza cualquier carácter que
                // no cumpla `alphaNumericRegex`; si no cumple, el valor anterior se
                // conserva y la pulsación no se refleja.
                ParkeoTextField(
                    value = brand,
                    onValueChange = { if (it.matches(alphaNumericRegex)) brand = it },
                    label = "Marca",
                    placeholder = "Toyota",
                    modifier = Modifier.fillMaxWidth()
                )

                ParkeoTextField(
                    value = model,
                    onValueChange = { if (it.matches(alphaNumericRegex)) model = it },
                    label = "Modelo",
                    placeholder = "Corolla",
                    modifier = Modifier.fillMaxWidth()
                )

                ParkeoTextField(
                    value = color,
                    onValueChange = { if (it.matches(alphaNumericRegex)) color = it },
                    label = "Color",
                    placeholder = "Gris Metálico",
                    modifier = Modifier.fillMaxWidth()
                )

                // Selector de tipo de vehículo. El campo visible es de solo lectura y
                // muestra el nombre del tipo seleccionado o un texto de estado. El
                // `ExposedDropdownMenu` lista las opciones reales del catálogo. El
                // `menuAnchor()` asocia el campo con el menú desplegable.
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    ParkeoTextField(
                        value = vehicleTypes.firstOrNull { it.id == selectedTypeId }?.name
                            ?: if (vehicleTypes.isEmpty()) "Cargando tipos..." else "Seleccionar tipo",
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
                        // Al elegir un tipo se guarda su id y se cierra el menú.
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
            // Validación local mínima: placa de 7 caracteres (incluye el guion),
            // tipo seleccionado y ninguna carga en curso. `onAdd` recibe los campos
            // recortados; los opcionales vacíos se convierten en `null`. La validación
            // definitiva y la unicidad de la placa corresponden al backend.
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
                enabled = plate.length == 7 && selectedTypeId != null && !isLoading,
                style = ParkeoButtonStyle.Primary,
                modifier = Modifier.height(Dimens.buttonHeightDefault)
            )
        },
        dismissButton = {
            // Cerrar no envía datos; el padre decide limpiar mensajes en `onDismiss`.
            ParkeoButton(
                text = "Cancelar",
                onClick = onDismiss,
                style = ParkeoButtonStyle.Ghost,
                modifier = Modifier.height(Dimens.buttonHeightDefault)
            )
        }
    )
}