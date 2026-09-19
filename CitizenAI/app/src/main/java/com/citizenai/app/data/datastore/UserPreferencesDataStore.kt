package com.citizenai.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "citizen_ai_prefs")

/**
 * UserPreferencesDataStore — secure session storage using Jetpack DataStore.
 * Stores: access token, user ID, user role, admin level.
 * Does NOT store passwords.
 */
@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ACCESS_TOKEN     = stringPreferencesKey("access_token")
        val USER_ID          = stringPreferencesKey("user_id")
        val USER_NAME        = stringPreferencesKey("user_name")
        val USER_EMAIL       = stringPreferencesKey("user_email")
        val USER_ROLE        = stringPreferencesKey("user_role")
        val ADMIN_LEVEL      = stringPreferencesKey("admin_level")
        val IS_AUTHENTICATED = booleanPreferencesKey("is_authenticated")
        val LANGUAGE         = stringPreferencesKey("app_language")
    }

    fun getAccessToken(): Flow<String?> =
        context.dataStore.data.map { it[Keys.ACCESS_TOKEN] }

    fun getUserId(): Flow<String?> =
        context.dataStore.data.map { it[Keys.USER_ID] }

    fun getUserName(): Flow<String?> =
        context.dataStore.data.map { it[Keys.USER_NAME] }

    fun getUserEmail(): Flow<String?> =
        context.dataStore.data.map { it[Keys.USER_EMAIL] }

    fun getUserRole(): Flow<String?> =
        context.dataStore.data.map { it[Keys.USER_ROLE] }

    fun getAdminLevel(): Flow<String?> =
        context.dataStore.data.map { it[Keys.ADMIN_LEVEL] }

    fun isAuthenticated(): Flow<Boolean> =
        context.dataStore.data.map { it[Keys.IS_AUTHENTICATED] ?: false }

    suspend fun saveSession(
        token: String,
        userId: String,
        userName: String,
        email: String,
        role: String,
        adminLevel: String? = null
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN]     = token
            prefs[Keys.USER_ID]          = userId
            prefs[Keys.USER_NAME]        = userName
            prefs[Keys.USER_EMAIL]       = email
            prefs[Keys.USER_ROLE]        = role
            prefs[Keys.IS_AUTHENTICATED] = true
            if (adminLevel != null) {
                prefs[Keys.ADMIN_LEVEL] = adminLevel
            } else {
                prefs.remove(Keys.ADMIN_LEVEL)
            }
        }
    }

    suspend fun updateUserProfile(userName: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_NAME]  = userName
            prefs[Keys.USER_EMAIL] = email
        }
    }

    fun getLanguage(): Flow<String> =
        context.dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }

    suspend fun setLanguage(languageCode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = languageCode
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_NAME)
            prefs.remove(Keys.USER_EMAIL)
            prefs.remove(Keys.USER_ROLE)
            prefs.remove(Keys.ADMIN_LEVEL)
            prefs[Keys.IS_AUTHENTICATED] = false
            // NOTE: Language is intentionally NOT cleared on logout
        }
    }
}
