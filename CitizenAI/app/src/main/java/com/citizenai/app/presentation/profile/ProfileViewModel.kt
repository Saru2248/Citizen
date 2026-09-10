package com.citizenai.app.presentation.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.app.data.datastore.UserPreferencesDataStore
import com.citizenai.app.domain.usecase.auth.LogoutUseCase
import com.citizenai.app.util.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
    private val dataStore: UserPreferencesDataStore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val userName = dataStore.getUserName().map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val userEmail = dataStore.getUserEmail().map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    /** Currently persisted language code: "en", "mr", or "hi". */
    val currentLanguage: StateFlow<String> = dataStore.getLanguage()
        .stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    fun updateProfile(name: String, email: String) {
        viewModelScope.launch {
            dataStore.updateUserProfile(name, email)
        }
    }

    /**
     * 1. Persist to DataStore (reactive source of truth for UI).
     * 2. Persist to SharedPreferences bootstrap (for attachBaseContext before Hilt is ready).
     * 3. Call AppCompatDelegate via LanguageManager — triggers Activity recreation automatically.
     */
    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            dataStore.setLanguage(languageCode)
            // applyLanguage also writes to SharedPreferences and calls AppCompatDelegate
            LanguageManager.applyLanguage(appContext, languageCode)
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            logoutUseCase()
            onLoggedOut()
        }
    }
}
