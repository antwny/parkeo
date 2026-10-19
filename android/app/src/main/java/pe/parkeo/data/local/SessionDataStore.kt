/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/local/SessionDataStore.kt
 * Capa: datos/local (persistencia de sesión en el dispositivo).
 * Propósito: guardar y exponer de forma reactiva los datos mínimos de la sesión
 * autenticada (tokens, identidad y rol) y el indicador de onboarding.
 * Conceptos clave: Preferences DataStore, Flow, claves tipadas, escritura atómica,
 * arranque en frío, persistencia local y separación entre sesión y datos de negocio.
 * Flujo resumido: AuthRepositoryImpl recibe AuthResponseDto tras login, registro o
 * refresh y llama a saveSession(); Splash/Main observan isLoggedIn y getUserInfo();
 * AuthInterceptor lee getAccessToken(); el logout llama a clearSession().
 * Relación: lo crea AppContainer (DefaultAppContainer) y lo comparten AuthRepositoryImpl,
 * AuthInterceptor y los ViewModels de arranque, autenticación y perfil. No conoce Retrofit
 * ni las reglas del backend; solo persiste lo que la capa de datos decide guardar.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Instancia única de `Preferences DataStore` asociada al `Context` de la aplicación.
 *
 * El delegado `preferencesDataStore` crea, en la primera lectura, el archivo
 * `parkeo_session.preferences_pb` dentro del directorio `files/datastore` de la app y
 * mantiene el almacén abierto durante el proceso. Debe declararse una sola vez por
 * archivo y proceso: crear varias instancias sobre el mismo archivo provoca bloqueos o
 * corrupción. DataStore serializa lecturas y escrituras, por lo que es seguro acceder
 * desde varias corrutinas. La extensión permite usar `context.dataStore` desde cualquier
 * componente con `Context`, en este caso `SessionDataStore`.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "parkeo_session")

/**
 * Fuente de verdad local de la sesión autenticada.
 *
 * Persiste tokens y datos de identidad en Preferences DataStore y los expone como
 * `Flow` para que la UI reaccione a los cambios. Los tokens son credenciales: este
 * almacén nunca los imprime en logs, no los muestra en pantalla y no los incluye en
 * mensajes de error. DataStore no cifra por sí mismo; la protección depende del
 * sandbox de la aplicación y de no respaldar el archivo en ubicaciones inseguras.
 *
 * @param context contexto de aplicación usado para obtener la instancia de DataStore.
 * @property context se conserva para acceder a `context.dataStore` en cada operación.
 */
class SessionDataStore(private val context: Context) {

    /**
     * Claves tipadas del archivo de preferencias.
     *
     * Se agrupan en un `object` privado para evitar literales repetidos y errores de
     * tipeo. Los nombres son parte del formato persistido: cambiarlos invalida las
     * sesiones ya guardadas en instalaciones existentes.
     */
    private object Keys {
        // Token de acceso de corta duración. Se envía como `Authorization: Bearer`.
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        // Token de larga duración y revocable, usado solo contra `/api/auth/refresh`.
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        // Identificador interno del usuario en el backend (BIGINT).
        val USER_ID = longPreferencesKey("user_id")
        // Correo de la cuenta; se usa como indicador de identidad guardada.
        val USER_EMAIL = stringPreferencesKey("user_email")
        // Nombre visible, compuesto por el repositorio como `firstName lastName`.
        val USER_NAME = stringPreferencesKey("user_name")
        // Rol devuelto por el backend: `ROLE_USER`, `ROLE_OPERATOR` o `ROLE_ADMIN`.
        val USER_ROLE = stringPreferencesKey("user_role")
        // Bandera de onboarding; se conserva aunque se cierre sesión.
        val ONBOARDING_SHOWN = booleanPreferencesKey("onboarding_shown")
    }

    /**
     * Indica si existe un token de acceso no vacío.
     *
     * Es un `Flow` frío: cada colector recibe el valor actual y las actualizaciones
     * posteriores de DataStore. Solo comprueba presencia, no vigencia, firma ni
     * autorización; un token expirado puede seguir emitiendo `true` hasta que una
     * llamada HTTP reciba 401 o se ejecute `clearSession()`. No sustituye la
     * validación que hace el backend.
     */
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[Keys.ACCESS_TOKEN].isNullOrBlank()
    }

    /**
     * Lee el token de acceso actual.
     *
     * Usa `first()` para tomar una instantánea y suspender hasta que DataStore termine
     * de leer el archivo. Lo invoca `AuthInterceptor` antes de cada petición HTTP.
     * El valor es sensible: no debe registrarse en logs ni exponerse en la UI.
     *
     * @return el token JWT guardado, o `null` si no hay sesión.
     */
    suspend fun getAccessToken(): String? {
        return context.dataStore.data.first()[Keys.ACCESS_TOKEN]
    }

    /**
     * Lee el refresh token actual.
     *
     * Se usa para renovar la sesión contra `POST /api/auth/refresh` o para revocarla
     * en `POST /api/auth/logout`. Es una credencial de larga duración y tampoco debe
     * registrarse.
     *
     * @return el refresh token guardado, o `null` si no existe.
     */
    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.first()[Keys.REFRESH_TOKEN]
    }

    /**
     * Persiste una sesión completa en una única transacción.
     *
     * `edit` aplica todos los cambios de forma atómica: si la escritura falla, no se
     * deja una sesión parcial. Sobrescribe cualquier sesión previa. El repositorio
     * compone `name` como `firstName lastName` y guarda el rol tal como llega del
     * backend (`ROLE_USER`, `ROLE_OPERATOR` o `ROLE_ADMIN`); la UI lo interpreta con
     * comparaciones tolerantes a mayúsculas y a las variantes en español.
     *
     * @param accessToken token JWT que se enviará en `Authorization: Bearer`.
     * @param refreshToken token de larga duración para renovar la sesión.
     * @param userId identificador interno del usuario.
     * @param email correo de la cuenta.
     * @param name nombre visible para mostrar en la interfaz.
     * @param role rol devuelto por el backend; se guarda como texto.
     */
    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: Long,
        email: String,
        name: String,
        role: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            prefs[Keys.REFRESH_TOKEN] = refreshToken
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USER_EMAIL] = email
            prefs[Keys.USER_NAME] = name
            prefs[Keys.USER_ROLE] = role
        }
    }

    /**
     * Elimina las claves de sesión en una única transacción.
     *
     * No borra `onboarding_shown`: cerrar sesión no debe obligar a ver otra vez el
     * onboarding. Tampoco revoca el refresh token en el servidor; de eso se encarga
     * `AuthRepositoryImpl.logout()` antes de llamar aquí. Si la revocación remota falla,
     * la sesión local se limpia igualmente para no dejar credenciales en el dispositivo.
     */
    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_EMAIL)
            prefs.remove(Keys.USER_NAME)
            prefs.remove(Keys.USER_ROLE)
        }
    }

    /**
     * Indica si el onboarding ya fue completado.
     *
     * Emite `false` cuando la clave no existe, de modo que una instalación nueva vea
     * el onboarding. El valor persiste entre sesiones y solo se reinicia al borrar los
     * datos de la aplicación o reinstalarla.
     */
    val onboardingShown: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_SHOWN] ?: false
    }

    /**
     * Marca el onboarding como mostrado.
     *
     * La operación es idempotente: escribir `true` varias veces no cambia el resultado.
     * No existe un método público para volver a `false`; eso evita que la pantalla de
     * bienvenida reaparezca por accidente.
     */
    suspend fun setOnboardingShown() {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_SHOWN] = true
        }
    }

    /**
     * Expone la identidad mínima del usuario para la UI.
     *
     * Devuelve `Triple(email, name, role)` o `null` si no hay correo guardado. El rol
     * usa `"CLIENTE"` como respaldo local cuando la clave está ausente; el backend
     * suele enviar `ROLE_USER`, por lo que los consumidores deben comparar de forma
     * tolerante. No incluye `userId` ni tokens: esos datos quedan fuera de la UI.
     *
     * @return `Flow` con la tupla de identidad, o `null` cuando no hay sesión.
     */
    fun getUserInfo(): Flow<Triple<String, String, String>?> = context.dataStore.data.map { prefs ->
        val email = prefs[Keys.USER_EMAIL]
        if (!email.isNullOrBlank()) {
            val name = prefs[Keys.USER_NAME] ?: ""
            val role = prefs[Keys.USER_ROLE] ?: "CLIENTE"
            Triple(email, name, role)
        } else null
    }
}
