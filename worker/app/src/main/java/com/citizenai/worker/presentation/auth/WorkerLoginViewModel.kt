package com.citizenai.worker.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.worker.data.remote.WorkerSocketManager
import com.citizenai.worker.domain.model.WorkerUser
import com.citizenai.worker.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val identifierInput: String = "",
    val passwordInput: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class WorkerLoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val socketManager: WorkerSocketManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onIdentifierChanged(value: String) {
        _uiState.value = _uiState.value.copy(identifierInput = value, errorMessage = null)
    }

    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(passwordInput = value, errorMessage = null)
    }

    fun onTogglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(isPasswordVisible = !_uiState.value.isPasswordVisible)
    }

    fun login() {
        val state = _uiState.value
        val identifier = state.identifierInput.trim()
        val password = state.passwordInput

        if (identifier.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter your Worker ID or Mobile Number")
            return
        }

        if (password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter your password")
            return
        }

        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            val result = authRepository.login(identifier, password)
            result.fold(
                onSuccess = { user ->
                    handleSuccess(user)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Invalid Worker ID/Mobile Number or Password"
                    )
                }
            )
        }
    }

    private fun handleSuccess(user: WorkerUser) {
        _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
        val socketRoomId = user.workerId?.takeIf { it.isNotBlank() } ?: user.firebaseUid ?: user.id
        socketManager.connect(user.token, socketRoomId)
    }
}
