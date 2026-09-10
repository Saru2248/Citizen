package com.citizenai.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.citizenai.app.data.datastore.UserPreferencesDataStore
import com.citizenai.app.data.remote.firebase.FirestoreService
import com.citizenai.app.navigation.AppNavHost
import com.citizenai.app.service.CitizenFcmService
import com.citizenai.app.ui.theme.CitizenAITheme
import com.citizenai.app.util.LanguageManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * MainActivity — Single-activity architecture entry point.
 * Handles notification permission request & FCM token registration on app startup.
 *
 * Language:
 *  [attachBaseContext] reads the persisted language code from a lightweight SharedPreferences
 *  file (written by [LanguageManager.applyLanguage]) before Hilt/DataStore is available,
 *  and wraps the base Context so all resources load in the correct locale.
 *  [AppCompatDelegate.setApplicationLocales] is invoked inside [LanguageManager.applyLanguage]
 *  whenever the user changes language, triggering Activity recreation automatically.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var firestoreService: FirestoreService
    @Inject lateinit var dataStore: UserPreferencesDataStore

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            CitizenFcmService.getAndRegisterCurrentToken(firestoreService, dataStore, lifecycleScope)
        }
    }

    /**
     * Wrap base context with the saved locale BEFORE the Activity window is created.
     * Uses the lightweight [LanguageManager.LANG_PREFS] SharedPreferences — no Hilt needed.
     */
    override fun attachBaseContext(newBase: Context) {
        val langCode = LanguageManager.getSavedLanguageCode(newBase)
        super.attachBaseContext(LanguageManager.applyLocale(newBase, langCode))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Automatically fetch and register FCM token for active user
        CitizenFcmService.getAndRegisterCurrentToken(firestoreService, dataStore, lifecycleScope)

        // Prompt for POST_NOTIFICATIONS runtime permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            CitizenAITheme {
                AppNavHost()
            }
        }
    }
}
