package com.citizenai.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.app.domain.usecase.auth.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isPasswordVisible: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<Boolean>()
    val events: SharedFlow<Boolean> = _events.asSharedFlow()

    fun onNameChange(v: String) = update { copy(name = v, error = null) }
    fun onEmailChange(v: String) = update { copy(email = v, error = null) }
    fun onPasswordChange(v: String) = update { copy(password = v, error = null) }
    fun onPhoneChange(v: String) = update { copy(phone = v, error = null) }
    fun togglePasswordVisibility() = update { copy(isPasswordVisible = !isPasswordVisible) }

    fun register() {
        val s = _uiState.value
        viewModelScope.launch {
            update { copy(isLoading = true, error = null) }
            try {
                val result = kotlinx.coroutines.withTimeout(15000L) {
                    registerUseCase(s.name, s.email, s.password, s.phone)
                }
                result.fold(
                    onSuccess = {
                        update { copy(isLoading = false) }
                        _events.emit(true)
                    },
                    onFailure = { err ->
                        update { copy(isLoading = false, error = err.message ?: "Registration failed.") }
                    }
                )
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                update { copy(isLoading = false, error = "Connection timed out. Please check your internet connection and try again.") }
            } catch (e: Exception) {
                update { copy(isLoading = false, error = e.message ?: "An error occurred during registration.") }
            } finally {
                update { copy(isLoading = false) }
            }
        }
    }

    private fun update(block: RegisterUiState.() -> RegisterUiState) {
        _uiState.value = _uiState.value.block()
    }
}
