package com.citizenai.app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.app.domain.model.UserRole
import com.citizenai.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashDestination {
    object Loading : SplashDestination()
    object Login : SplashDestination()
    object CitizenHome : SplashDestination()
    object WorkerHome : SplashDestination()
    object AdminHome : SplashDestination()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            // Minimum splash duration for brand visibility
            delay(1500)

            val isAuthenticated = authRepository.isAuthenticated().firstOrNull() ?: false
            if (!isAuthenticated) {
                _destination.value = SplashDestination.Login
                return@launch
            }

            val role = authRepository.getStoredUserRole().firstOrNull()
            _destination.value = when (role) {
                UserRole.WORKER  -> SplashDestination.WorkerHome
                UserRole.CITIZEN -> SplashDestination.CitizenHome
                UserRole.ADMIN   -> SplashDestination.AdminHome
                null             -> SplashDestination.Login
            }
        }
    }
}
