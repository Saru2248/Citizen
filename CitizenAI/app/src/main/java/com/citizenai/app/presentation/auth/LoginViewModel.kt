package com.citizenai.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.app.domain.model.UserRole
import com.citizenai.app.domain.usecase.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isRetryable: Boolean = false,
    val isPasswordVisible: Boolean = false
)

sealed class LoginEvent {
    data class NavigateToCitizenHome(val name: String) : LoginEvent()
    data class NavigateToWorkerHome(val name: String) : LoginEvent()
    data class NavigateToAdminHome(val name: String) : LoginEvent()
    data class ShowError(val message: String) : LoginEvent()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            emailError = null,
            generalError = null,
            isRetryable = false
        )
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            passwordError = null,
            generalError = null,
            isRetryable = false
        )
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(isPasswordVisible = !_uiState.value.isPasswordVisible)
    }

    fun retry() {
        login()
    }

    fun login() {
        val state = _uiState.value

        // Prevent multiple simultaneous requests while loading
        if (state.isLoading) return

        var hasError = false
        var emailErr: String? = null
        var passwordErr: String? = null

        // Field Validation Rules
        if (state.email.isBlank()) {
            emailErr = "Please enter your email or username."
            hasError = true
        }

        if (state.password.isBlank()) {
            passwordErr = "Please enter your password."
            hasError = true
        } else if (state.password.length < 4) {
            passwordErr = "Password must be at least 4 characters."
            hasError = true
        }

        if (hasError) {
            _uiState.value = state.copy(
                emailError = emailErr,
                passwordError = passwordErr,
                generalError = null,
                isRetryable = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(
                isLoading = true,
                generalError = null,
                emailError = null,
                passwordError = null,
                isRetryable = false
            )
            try {
                val result = kotlinx.coroutines.withTimeout(15000L) {
                    loginUseCase(state.email.trim(), state.password)
                }
                result.fold(
                    onSuccess = { user ->
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        when (user.role) {
                            UserRole.CITIZEN -> _events.emit(LoginEvent.NavigateToCitizenHome(user.name))
                            UserRole.WORKER  -> _events.emit(LoginEvent.NavigateToWorkerHome(user.name))
                            UserRole.ADMIN   -> _events.emit(LoginEvent.NavigateToAdminHome(user.name))
                        }
                    },
                    onFailure = { error ->
                        val errMsg = error.message ?: "Unable to sign in. Please check your credentials and network connection."
                        val retryable = errMsg.contains("connect", ignoreCase = true) ||
                                        errMsg.contains("responding", ignoreCase = true) ||
                                        errMsg.contains("network", ignoreCase = true) ||
                                        errMsg.contains("timed out", ignoreCase = true)

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            generalError = errMsg,
                            isRetryable = retryable
                        )
                    }
                )
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    generalError = "Connection timed out. Please check your internet connection and try again.",
                    isRetryable = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    generalError = e.message ?: "An unexpected error occurred during sign in. Please try again.",
                    isRetryable = true
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
