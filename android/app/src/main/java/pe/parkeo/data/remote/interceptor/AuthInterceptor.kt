/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/remote/interceptor/AuthInterceptor.kt
 * Capa: datos/red (interceptor de OkHttp).
 * Propósito: añadir el token de acceso de la sesión local a cada petición HTTP que
 * construye Retrofit, sin que los repositorios tengan que conocer ese detalle.
 * Conceptos clave: `Interceptor` de OkHttp, cadena de interceptores, `Request`,
 * `Response`, `Authorization: Bearer`, `runBlocking`, `Flow.first()`, token JWT.
 * Flujo resumido: Retrofit crea la petición -> OkHttp ejecuta este interceptor ->
 * se lee `SessionDataStore.getAccessToken()` -> si hay token se añade el encabezado ->
 * `chain.proceed()` continúa hacia el resto de interceptores y la red -> el backend
 * valida el JWT y responde 200, 401 o 403 según la ruta y el rol.
 * Relación: lo registra `AppContainer` en el `OkHttpClient` que comparte Retrofit;
 * depende de `SessionDataStore` y protege los endpoints de `AuthApi`, `ParkingApi`,
 * `ReservationApi`, `VehicleApi`, `AdminApi` y `OperatorApi`. No implementa refresh
 * automático ni decide roles: eso vive en el backend y en los ViewModels.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.remote.interceptor

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import pe.parkeo.data.local.SessionDataStore

/**
 * Interceptor de aplicación de OkHttp que inyecta el token de acceso.
 *
 * OkHttp ejecuta los interceptores de aplicación en el orden en que se registran.
 * `AppContainer` registra este interceptor antes del logging; por eso el encabezado
 * ya está presente cuando se registra la petición. En compilaciones de depuración hay
 * que revisar que el logging de OkHttp no exponga el valor del token.
 *
 * No mantiene una lista de rutas públicas: si existe token, lo añade también a las
 * rutas de autenticación. Que una ruta ignore el encabezado o exija autenticación lo decide
 * Spring Security en el backend. Tampoco renueva tokens ni reintenta peticiones: un
 * 401 llega tal cual al repositorio o ViewModel.
 *
 * @param sessionDataStore almacén local del que se lee el token vigente.
 * @property sessionDataStore se consulta de forma suspendida en cada intercepción.
 */
class AuthInterceptor(
    private val sessionDataStore: SessionDataStore
) : Interceptor {
    /**
     * Añade `Authorization: Bearer <token>` y continúa la cadena.
     *
     * `Interceptor.intercept` es síncrono y no `suspend`, mientras que DataStore solo
     * ofrece lectura suspendida. `runBlocking` confina el bloqueo al hilo de trabajo de
     * OkHttp (nunca al hilo principal de la UI) y `first()` toma una instantánea sin
     * quedarse observando cambios. El costo es una lectura por petición; el beneficio
     * es que el token siempre refleja el último login, refresh o logout.
     *
     * Si no hay token o está en blanco, la petición continúa sin el encabezado: los
     * endpoints públicos responden normal y los protegidos devuelven 401. Este
     * interceptor no registra el token ni lo incluye en la URL; el logging de OkHttp
     * en depuración debe revisarse para no exponer credenciales. Si DataStore no puede
     * leerse, la excepción de E/S se propaga y la petición falla en vez de enviarse
     * silenciosamente sin token.
     *
     * @param chain cadena de interceptores de OkHttp; permite leer la petición y
     *   continuar con `proceed`.
     * @return la respuesta HTTP producida por el resto de la cadena.
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { sessionDataStore.getAccessToken() }
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
