/*
 * PARKeo · Android
 * Archivo: `android/app/src/main/java/pe/parkeo/ui/screens/admin/AdminDashboardScreen.kt`
 * Capa: presentación (Jetpack Compose), rol administrador (`ADMIN`).
 *
 * Propósito:
 * Mostrar el panel inicial del administrador con un resumen global de la red de
 * estacionamientos y reservas, además de accesos directos a las dos tareas de
 * gestión principales: estacionamientos y usuarios.
 *
 * Conceptos clave:
 * - Rol `ADMIN`: perfil con capacidad de supervisar toda la red y administrar
 *   recursos globales; no debe confundirse con la autorización efectiva, que
 *   corresponde validar al backend.
 * - Dashboard: vista de solo lectura que resume métricas y ofrece navegación.
 * - Métricas: contadores agregados de estacionamientos y reservas por estado.
 * - Gestión de estacionamientos y usuarios: el dashboard enlaza a esos flujos
 *   de administración (consulta, cambios de estado, asignación y roles), pero
 *   no implementa el CRUD completo dentro de esta pantalla.
 * - `Compose`: UI declarativa; la pantalla se redibuja cuando cambia el estado.
 * - `StateFlow`: el `AdminViewModel` publica `AdminUiState` y la pantalla lo
 *   observa con `collectAsState()`.
 * - `LazyColumn`: lista diferida que aquí organiza los bloques del panel.
 * - Formularios, diálogos y paginación: esta vista no los contiene; la edición
 *   y los diálogos pertenecen a las pantallas de gestión. Tampoco pagina: usa
 *   los conteos y las listas ya cargadas por el ViewModel.
 * - Navegación por callbacks: la pantalla no conoce rutas ni el `NavGraph`;
 *   solo invoca `onNavigateToParkingLots` u `onNavigateToUsers`.
 *
 * Flujo de usuario y datos:
 * 1. Al entrar se ejecuta `LaunchedEffect(Unit)` y se llama a
 *    `viewModel.loadDashboardData()`.
 * 2. El ViewModel consulta estadísticas, estacionamientos y usuarios mediante
 *    `AdminRepository.getStatistics()`, `getParkingLots()` y `getUsers()`, y
 *    actualiza `AdminUiState` (data class de estado de presentación).
 * 3. La pantalla muestra las tarjetas KPI y los accesos rápidos con esos datos.
 * 4. El botón de refresco vuelve a cargar con `refresh = true`, sin ocultar la
 *    información que ya estaba visible.
 *
 * Relación con ViewModel, navegación y backend:
 * - `AdminViewModel` es la fuente de verdad de presentación; aquí no se llama
 *   directamente a Retrofit ni a un repositorio.
 * - Las métricas provienen de `AdminStatisticsDto`; los conteos de locales y
 *   usuarios provienen de las listas `parkingLots` y `users` del mismo estado.
 * - Al tocar una tarjeta o un acceso rápido se delega la navegación al host de
 *   navegación (`MainScreen`/`ParkeoNavGraph`), que decide la ruta concreta.
 * - El backend es la fuente autoritativa de los datos y de los permisos. Esta
 *   pantalla solo presenta lo que el servidor devolvió para el usuario actual.
 *
 * Guía de estudio: `docs/GUIA_MAESTRA_ESTUDIANTE.md`.
 */
package pe.parkeo.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.parkeo.ui.components.*
import pe.parkeo.ui.theme.*
import pe.parkeo.ui.viewmodel.AdminViewModel

/**
 * Panel principal del administrador.
 *
 * Presenta seis tarjetas KPI con métricas agregadas de la red y dos accesos
 * rápidos. No contiene formularios ni edición directa: cualquier cambio se
 * realiza en las pantallas de estacionamientos o usuarios, a las que se navega
 * mediante los callbacks.
 *
 * La pantalla es reactiva: observa `AdminViewModel.uiState` como `StateFlow`.
 * Si `isLoading` es verdadero y no se trata de un refresco, muestra el estado de
 * carga inicial. Si ya había datos, un refresco mantiene el contenido visible.
 * No existe una rama visual dedicada para `uiState.error`; ese mensaje se
 * conserva en el estado y las pantallas de gestión lo muestran en un snackbar.
 * Tampoco hay un estado vacío explícito: si aún no hay datos, las tarjetas
 * muestran ceros y los accesos rápidos muestran conteos en cero.
 *
 * @param viewModel ViewModel del rol administrador. Aporta estadísticas,
 *   estacionamientos, usuarios y la acción de recarga.
 * @param onNavigateToParkingLots Callback que solicita abrir la gestión de
 *   estacionamientos. Se usa en la tarjeta KPI y en el acceso rápido.
 * @param onNavigateToUsers Callback que solicita abrir la gestión de usuarios.
 */
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onNavigateToParkingLots: () -> Unit = {},
    onNavigateToUsers: () -> Unit = {}
) {
    // Estado de presentación observado desde el ViewModel. `by` permite leerlo
    // como un valor normal; Compose registra la lectura para recomponer.
    val uiState by viewModel.uiState.collectAsState()
    val extended = ParkeoTheme.colors
    val stats = uiState.statistics

    // Carga inicial. `LaunchedEffect(Unit)` se ejecuta una vez por composición
    // estable de la pantalla; la clave `Unit` evita repetir la llamada en cada
    // recomposición. La carga real ocurre en una corrutina del ViewModel.
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    // Estructura Material 3: barra superior fija y contenido desplazable.
    Scaffold(
        containerColor = extended.background,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            // Barra superior: título del panel y acción de actualización manual.
            // El refresco conserva los datos actuales mientras consulta de nuevo.
            ParkeoTopBar(
                title = "Panel de Administración",
                subtitle = "Supervisión global de la red Parkeo",
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
        // Estado de carga inicial: solo se muestra si no hay un refresco en
        // curso. En un refresco se prefiere no tapar las métricas anteriores.
        if (uiState.isLoading && !uiState.isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                ParkeoLoadingView(message = "Cargando métricas de la red...")
            }
        } else {
            // Contenido del panel. `LazyColumn` permite desplazar bloques y
            // reservar espacio entre ellos mediante `spacedBy`.
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = Dimens.spacingMd),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
            ) {
                item {
                    Spacer(Modifier.height(2.dp))
                    // Sección: encabezado de métricas.
                    Text(
                        text = "MÉTRICAS CLAVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = extended.textTertiary
                    )
                }

                item {
                    // Métricas: inventario de estacionamientos y volumen total
                    // de reservas. `stats` proviene de `AdminStatisticsDto`,
                    // cargado por `loadDashboardData()` desde el repositorio.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                    ) {
                        ModernKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Estacionamientos",
                            value = "${stats.totalParkingLots}",
                            icon = Icons.Filled.LocalParking,
                            tintColor = extended.accent,
                            // La tarjeta es clicable solo si se proporciona
                            // un callback; aquí abre la gestión de locales.
                            onClick = onNavigateToParkingLots
                        )
                        ModernKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Total Reservas",
                            value = "${stats.totalReservations}",
                            icon = Icons.Filled.BookmarkBorder,
                            tintColor = extended.textPrimary
                        )
                    }
                }

                item {
                    // Métricas: reservas activas y pendientes. Reflejan el
                    // trabajo operativo inmediato de la red.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                    ) {
                        ModernKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Reservas Activas",
                            value = "${stats.activeReservations}",
                            icon = Icons.Filled.DirectionsCar,
                            tintColor = extended.signalGreen
                        )
                        ModernKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Pendientes",
                            value = "${stats.pendingReservations}",
                            icon = Icons.Filled.Schedule,
                            tintColor = extended.signalAmber
                        )
                    }
                }

                item {
                    // Métricas: reservas completadas y canceladas. Permiten
                    // comparar operación concluida frente a cancelaciones.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                    ) {
                        ModernKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Completadas",
                            value = "${stats.completedReservations}",
                            icon = Icons.Filled.CheckCircleOutline,
                            tintColor = extended.signalGreen
                        )
                        ModernKpiCard(
                            modifier = Modifier.weight(1f),
                            title = "Canceladas",
                            value = "${stats.cancelledReservations}",
                            icon = Icons.Filled.Cancel,
                            tintColor = extended.signalRed
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    // Sección: accesos rápidos a las tareas de administración.
                    Text(
                        text = "GESTIÓN DIRECTA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = extended.textTertiary
                    )
                }

                item {
                    // Accesos rápidos: estacionamientos. Toda la tarjeta es
                    // clicable y delega la navegación al host de navegación.
                    ParkeoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToParkingLots)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.spacingLg),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Contenedor circular del icono decorativo.
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(extended.accent.copy(alpha = 0.12f), CircleShape)
                                    .border(Dimens.borderHairline, extended.accent.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Storefront,
                                    contentDescription = null,
                                    tint = extended.accent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Texto descriptivo: el conteo usa la lista de
                            // estacionamientos ya cargada en el estado.
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gestión de Estacionamientos",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = extended.textPrimary
                                )
                                Text(
                                    text = "${uiState.parkingLots.size} locales en la red. Control de disponibilidad y estado.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.textSecondary
                                )
                            }

                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = extended.textTertiary
                            )
                        }
                    }
                }

                item {
                    // Accesos rápidos: usuarios. El conteo incluye todas las
                    // cuentas cargadas, sin filtrar por rol en esta pantalla.
                    ParkeoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToUsers)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimens.spacingLg),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Contenedor circular del icono decorativo.
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(extended.surface3, CircleShape)
                                    .border(Dimens.borderHairline, extended.borderSubtle, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Group,
                                    contentDescription = null,
                                    tint = extended.textPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Texto descriptivo: administradores, operadores y
                            // clientes comparten la misma lista de usuarios.
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gestión de Usuarios",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = extended.textPrimary
                                )
                                Text(
                                    text = "${uiState.users.size} cuentas registradas (Admin, Operadores, Clientes).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = extended.textSecondary
                                )
                            }

                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = extended.textTertiary
                            )
                        }
                    }
                }

                item {
                    // Espacio final para que el último acceso rápido no quede
                    // pegado al borde inferior de la ventana.
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Tarjeta reutilizable para una métrica del dashboard.
 *
 * Muestra un título corto, un icono con color de acento y el valor numérico en
 * tipografía monoespaciada. El comportamiento es pasivo salvo que se reciba
 * `onClick`; en ese caso toda la tarjeta se vuelve clicable. La tarjeta no
 * interpreta el valor: recibe el texto ya formateado por el llamador.
 *
 * @param modifier Modificador externo, habitualmente con `weight(1f)` para
 *   repartir el ancho de una fila entre dos tarjetas.
 * @param title Etiqueta de la métrica.
 * @param value Valor ya convertido a texto, por ejemplo el resultado de
 *   interpolar un contador del `AdminStatisticsDto`.
 * @param icon Icono representativo de la métrica.
 * @param tintColor Color del icono y de su fondo suave.
 * @param onClick Acción opcional al tocar la tarjeta. Si es `null`, la tarjeta
 *   no es interactiva.
 */
@Composable
private fun ModernKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    tintColor: Color,
    onClick: (() -> Unit)? = null
) {
    val extended = ParkeoTheme.colors
    ParkeoCard(
        modifier = modifier.then(
            // El modificador clicable solo se agrega cuando existe una acción.
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.spacingLg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Título de la métrica; una sola línea evita desbordes.
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = extended.textSecondary,
                    maxLines = 1
                )

                // Insignia visual del icono con fondo del color de acento.
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(tintColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Valor principal de la métrica, destacado por tamaño y peso.
            Text(
                text = value,
                style = Typography.MonospaceTechnical,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = extended.textPrimary
            )
        }
    }
}
