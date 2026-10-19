/*
 * PARKeo · Android
 * Archivo: di/AppContainer.kt
 * Capa: inyección de dependencias manual (patrón contenedor).
 *
 * Propósito:
 *   Construir y compartir las dependencias de toda la aplicación: DataStore,
 *   cliente HTTP, Retrofit, las interfaces de API y las implementaciones de
 *   repositorio. Es el único lugar donde se decide cómo se conectan las piezas;
 *   las pantallas y ViewModels reciben interfaces ya construidas.
 *
 * Conceptos clave:
 *   - Inversión de dependencias: la UI y los ViewModels dependen de interfaces
 *     (`AuthRepository`, `ParkingRepository`, ...), no de Retrofit ni de OkHttp.
 *   - Contenedor: `AppContainer` es el contrato y `DefaultAppContainer` la
 *     implementación real. `ParkeoApplication` crea una instancia por proceso y
 *     la expone en `appContainer`.
 *   - `by lazy`: cada dependencia se inicializa en el primer acceso y se
 *     reutiliza después. Evita crear Retrofit, OkHttp o DataStore antes de que
 *     se necesiten y garantiza una sola instancia por proceso.
 *   - Configuración de red: un único `OkHttpClient` con `AuthInterceptor` y
 *     logging solo en debug, más los timeouts que esperan las llamadas al
 *     backend.
 *   - Serialización: `Json` de kotlinx.serialization tolera campos extra del
 *     backend (`ignoreUnknownKeys`), entradas flexibles (`isLenient`) y valores
 *     inválidos recuperables (`coerceInputValues`).
 *
 * Flujo resumido:
 *   1. `ParkeoApplication.onCreate` crea `DefaultAppContainer(context)`.
 *   2. El primer uso de un repositorio dispara la cadena perezosa:
 *      `Json` -> `SessionDataStore` -> `AuthInterceptor` -> `OkHttpClient` ->
 *      `Retrofit` -> API -> repositorio.
 *   3. `ViewModelFactory` lee el contenedor desde `ParkeoApplication` y lo
 *      inyecta en los ViewModels.
 *
 * Relación con otros archivos:
 *   - `ParkeoApplication.kt` expone el contenedor.
 *   - La carpeta `data/remote/api/` define las interfaces que Retrofit implementa.
 *   - `data/remote/interceptor/AuthInterceptor.kt` agrega el token a cada
 *     petición usando `SessionDataStore`.
 *   - La carpeta `data/repository/` contiene las implementaciones que reciben
 *     estas APIs.
 *   - `ui/viewmodel/ViewModelFactory.kt` consume el contenedor.
 *
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones 2, 5 y 6).
 */
package pe.parkeo.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pe.parkeo.BuildConfig
import pe.parkeo.data.local.SessionDataStore
import pe.parkeo.data.remote.api.*
import pe.parkeo.data.remote.interceptor.AuthInterceptor
import pe.parkeo.data.repository.*
import pe.parkeo.domain.repository.*
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Contrato del contenedor de dependencias de PARKeo.
 *
 * Declara todo lo que la capa de presentación necesita para funcionar. Una
 * interfaz permite cambiar la implementación (por ejemplo, una versión de
 * pruebas con repositorios falsos) sin tocar los ViewModels ni las pantallas.
 * Solo se exponen repositorios y el almacén de sesión; las APIs, OkHttp y
 * Retrofit quedan encapsulados dentro de `DefaultAppContainer`.
 */
interface AppContainer {
    /** Almacén local de tokens y datos de sesión (Preferences DataStore). */
    val sessionDataStore: SessionDataStore
    /** Operaciones de autenticación: login, registro, refresh y logout. */
    val authRepository: AuthRepository
    /** Consulta de estacionamientos, detalle y disponibilidad. */
    val parkingRepository: ParkingRepository
    /** Gestión de vehículos del usuario autenticado. */
    val vehicleRepository: VehicleRepository
    /** Creación, listado y cancelación de reservas. */
    val reservationRepository: ReservationRepository
    /** Operaciones exclusivas del rol ADMIN. */
    val adminRepository: AdminRepository
    /** Operaciones exclusivas del rol OPERADOR. */
    val operatorRepository: OperatorRepository
}

/**
 * Implementación real del contenedor.
 *
 * Toda la construcción es perezosa (`by lazy`): el orden de las propiedades no
 * importa porque nada se crea hasta el primer acceso. El `Context` recibido es
 * el de la aplicación (no el de una Activity), de modo que el contenedor puede
 * vivir durante todo el proceso sin retener pantallas. Esta clase no contiene
 * lógica de negocio: solo ensambla objetos y los conecta.
 *
 * @param context contexto de aplicación usado por `SessionDataStore` para
 *   acceder a Preferences DataStore.
 * @property context se conserva solo para construir el almacén de sesión.
 */
class DefaultAppContainer(private val context: Context) : AppContainer {

    /**
     * Motor JSON compartido por Retrofit.
     *
     * `ignoreUnknownKeys` evita fallos cuando el backend agrega campos que los DTO
     * aún no modelan; `isLenient` permite formatos flexibles; `coerceInputValues`
     * sustituye valores inválidos por el valor por defecto del DTO cuando este lo
     * declara. Ninguna opción corrige nombres o tipos realmente distintos: en ese
     * caso la deserialización falla y el repositorio la reporta como error.
     */
    private val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    /** Almacén único de sesión: lo comparten repositorios, interceptor y ViewModels. */
    override val sessionDataStore: SessionDataStore by lazy {
        SessionDataStore(context)
    }

    /**
     * Interceptor que añade el bearer token a las peticiones HTTP.
     *
     * Se construye con el mismo `SessionDataStore` que usan los repositorios, de
     * modo que el token que se envía es el último login, refresh o logout.
     */
    private val authInterceptor: AuthInterceptor by lazy {
        AuthInterceptor(sessionDataStore)
    }

    /**
     * Cliente HTTP compartido por todas las APIs.
     *
     * El orden de los interceptores importa: `AuthInterceptor` se agrega antes del
     * logging para que el encabezado ya esté presente si en debug se inspecciona la
     * petición. El logging de cuerpo (`BODY`) solo se activa en `BuildConfig.DEBUG`;
     * en release queda en `NONE` para no exponer tokens ni datos en registros. Los
     * timeouts de 30 segundos dan margen a la red local sin bloquear la UI
     * indefinidamente.
     */
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Retrofit configurado con la URL del backend y el cliente HTTP compartido.
     *
     * `BuildConfig.BASE_URL` viene de `app/build.gradle.kts`: en debug apunta a la
     * IP configurada en `local.properties` (el emulador usa `10.0.2.2` para llegar
     * al equipo anfitrión) y en release a la URL HTTPS de producción. Retrofit exige
     * que la base termine en `/`, por eso el valor de Gradle la incluye. El conversor
     * de kotlinx.serialization conecta el motor JSON con los DTO anotados
     * `@Serializable`.
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    // ---------------------------------------------------------------------------
    // Implementaciones de las interfaces de API. `retrofit.create` genera en
    // tiempo de ejecución una clase que traduce cada anotación HTTP en una
    // petición real; no hay código manual de red en los repositorios.
    // ---------------------------------------------------------------------------

    private val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    private val parkingApi: ParkingApi by lazy {
        retrofit.create(ParkingApi::class.java)
    }

    private val vehicleApi: VehicleApi by lazy {
        retrofit.create(VehicleApi::class.java)
    }

    private val reservationApi: ReservationApi by lazy {
        retrofit.create(ReservationApi::class.java)
    }

    private val adminApi: AdminApi by lazy {
        retrofit.create(AdminApi::class.java)
    }

    private val operatorApi: OperatorApi by lazy {
        retrofit.create(OperatorApi::class.java)
    }

    // ---------------------------------------------------------------------------
    // Repositorios concretos. Aquí se unen las APIs con la persistencia local:
    // `AuthRepositoryImpl` recibe además el DataStore porque guarda y limpia la
    // sesión; el resto solo depende de su interfaz de red. Los ViewModels verán
    // estas propiedades a través del tipo `AppContainer`.
    // ---------------------------------------------------------------------------

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(authApi, sessionDataStore)
    }

    override val parkingRepository: ParkingRepository by lazy {
        ParkingRepositoryImpl(parkingApi)
    }

    override val vehicleRepository: VehicleRepository by lazy {
        VehicleRepositoryImpl(vehicleApi)
    }

    override val reservationRepository: ReservationRepository by lazy {
        ReservationRepositoryImpl(reservationApi)
    }

    override val adminRepository: AdminRepository by lazy {
        AdminRepositoryImpl(adminApi)
    }

    override val operatorRepository: OperatorRepository by lazy {
        OperatorRepositoryImpl(operatorApi)
    }
}
