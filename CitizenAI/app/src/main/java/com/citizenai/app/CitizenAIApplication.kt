package com.citizenai.app

import android.app.Application
import android.util.Log
import com.citizenai.app.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

/**
 * CitizenAIApplication — Hilt-enabled Application class.
 * Hilt requires this as the entry point for dependency injection.
 * Firebase is initialized here with safe Crashlytics diagnostic keys.
 */
@HiltAndroidApp
class CitizenAIApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initializeFirebase()
    }

    private fun initializeFirebase() {
        // Firebase auto-initializes via google-services.json.
        // This call is safe even if Firebase is already initialized.
        FirebaseApp.initializeApp(this)

        // Crashlytics — add safe diagnostic context.
        // NEVER log: passwords, tokens, personal data, or secrets.
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCustomKey("build_flavor",     BuildConfig.FLAVOR)
        crashlytics.setCustomKey("api_environment",  BuildConfig.API_ENVIRONMENT)
        crashlytics.setCustomKey("app_version",      BuildConfig.VERSION_NAME)
        crashlytics.setCustomKey("debug_build",      BuildConfig.DEBUG)

        // Disable Crashlytics crash reporting for debug builds to avoid noise.
        // Enable it only for staging and production.
        val enableCrashlytics = !BuildConfig.DEBUG ||
            BuildConfig.API_ENVIRONMENT == "STAGING" ||
            BuildConfig.API_ENVIRONMENT == "PRODUCTION"
        crashlytics.isCrashlyticsCollectionEnabled = enableCrashlytics

        Log.d("CitizenAI", "Firebase initialized — flavor=${BuildConfig.FLAVOR} env=${BuildConfig.API_ENVIRONMENT} crashlytics=$enableCrashlytics")
    }
}

