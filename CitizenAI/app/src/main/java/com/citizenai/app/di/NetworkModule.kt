package com.citizenai.app.di

import android.content.Context
import com.citizenai.app.BuildConfig
import com.citizenai.app.data.remote.interceptor.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): com.google.gson.Gson = com.google.gson.Gson()

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    @Provides
    @Singleton
    fun provideRetryInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request()
        // Do not retry non-GET requests (e.g. multipart POST uploads)
        if (request.method != "GET") {
            return@Interceptor chain.proceed(request)
        }
        var response: Response? = null
        var exception: IOException? = null
        var tryCount = 0
        val maxLimit = 2

        while (response == null && tryCount < maxLimit) {
            try {
                response = chain.proceed(request)
            } catch (e: IOException) {
                exception = e
                tryCount++
                if (tryCount >= maxLimit) {
                    throw e
                }
            }
        }
        response ?: throw exception ?: IOException("Unexpected network error")
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        retryInterceptor: Interceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            val request = chain.request()
            android.util.Log.d("NETWORK", "Connecting to backend... ${request.method} ${request.url}")
            chain.proceed(request)
        })
        .addInterceptor(retryInterceptor)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .retryOnConnectionFailure(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        android.util.Log.d("API CONFIG", "[API CONFIG]\nActive Variant: ${BuildConfig.API_ENVIRONMENT}\nREST Base URL: ${BuildConfig.API_BASE_URL}")
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): com.citizenai.app.data.remote.ApiService =
        retrofit.create(com.citizenai.app.data.remote.ApiService::class.java)
}
