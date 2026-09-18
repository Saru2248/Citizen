package com.citizenai.worker.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.citizenai.worker.domain.model.WorkerUser
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "worker_session_prefs")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val WORKER_PROFILE = stringPreferencesKey("worker_profile")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val LAST_OTP_VERIFIED_AT = stringPreferencesKey("last_otp_verified_at")

        fun isCurrentCalendarDay(timestampStr: String?): Boolean {
            if (timestampStr.isNullOrEmpty()) return false
            return try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).apply {
                    timeZone = java.util.TimeZone.getDefault()
                }
                val todayStr = sdf.format(java.util.Date())

                val verifiedDate = try {
                    val instant = java.time.Instant.parse(timestampStr)
                    val zdt = instant.atZone(java.time.ZoneId.systemDefault())
                    zdt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                } catch (e: Exception) {
                    if (timestampStr.length >= 10) timestampStr.substring(0, 10) else ""
                }

                todayStr == verifiedDate
            } catch (e: Exception) {
                false
            }
        }
    }

    val authToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[AUTH_TOKEN]
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        val loggedIn = prefs[IS_LOGGED_IN] ?: false
        val token = prefs[AUTH_TOKEN]
        loggedIn && !token.isNullOrEmpty()
    }

    val workerUser: Flow<WorkerUser?> = context.dataStore.data.map { prefs ->
        val json = prefs[WORKER_PROFILE]
        if (!json.isNullOrEmpty()) {
            try {
                gson.fromJson(json, WorkerUser::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    suspend fun saveSession(token: String, user: WorkerUser) {
        context.dataStore.edit { prefs ->
            prefs[AUTH_TOKEN] = token
            prefs[WORKER_PROFILE] = gson.toJson(user)
            prefs[LAST_OTP_VERIFIED_AT] = user.lastOtpVerifiedAt ?: java.time.Instant.now().toString()
            prefs[IS_LOGGED_IN] = true
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
