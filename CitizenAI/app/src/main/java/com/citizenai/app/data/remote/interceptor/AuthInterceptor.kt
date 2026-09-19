package com.citizenai.app.data.remote.interceptor

import com.citizenai.app.data.datastore.UserPreferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * AuthInterceptor — attaches Bearer token to all API requests.
 * Token is read synchronously from DataStore with timeout.
 * If no token is stored, the request proceeds unauthenticated
 * (the backend will return 401 and the app will redirect to login).
 */
class AuthInterceptor @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            try {
                withTimeoutOrNull(2000L) {
                    userPreferencesDataStore.getAccessToken().firstOrNull()
                }
            } catch (e: Exception) {
                null
            }
        }

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
