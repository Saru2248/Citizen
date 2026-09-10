package com.citizenai.worker.presentation.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.worker.data.remote.WorkerSocketManager
import com.citizenai.worker.domain.model.WorkerUser
import com.citizenai.worker.domain.repository.AuthRepository
import com.citizenai.worker.domain.repository.OtpSendResult
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LoginStep {
    ENTER_PHONE,
    ENTER_OTP
}

data class LoginUiState(
    val step: LoginStep = LoginStep.ENTER_PHONE,
    val workerIdInput: String = "",
    val mobileNumberInput: String = "",
    val otpInput: String = "",
    val maskedPhoneNumber: String = "",
    val verificationId: String? = null,
    val resendCountdown: Int = 30,
    val isResendEnabled: Boolean = false,
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

    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var countdownJob: Job? = null

    fun onWorkerIdChanged(value: String) {
        _uiState.value = _uiState.value.copy(workerIdInput = value, errorMessage = null)
    }

    fun onMobileNumberChanged(value: String) {
        val filtered = value.filter { it.isDigit() }.take(10)
        _uiState.value = _uiState.value.copy(mobileNumberInput = filtered, errorMessage = null)
    }

    fun onOtpChanged(value: String) {
        val filtered = value.filter { it.isDigit() }.take(6)
        _uiState.value = _uiState.value.copy(otpInput = filtered, errorMessage = null)
        if (filtered.length == 6 && !_uiState.value.isLoading) {
            verifyOtp()
        }
    }

    fun onBackToPhone() {
        countdownJob?.cancel()
        _uiState.value = _uiState.value.copy(
            step = LoginStep.ENTER_PHONE,
            otpInput = "",
            errorMessage = null,
            isLoading = false
        )
    }

    private fun maskPhoneNumber(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        val last10 = digits.takeLast(10)
        return if (last10.length >= 10) {
            val first5 = last10.substring(0, 5)
            "+91 $first5 XXXXX"
        } else {
            "+91 XXXXX XXXXX"
        }
    }

    fun sendOtp(activity: Activity) {
        val state = _uiState.value
        val workerId = state.workerIdInput.trim()
        val rawNumber = state.mobileNumberInput.trim()

        if (workerId.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter your Worker ID")
            return
        }

        if (rawNumber.length < 10) {
            _uiState.value = state.copy(errorMessage = "Please enter a valid 10-digit mobile number")
            return
        }

        if (state.isLoading) return

        val fullNumber = "+91$rawNumber"
        val masked = maskPhoneNumber(rawNumber)

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)

            // Step 1: Pre-validate that Worker ID and Mobile Number pair belong together in MongoDB
            val validationResult = authRepository.validateWorkerCredentials(workerId, fullNumber)
            if (validationResult.isFailure) {
                val err = validationResult.exceptionOrNull()?.message
                    ?: "Worker ID does not match the registered mobile number."
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = err
                )
                return@launch
            }

            // Step 2: Dispatch Firebase SMS OTP
            authRepository.sendOtp(
                activity = activity,
                mobileNumber = fullNumber,
                resendToken = resendToken
            ) { result ->
                viewModelScope.launch {
                    when (result) {
                        is OtpSendResult.CodeSent -> {
                            resendToken = result.token
                            _uiState.value = _uiState.value.copy(
                                step = LoginStep.ENTER_OTP,
                                verificationId = result.verificationId,
                                maskedPhoneNumber = masked,
                                isLoading = false,
                                otpInput = "",
                                errorMessage = null
                            )
                            startCountdown()
                        }
                        is OtpSendResult.AutoVerified -> {
                            handleSuccess(result.user)
                        }
                        is OtpSendResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun resendOtp(activity: Activity) {
        if (!_uiState.value.isResendEnabled || _uiState.value.isLoading) return
        sendOtp(activity)
    }

    fun verifyOtp() {
        val state = _uiState.value
        val code = state.otpInput.trim()
        val workerId = state.workerIdInput.trim()

        if (code.length != 6) {
            _uiState.value = state.copy(errorMessage = "Please enter the complete 6-digit OTP")
            return
        }
        val vId = state.verificationId
        if (vId.isNullOrEmpty()) {
            _uiState.value = state.copy(errorMessage = "Session expired. Please request a new OTP.")
            return
        }

        if (state.isLoading) return

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = null)
            val result = authRepository.verifyOtp(vId, code, workerId)
            result.fold(
                onSuccess = { user ->
                    handleSuccess(user)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message ?: "Invalid OTP code. Please try again."
                    )
                }
            )
        }
    }

    private fun handleSuccess(user: WorkerUser) {
        countdownJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
        socketManager.connect(user.token, user.firebaseUid ?: user.id)
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(resendCountdown = 30, isResendEnabled = false)
            for (sec in 29 downTo 0) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    resendCountdown = sec,
                    isResendEnabled = sec == 0
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
