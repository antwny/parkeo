# PARKeo — Conceptos de la app Android

Explicación de cada concepto del stack Android que usa la app y **cómo se aplica concretamente en PARKeo**, con rutas y código reales del proyecto (`android/app/src/main/java/pe/parkeo/`).

El objetivo es que puedas abrir cualquier archivo `pe.parkeo` y saber qué problema resuelve cada pieza y por qué está donde está.

---

## Mapa del stack: qué pieza resuelve qué

| Concepto | Problema que resuelve | Dónde vive en PARKeo |
|---|---|---|
| **Jetpack Compose** | Dibujar la UI sin XML, de forma declarativa y reactiva al estado | `ui/` (pantallas, componentes, tema) |
| **ViewModel** | Sobrevivir a la rotación de pantalla y mantener la lógica fuera de la UI | `ui/viewmodel/*.kt` |
| **StateFlow** | Estado observable: la UI se redibuja sola cuando cambia | `uiState` en cada ViewModel |
| **Coroutines** | Llamadas de red sin bloquear ni callbacks enredados | `viewModelScope.launch` en ViewModels, `suspend` en repos |
| **Retrofit** | Declarar endpoints como funciones Kotlin en vez de armar HTTP a mano | `data/remote/api/*.kt` |
| **Interceptors / Authenticator (OkHttp)** | Agregar el token a cada request y renovarlo ante `401` sin tocar la UI | `data/remote/interceptor/AuthInterceptor.kt`, `TokenAuthenticator.kt` |
| **DataStore** | Persistir sesión y preferencias de forma asíncrona y segura | `data/local/SessionDataStore.kt` |
| **Navigation Compose** | Moverse entre pantallas como rutas con parámetros | `ui/navigation/NavRoutes.kt`, `ParkeoNavGraph.kt` |
| **WorkManager** | Recordatorios que se disparan aunque la app esté cerrada | `util/ReminderScheduler.kt`, `ReservationReminderWorker.kt` |
| **Google Maps Compose** | Mapa con marcadores dentro de Compose | `ui/screens/home/` (Explorar) |
| **Inyección manual (AppContainer)** | Construir/compartir dependencias sin framework (no Hilt) | `di/AppContainer.kt`, `ParkeoApplication.kt` |

---

## 1. Jetpack Compose: UI declarativa

### ¿Qué es?

Antes de Compose, una UI Android se describía en XML ("aquí va un botón en (x, y)") y el código Java/Kotlin buscaba ese botón y lo mutaba (`findViewById`). Compose invierte el modelo: **la pantalla es una función Kotlin que describe la UI a partir del estado actual**, y cuando el estado cambia, Compose re-dibuja lo mínimo necesario.

Lo importante no es la sintaxis sino el contrato:

```kotlin
@Composable
fun MiTarjeta(nombre: String, disponible: Int) { ... }
```

- Del **estado** (`nombre`, `disponible`) se deduce **toda** la interfaz: misma entrada → misma salida. No hay "manualmente esconder el TextView".
- Cuando una entrada cambia, Compose llama de nuevo a esa función (**recomposición**). El desarrollador no escribe código de "actualizar la vista".

### Cómo se aplica en PARKeo

- **Raíz**: `MainActivity.kt` llama a `setContent { ParkeoApp() }`. `ParkeoApp` (`ui/ParkeoApp.kt`) es la única composable raíz: elige el tema y monta el NavGraph.
- **Pantallas = funciones**: `HomeScreen`, `ReservationCreateScreen`, `OperatorReservationsScreen`… cada una recibe sus **datos por parámetro** y **eventos por callbacks** (`onNavigateToReservations`, `onReservationCreated`) y **no conoce** el `NavController`: la pantalla solo "decide qué pasó", el NavGraph decide adónde ir.
- **Componentes reutilizables**: `ui/components/` tiene la librería de marca: `ParkeoButton` (con `ParkeoButtonStyle` Primary/Secondary/Ghost/Destructive), `ParkeoBadge` (estados Available/Occupied/Closed…), `ParkeoCard`, etc. Cualquier pantalla nueva debe usar estos y no emitir texto/superficies "a mano".
- **Tema**: `ui/theme/Theme.kt` expone `ParkeoTheme` con la paleta extendida (`ParkeoExtendedColors`: `accent` lime, `surface1`, `textPrimary`, …) y modos **sistema/claro/oscuro** leídos de DataStore. Los tokens de espaciado/tamaño viven en `Dimens.kt` (`spacingMd`, `borderHairline`…) y la tipografía técnica monoespaciada en `Type.kt` (`Typography.MonospaceTechnical`).

> Regla práctica: si un valor de color/tamaño no está en `theme/`, no va hardcodeado en la pantalla.

---

## 2. ViewModel: la memoria de la pantalla

### ¿Qué es?

`ViewModel` es el componente que **posee el estado de una pantalla y la lógica de negocio**. Su superpoder: sobrevive a la **rotación** (y otros cambios de configuración). Si el usuario gira el teléfono, Android destruye la Activity/Fragment pero el `ViewModel` sigue vivo; la UI se re-adjunta al mismo estado y no se pierde nada (ni hace refetch).

También marca la frontera: la pantalla **no habla con el backend**, solo le pide cosas al ViewModel; y el ViewModel no toca vistas, solo publica datos.

### El patrón UiState en PARKeo

Cada ViewModel expone **un solo objeto de estado inmutable** con todo lo que la pantalla necesita. Ejemplo real (`ui/viewmodel/ReservationViewModel.kt`):

```kotlin
data class ReservationUiState(
    val isLoading: Boolean = false,
    val reservations: List<ReservationDto> = emptyList(),
    val currentReservation: ReservationDto? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val reservationCreated: Boolean = false,
    // Id de la última reserva cancelada con éxito (la pantalla lo usa para cancelar sus recordatorios)
    val cancelledReservationId: Long? = null
)

class ReservationViewModel(...) : ViewModel() {
    private val _uiState = MutableStateFlow(ReservationUiState())
    val uiState: StateFlow<ReservationUiState> = _uiState.asStateFlow()

    fun createReservation(...) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = reservationRepository.createReservation(parkingLotId, ...)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, currentReservation = result.data, reservationCreated = true) }
                is Result.Error  -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }
}
```

Lectura del ejemplo:

1. `ReservationUiState` (data class) es el estado completo: `isLoading`, datos, `error`, banderas como `reservationCreated`.
2. El estado se guarda en `_uiState: MutableStateFlow` (privado, solo el VM lo muta con `.update { copy(...) }`) y se expone como `StateFlow` de solo lectura.
3. Cada función pública es un **evento**: carga, crear, cancelar. Nada de "botón X dispara operación Y" dentro de la pantalla.
4. El resultado del repositorio se examina con `when` en dos ramas (`Success`/`Error`); el manejo de excepciones vive en el repositorio, no en el ViewModel.

La pantalla lo consume así (`ReservationCreateScreen.kt`):

```kotlin
val parkingState by parkingViewModel.uiState.collectAsState()
val availabilityState by reservationViewModel.availability.collectAsState()
```

`collectAsState` convierte el Flow en estado de Compose: cuando `_uiState` cambia, la pantalla se recompone con los valores nuevos. De ese flujo salen las decisiones de UI:

```kotlin
LaunchedEffect(reservationState.reservationCreated) {
    if (reservationState.reservationCreated) { /* navegar a Mis Reservas + boarding pass */ }
}
```

`LaunchedEffect` ejecuta procesos reactivos ligados al ciclo de la pantalla: aquí y en el resto de la app se usa para (a) disparar la carga inicial (`LaunchedEffect(parkingId) { viewModel.loadDetail() }`) y (b) reaccionar a cambios de estado que generan navegación.

### Un ViewModel por pantalla, fábrica centralizada

PARKeo tiene 9 ViewModels, uno por dominio de pantalla (`SplashViewModel`, `AuthViewModel`, `HomeViewModel`, `ParkingDetailViewModel`, `VehicleViewModel`, `ReservationViewModel`, `OperatorViewModel`, `AdminViewModel`, `OnboardingViewModel`). Ninguno los crea la UI: los construye `ParkeoViewModelFactory` (`ui/viewmodel/ViewModelFactory.kt`):

```kotlin
modelClass.isAssignableFrom(ReservationViewModel::class.java) ->
    ReservationViewModel(appContainer.reservationRepository, appContainer.parkingRepository) as T
```

La fábrica recibe el `AppContainer` y **inyecta los repositorios**. Si agregas un ViewModel nuevo: crea la clase + su UiState y registra su rama aquí; las pantallas siguen usando `viewModel(factory = viewModelFactory)` sin cambios.

---

## 3. Coroutines: asincronía ordenada

### ¿Qué es?

Una red tarda y el hilo de UI no puede esperar. Las coroutines resuelven la asincronía con código que "parece secuencial":

- `suspend fun` = función que puede pausarse sin bloquear el hilo; al terminar la llamada de red, retoma el hilo de UI sin callbacks.
- `viewModelScope.launch { ... }` = arranca la coroutine en el alcance del ViewModel (por defecto `Dispatchers.Main`); se cancela sola cuando el ViewModel muere (no hay fugas ni callbacks huérfanos).
- Resultado: UI fluida y código lineal.

### Cómo se aplica en PARKeo

**Regla de la casa**: toda la ruta red → pantalla es `suspend`. Los ViewModels nunca bloquean el hilo de UI: llaman funciones `suspend` desde `viewModelScope`; los repositorios implementan todas sus operaciones como `suspend` (`data/repository/ReservationRepositoryImpl.kt`):

```kotlin
override suspend fun getMyReservations(status: String?): Result<List<ReservationDto>> =
    try {
        val response = reservationApi.getMyReservations(status)   // Retrofit suspend
        if (response.isSuccessful && response.body()?.success == true) {
            Result.Success(response.body()!!.data?.content ?: emptyList())
        } else Result.Error(errorMessage(response, "Error al cargar reservas"))
    } catch (e: Exception) { Result.Error(e.message ?: "Error de conexión") }
```

El `when (val result = ...)` del ViewModel se empareja con `Result` (`util/Result.kt`):

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
```

Cada repo convierte "HTTP ok/no-ok/excepción" en `Result` y los errores HTTP del backend se traducen a texto de usuario con `util/NetworkErrorParser.kt` (400, 403, 409…).

**Debounce en vivo**: el buscador de Explorar no dispara una petición por tecla. `HomeViewModel` mantiene un `searchQueries: MutableSharedFlow` y en su init hace:

```kotlin
searchQueries
    .debounce(300)                    // espera a que el usuario deje de escribir
    .collectLatest { query ->         // y solo atiende la última
        ... parkingRepository.searchParkingLots(query) ...
    }
```

**El `runBlocking` tolerable**: tanto `AuthInterceptor` como `TokenAuthenticator` usan `runBlocking` para leer el token de DataStore. Ahí sí es correcto: corren en el **hilo de red de OkHttp**, no en el de UI, y OkHttp exige un valor sincrónico para armar/reintentar la petición. Son los únicos; no copies `runBlocking` fuera de los interceptores.

---

## 4. Retrofit + OkHttp: la capa de red

### ¿Qué es?

**Retrofit** convierte endpoints en funciones Kotlin (interfaces anotadas) y vía un conversor JSON serializa/deserializa. **OkHttp** es el cliente HTTP debajo, con puntos de extensión (interceptors/authenticators) para tocar las peticiones.

### Cómo se aplica en PARKeo

- **Interfaces de API**: `data/remote/api/*.kt` — una por módulo (`AuthApi`, `ParkingApi`, `VehicleApi`, `ReservationApi`, `OperatorApi`, `AdminApi` y la de usuarios). Ejemplo:

```kotlin
@POST("api/reservations")
suspend fun createReservation(@Body request: CreateReservationRequestDto): Response<ApiResponseDto<ReservationDto>>

@GET("api/operator/reservations/by-code/{code}")
suspend fun getReservationByCode(@Path("code") code: String): Response<ApiResponseDto<ReservationDto>>
```

- **DTOs**: `data/remote/dto/` con `kotlinx.serialization` (`Json { ignoreUnknownKeys = true }` tolera campos nuevos del backend). El backend responde el sobre estándar `ApiResponseDto { success, message, data }`.
- **AuthInterceptor** agrega `Authorization: Bearer <token>` a cada request normal, leído de `SessionDataStore` (si no hay sesión, la request sale sin header y el backend responde 401, que es justo lo que dispara el authenticator).
- **TokenAuthenticator** reacciona al `401 + WWW-Authenticate: Bearer` del backend: refresca con single-flight, reemplaza ambos tokens y reintenta una vez; si el refresh falla, limpia sesión y emite el evento global de expiración. Detalle completo en `docs/seguridad.md`.
- **Dos clientes OkHttp** en `AppContainer`: `bareOkHttpClient` (solo logging + timeouts) para login/register/refresh —así la petición de renovación no vuelve a pasar por el authenticator y no hay bucles— y `okHttpClient` (interceptor + authenticator) con su propio `retrofit` para todo el resto.
- **AuthApi desacoplada**: `bareAuthApi` se construye sobre `bareRetrofit` y es la única instancia de `AuthApi`; la usan `AuthRepositoryImpl` (login, registro, refresh, logout) y `TokenAuthenticator`. Todo lo demás (`parkingApi`, `vehicleApi`, `reservationApi`, `adminApi`, `operatorApi`) se crea sobre el `retrofit` autentificado.
- **Eventos fuera de Compose**: el authenticator no puede "navegar". Cuando la sesión muere, publica en `SessionEvents` (`util/SessionEvents.kt`, un `StateFlow<SessionExpired?>` con contador incremental para que cada notificación sea única), y `ParkeoApp` lo recoge con `LaunchedEffect` → Toast + `navigate(LOGIN)` limpiando el backstack (`popUpTo(0)`).

---

## 5. DataStore: sesión y preferencias

### ¿Qué es?

Reemplazo moderno de `SharedPreferences`: asíncrono, con `Flow` reactivo, y a prueba de escrituras concurrentes.

### Cómo se aplica en PARKeo

`data/local/SessionDataStore.kt` guarda en `parkeo_session`: `access_token`, `refresh_token`, `user_id`, `user_email`, `user_name`, `user_role`, `onboarding_shown` y `theme_mode`. Las escrituras/lecturas puntuales son `suspend`; los valores que la UI observa son `Flow` (así el selector de tema del Perfil re-tematiza la app en vivo: `ParkeoApp` observa `sessionDataStore.themeMode`).

Secuencia típica: `AuthViewModel.login()` → repo `POST /api/auth/login` → `Result.Success` → `sessionDataStore.saveSession(...)` (tokens + rol) → navegación a `HOME`. La decisión del splash (`SplashViewModel`) solo consulta este almacenamiento: si hay sesión va a Home; si no, a Onboarding (primera vez) o Login.

---

## 6. Navigation Compose: rutas y parámetros

### ¿Qué es?

Es el mecanismo oficial para navegar entre pantallas con un `NavController`: cada destino es una **ruta string** opcionalmente con placeholders de argumento.

### Cómo se aplica en PARKeo

- **Rutas** en `NavRoutes.kt`, con la convención "la ruta tiene el dato" y funciones helpers para construir rutas con valor:

```kotlin
const val PARKING_DETAIL = "parking/{parkingId}"
const val RESERVATION_CREATE = "reservation/create/{parkingId}"
fun parkingDetail(id: Long) = "parking/$id"
```

- **El grafo** (`ParkeoNavGraph.kt`) mapea ruta → composible y declara el tipo del argumento:

```kotlin
composable(
    route = NavRoutes.PARKING_DETAIL,
    arguments = listOf(navArgument("parkingId") { type = NavType.LongType })
) { backStackEntry ->
    val parkingId = backStackEntry.arguments?.getLong("parkingId") ?: ...
}
```

- **Pantallas desacopladas**: el destino pasa callbacks (`onReservationCreated: () -> Unit`) y el NavGraph decide el destino (por ejemplo, tras crear la reserva navega a `RESERVATIONS` con `popUpTo(HOME)` para que Back no regrese al formulario).
- **Navegación por rol**: no hay un grafo distinto por rol; un mismo grafo y `MainScreen` cambia su menú inferior según `role` (cliente: 4 pestañas; operador: 4; admin: 5 — tabla completa en `docs/arquitectura.md`). Las reservas del admin tienen su propio destino `ADMIN_RESERVATIONS`.
- **Estado global de sesión**: `ParkeoApp` escucha `SessionEvents` para re-rutar a LOGIN.

---

## 7. WorkManager: recordatorios que sobreviven al cierre

### ¿Qué es?

Cola de trabajos diferidos del sistema: los ejecuta aunque el proceso muera y son persistentes entre reinicios (a diferencia de handlers o alcances de coroutine). Ideal para "avisar al usuario dentro de 2 horas".

### Cómo se aplica en PARKeo

`util/ReminderScheduler.kt` agenda dos recordatorios por reserva y usa **nombres únicos** para poder cancelarlos:

- `KIND_START` — 15 min antes del inicio; `KIND_END` — 10 min antes del fin (zona horaria `America/Lima`). Si el momento clave (inicio/fin) ya pasó, no agenda nada; si solo pasó la hora del aviso, lo encola de inmediato.
- El worker real (`ReservationReminderWorker`) dispara la notificación (con `POST_NOTIFICATIONS` solicitada al crear la reserva vía `RequestNotificationPermission`).
- Al **cancelar** la reserva, `ReminderScheduler.cancel()` cancela los dos trabajos por su nombre único (`workName(kind, reservationId)`), coordinado con la UI mediante `cancelledReservationId` en `ReservationUiState`.

---

## 8. Inyección manual: AppContainer (sin Hilt)

### ¿Qué es?

La app necesita compartir objetos costosos/únicos (Retrofit, OkHttp, DataStore, repositorios). Un contenedor DI centraliza su construcción con `lazy` para resolver dependencias una sola vez.

### Cómo se aplica en PARKeo

```kotlin
class ParkeoApplication : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(applicationContext)
    }
}
```

`di/AppContainer.kt` (`DefaultAppContainer`) construye todo con `lazy`: `Json` → interceptores → `bareOkHttpClient` y `okHttpClient` (con interceptor + authenticator) → `bareRetrofit` y `retrofit` → los API (`parkingApi`, `vehicleApi`, `reservationApi`, `adminApi`, `operatorApi`; `bareAuthApi` es el único sobre el cliente bare) → los 6 repositorios. Los ViewModels los reciben vía `ParkeoViewModelFactory` (ver §2).

Para agregar un repositorio: interfaz en `domain/repository/`, impl en `data/repository/`, API en `data/remote/api/` y propiedad `lazy` en `AppContainer`. La decisión de "no Hilt" está documentada en `docs/arquitectura.md`.

---

## 9. Tema y design system

- `ui/theme/Theme.kt`: `ParkeoTheme(darkTheme = ...)` aplica el esquema Material 3 y agrega **colores extendidos de marca** (`ParkeoExtendedColors`: `accent`/`accentGlow`, `surface1/2`, `textPrimary/Tertiary`, `signalGreen`, …) accesibles en cualquier composable con `ParkeoTheme.colors`.
- `ui/theme/Color.kt` define el par **Obsidian (oscuro)** / **claro** (`DarkParkeoExtendedColors`, `LightParkeoExtendedColors`).
- `Dimens.kt` y `Type.kt` concentran medidas y tipografía (escala de texto, `MonospaceTechnical` para códigos PKO, montos y horas, iconos estándar).
- `ui/theme/Shape.kt` define radios de marca: `ParkeoTagShape` (6dp), `ParkeoInputShape`/`ParkeoButtonShape` (10dp), `ParkeoCardShape` (14dp), `ParkeoSheetShape` (20dp arriba) y `ParkeoPillShape` (50%).
- El selector claro/oscuro/sistema del Perfil solo escribe `theme_mode` en DataStore; `ParkeoApp` lo observa y aplica `isSystemInDarkTheme()` cuando vale "system".

---

## 10. Cómo encaja todo: el recorrido de una acción de extremo a extremo

*"Reservar el espacio A-40"* paso a paso por las capas:

1. **UI**: el usuario pulsa el botón primario en `ReservationCreateScreen` (deshabilitado mientras `state.isLoading`, con el precio calculado por el servidor visible en el resumen).
2. **Evento → ViewModel**: con la disponibilidad ya confirmada (`reservationViewModel.checkAvailability(...)` llenó `availabilityState.check` con espacios libres + precio real) y el vehículo elegido, la pantalla llama `reservationViewModel.createReservation(parkingLotId, parkingSpaceId, vehicleId, start, end)`.
3. **ViewModel → repositorio** (`viewModelScope.launch`): `ReservationRepositoryImpl.createReservation(...)` arma `CreateReservationRequestDto` y llama `ReservationApi.createReservation` (Retrofit suspend).
4. **OkHttp**: `AuthInterceptor` adjunta el Bearer; si el access token está vencido, `TokenAuthenticator` refresca y reintenta (el usuario no se entera).
5. **Backend**: valida, bloquea el espacio con `FOR UPDATE`, calcula el precio y responde `ApiResponseDto.data = ReservationDto` (estado `CONFIRMED` + `confirmationCode` `PKO-XXXXXXXX`).
6. **Resultado**: el repo mapea a `Result.Success`; el ViewModel hace `_uiState.update { it.copy(reservationCreated = true, currentReservation = result.data) }`.
7. **Reactivo → navegar**: el `LaunchedEffect(reservationState.reservationCreated)` de la pantalla dispara `onReservationCreated()`, el NavGraph lleva a Mis Reservas y ahí se agendan los recordatorios con `ReminderScheduler.schedule(...)`; en esa pantalla el boarding pass muestra el QR del código.
8. **Expiración global (si aplica)**: si en cualquier paso el refresh falló, `SessionEvents` → `ParkeoApp` → LOGIN con sesión limpia.

### Errores

Cualquier paso puede fallar: el repo devuelve `Result.Error` con mensaje ya traducido (`NetworkErrorParser`), el ViewModel lo publica en `uiState.error` y la UI lo muestra inline (snackbar/texto de error); el usuario puede corregir y reintentar sin salir de la pantalla.

---

## 11. Checklist: agregar una pantalla nueva

1. **Ruta**: `NavRoutes.kt` — const + helper con parámetros si hace falta, y `composable(...)` en `ParkeoNavGraph` con `navArgument(...)` tipado.
2. **ViewModel**: clase + `XxxUiState` (defaults: `isLoading`, datos, `error`), funciones de evento `_uiState.update { copy(...) }` y registro en `ParkeoViewModelFactory`.
3. **Datos si es nuevo**: interfaz en `domain/repository/`, impl en `data/repository/` (suspend + `Result`), API en `data/remote/api/` y DTOs en `data/remote/dto/`, propiedad `lazy` en `AppContainer`.
4. **Pantalla**: en `ui/screens/<dominio>/`, datos por parámetro + callbacks `onXxx`, sin `NavController` propio; usa `collectAsState` + `LaunchedEffect` para carga y efectos de navegación.
5. **Marca**: solo componentes `Parkeo*` (`ui/components/`), tokens de `theme/` (nada de colores/tamaños hardcodeados), copy en español.
6. **MainScreen**: si es pestaña, agrégala a la lista del rol correspondiente (`ParkeoBottomNavItem`); si es destino, conéctala con un callback desde la pestaña que la invoca.
7. **Extras**: si la pantalla publica notificaciones, pide `POST_NOTIFICATIONS` con `util/RequestNotificationPermission.kt`; si es accesible solo con sesión, el guard ya lo cubre el nav de sesión.

---

## Referencias cruzadas

- Arquitectura de capas y decisiones: `docs/arquitectura.md`
- Seguridad (JWT, refresh, rate limit): `docs/seguridad.md`
- Endpoints del backend que consumen estos ViewModels: `docs/api.md`
- Flujos de usuario por rol (qué se ve en cada pantalla): `docs/flujos.md`
