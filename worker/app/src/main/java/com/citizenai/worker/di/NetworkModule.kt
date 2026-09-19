package com.citizenai.worker.di

import android.content.Context
import android.util.Log
import com.citizenai.worker.BuildConfig
import com.citizenai.worker.data.local.SessionManager
import com.citizenai.worker.data.remote.WorkerApiService
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TAG = "WorkerNetwork"

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideFirebaseAuth(): com.google.firebase.auth.FirebaseAuth =
        com.google.firebase.auth.FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        sessionManager: SessionManager,
        firebaseAuth: com.google.firebase.auth.FirebaseAuth
    ): Interceptor {
        return Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            val token = runBlocking {
                val cached = sessionManager.authToken.firstOrNull()
                if (!cached.isNullOrEmpty()) {
                    cached
                } else {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        try {
                            val task = user.getIdToken(false)
                            com.google.android.gms.tasks.Tasks.await(task, 2, TimeUnit.SECONDS)?.token
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                }
            }
            if (!token.isNullOrEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor { message ->
            Log.d(TAG, "[OkHttp] $message")
        }.apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        val baseUrl = BuildConfig.API_BASE_URL
        Log.i(TAG, "[Worker Network] Initializing Retrofit for environment '${BuildConfig.API_ENVIRONMENT}' with API Base URL: $baseUrl")
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideWorkerApiService(retrofit: Retrofit): WorkerApiService {
        return retrofit.create(WorkerApiService::class.java)
    }
}
