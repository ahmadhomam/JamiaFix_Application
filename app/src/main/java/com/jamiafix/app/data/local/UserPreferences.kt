package com.jamiafix.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jamiafix.app.data.model.UserDto
import com.jamiafix.app.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jamiafix_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("jwt_token")
        private val KEY_USER_ID = intPreferencesKey("user_id")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_ROLE = stringPreferencesKey("user_role")
        private val KEY_BASE_URL = stringPreferencesKey("custom_base_url")

        // USB tunnel via: adb reverse tcp:8000 tcp:8000
        // Phone's 127.0.0.1:8000 → PC's localhost:8000 (works on any WiFi)
        const val DEFAULT_BASE_URL = "http://10.57.1.228:8000/"
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_TOKEN]
    }

    val userRoleFlow: Flow<UserRole> = context.dataStore.data.map { prefs ->
        UserRole.fromString(prefs[KEY_USER_ROLE])
    }

    val currentUserIdFlow: Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }

    val currentUserNameFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME]
    }

    val currentUserEmailFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_EMAIL]
    }

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        !prefs[KEY_TOKEN].isNullOrBlank()
    }

    val baseUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_BASE_URL] ?: DEFAULT_BASE_URL
    }

    suspend fun saveAuthSession(token: String, user: UserDto) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
            prefs[KEY_USER_ID] = user.id
            prefs[KEY_USER_NAME] = user.name
            prefs[KEY_USER_EMAIL] = user.email
            prefs[KEY_USER_ROLE] = user.role
        }
    }

    suspend fun getToken(): String? {
        return context.dataStore.data.first()[KEY_TOKEN]
    }

    suspend fun getUserId(): Int? {
        return context.dataStore.data.first()[KEY_USER_ID]
    }

    suspend fun getUserRole(): UserRole {
        val roleStr = context.dataStore.data.first()[KEY_USER_ROLE]
        return UserRole.fromString(roleStr)
    }

    suspend fun setBaseUrl(url: String) {
        val formatted = if (url.endsWith("/")) url else "$url/"
        context.dataStore.edit { prefs ->
            prefs[KEY_BASE_URL] = formatted
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_USER_NAME)
            prefs.remove(KEY_USER_EMAIL)
            prefs.remove(KEY_USER_ROLE)
        }
    }
}
