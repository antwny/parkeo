package pe.parkeo.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "parkeo_session")

class SessionDataStore(private val context: Context) {

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = longPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_ROLE = stringPreferencesKey("user_role")
        val ONBOARDING_SHOWN = booleanPreferencesKey("onboarding_shown")
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[Keys.ACCESS_TOKEN].isNullOrBlank()
    }

    suspend fun getAccessToken(): String? {
        return context.dataStore.data.first()[Keys.ACCESS_TOKEN]
    }

    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.first()[Keys.REFRESH_TOKEN]
    }

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

    val onboardingShown: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_SHOWN] ?: false
    }

    suspend fun setOnboardingShown() {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_SHOWN] = true
        }
    }

    fun getUserInfo(): Flow<Triple<String, String, String>?> = context.dataStore.data.map { prefs ->
        val email = prefs[Keys.USER_EMAIL]
        val name = prefs[Keys.USER_NAME]
        val role = prefs[Keys.USER_ROLE]
        if (email != null && name != null && role != null) Triple(email, name, role)
        else null
    }
}
