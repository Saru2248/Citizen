package com.citizenai.worker.domain.repository

import android.app.Activity
import com.citizenai.worker.domain.model.WorkerUser
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.Flow

sealed class OtpSendResult {
    data class CodeSent(
        val verificationId: String,
        val token: PhoneAuthProvider.ForceResendingToken?
    ) : OtpSendResult()
    data class AutoVerified(val user: WorkerUser) : OtpSendResult()
    data class Error(val message: String) : OtpSendResult()
}

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    val workerUser: Flow<WorkerUser?>

    suspend fun validateWorkerCredentials(
        workerId: String,
        mobileNumber: String
    ): Result<Boolean>

    suspend fun sendOtp(
        activity: Activity,
        mobileNumber: String,
        resendToken: PhoneAuthProvider.ForceResendingToken? = null,
        onResult: (OtpSendResult) -> Unit
    )

    suspend fun verifyOtp(
        verificationId: String,
        otpCode: String,
        workerId: String? = null
    ): Result<WorkerUser>

    suspend fun login(emailOrEmployeeId: String, password: String): Result<WorkerUser>

    suspend fun logout()
}
