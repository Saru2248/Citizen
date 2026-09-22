package com.citizenai.worker.data.repository

import android.app.Activity
import android.util.Log
import com.citizenai.worker.BuildConfig
import com.citizenai.worker.data.local.SessionManager
import com.citizenai.worker.data.local.WorkerTaskDao
import com.citizenai.worker.data.remote.WorkerApiService
import com.citizenai.worker.data.remote.dto.*
import com.citizenai.worker.domain.model.WorkerUser
import com.citizenai.worker.domain.repository.AuthRepository
import com.citizenai.worker.domain.repository.OtpSendResult
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: WorkerApiService,
    private val sessionManager: SessionManager,
    private val firebaseAuth: FirebaseAuth,
    private val taskDao: WorkerTaskDao
) : AuthRepository {

    private val TAG = "WorkerAuth"

    override val isLoggedIn: Flow<Boolean> = sessionManager.isLoggedIn
    override val workerUser: Flow<WorkerUser?> = sessionManager.workerUser

    private fun normalizePhoneNumber(phone: String): String {
        var cleaned = phone.replace(Regex("[^0-9+]"), "")
        if (cleaned.startsWith("0")) cleaned = cleaned.substring(1)
        if (!cleaned.startsWith("+")) {
            cleaned = if (cleaned.length == 10) "+91$cleaned" else "+$cleaned"
        }
        return cleaned
    }

    private fun parseErrorMessage(errorBody: String?): String? {
        if (errorBody.isNullOrEmpty()) return null
        return try {
            val json = Gson().fromJson(errorBody, JsonObject::class.java)
            json.get("message")?.asString
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun validateWorkerCredentials(
        workerId: String,
        mobileNumber: String
    ): Result<Boolean> {
        return try {
            val normalized = normalizePhoneNumber(mobileNumber)
            val response = apiService.validateWorker(
                ValidateWorkerRequestDto(
                    workerId = workerId.trim(),
                    mobileNumber = normalized
                )
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Log.i(TAG, "[WORKER AUTH] Worker credentials pre-validated for: ${workerId.trim()} -> $normalized")
                Result.success(true)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = parseErrorMessage(errorBody)
                    ?: "Worker ID does not match the registered mobile number."
                Log.w(TAG, "[WORKER AUTH] Validation rejected: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "[WORKER AUTH] Validation error: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun sendOtp(
        activity: Activity,
        mobileNumber: String,
        resendToken: PhoneAuthProvider.ForceResendingToken?,
        onResult: (OtpSendResult) -> Unit
    ) {
        val normalizedNumber = normalizePhoneNumber(mobileNumber)
        Log.i(TAG, "[WORKER OTP] SMS requested for mobile number: $normalizedNumber")

        val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(normalizedNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.i(TAG, "[WORKER OTP] Firebase verification successful (auto-retrieval)")
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = authenticateWithCredential(credential)
                        result.fold(
                            onSuccess = { user -> onResult(OtpSendResult.AutoVerified(user)) },
                            onFailure = { err -> onResult(OtpSendResult.Error(err.message ?: "Authentication failed")) }
                        )
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e(TAG, "[WORKER OTP] Verification failed: ${e.message}", e)
                    onResult(OtpSendResult.Error(e.message ?: "Failed to send SMS OTP. Please try again."))
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.i(TAG, "[WORKER OTP] SMS requested - code sent. Verification ID: $verificationId")
                    onResult(OtpSendResult.CodeSent(verificationId, token))
                }
            })

        if (resendToken != null) {
            optionsBuilder.setForceResendingToken(resendToken)
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    override suspend fun verifyOtp(
        verificationId: String,
        otpCode: String,
        workerId: String?
    ): Result<WorkerUser> {
        val credential = PhoneAuthProvider.getCredential(verificationId, otpCode)
        return authenticateWithCredential(credential, workerId)
    }

    private suspend fun authenticateWithCredential(
        credential: PhoneAuthCredential,
        workerId: String? = null
    ): Result<WorkerUser> {
        return try {
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Authentication failed: Empty Firebase user"))

            Log.i(TAG, "[WORKER OTP] Firebase verification successful")

            val idToken = firebaseUser.getIdToken(true).await()?.token
            if (idToken.isNullOrEmpty()) {
                Log.e(TAG, "[WORKER AUTH] Worker rejected: Missing Firebase ID token")
                firebaseAuth.signOut()
                return Result.failure(Exception("Failed to acquire authentication token from Firebase"))
            }

            Log.i(TAG, "[WORKER AUTH] Firebase token received")

            val reqDto = VerifyWorkerOtpRequestDto(
                idToken = idToken,
                workerId = workerId,
                phone = firebaseUser.phoneNumber
            )
            val response = apiService.verifyWorkerOtp("Bearer $idToken", reqDto)
            if (response.isSuccessful && response.body() != null && response.body()!!.success) {
                val userDto = response.body()!!.data
                if (userDto == null) {
                    Log.e(TAG, "[WORKER AUTH] Worker rejected: Empty user data in response")
                    firebaseAuth.signOut()
                    return Result.failure(Exception("Failed to load worker profile from server"))
                }

                val role = userDto.role ?: "WORKER"
                if (role.uppercase() != "WORKER" && role.uppercase() != "FIELD_WORKER") {
                    Log.e(TAG, "[WORKER AUTH] Worker rejected: Unauthorized role $role")
                    firebaseAuth.signOut()
                    return Result.failure(Exception("Access denied: Worker role required for this application."))
                }

                val workerUser = WorkerUser(
                    id = userDto.id ?: firebaseUser.uid,
                    workerId = userDto.workerId ?: ("WRK-" + (userDto.id ?: "1001").takeLast(4)),
                    name = userDto.name ?: "Worker",
                    email = userDto.email,
                    phone = userDto.phone ?: firebaseUser.phoneNumber,
                    mobileNumber = userDto.mobileNumber ?: firebaseUser.phoneNumber,
                    role = role,
                    firebaseUid = userDto.firebaseUid ?: firebaseUser.uid,
                    department = userDto.department,
                    departmentId = userDto.departmentId,
                    employeeId = userDto.employeeId,
                    accountStatus = userDto.accountStatus ?: "ACTIVE",
                    isActive = userDto.isActive ?: true,
                    lastOtpVerifiedAt = userDto.lastOtpVerifiedAt ?: java.time.Instant.now().toString(),
                    totalLogins = userDto.totalLogins ?: 0,
                    tasksCompleted = userDto.tasksCompleted ?: 0,
                    tasksInProgress = userDto.tasksInProgress ?: 0,
                    token = idToken
                )

                sessionManager.saveSession(idToken, workerUser)
                Log.i(TAG, "[WORKER AUTH] Worker authorized: ${workerUser.name} (${workerUser.workerId}, Logins: ${workerUser.totalLogins})")
                Result.success(workerUser)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMsg = parseErrorMessage(errorBody)
                    ?: "This mobile number is not registered as an authorized worker."
                Log.e(TAG, "[WORKER AUTH] Worker rejected: HTTP ${response.code()} - $errorMsg")
                firebaseAuth.signOut()
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "[WORKER AUTH] Worker rejected: ${e.message}", e)
            firebaseAuth.signOut()
            Result.failure(e)
        }
    }

    override suspend fun login(emailOrEmployeeId: String, password: String): Result<WorkerUser> {
        return try {
            val identifier = emailOrEmployeeId.trim()
            Log.i(TAG, "[WORKER AUTH] Attempting login for identifier: $identifier (Env: ${BuildConfig.API_ENVIRONMENT}, Base: ${BuildConfig.API_BASE_URL})")

            // Pre-flight backend connectivity and health probe
            try {
                val healthResponse = apiService.checkHealth()
                if (!healthResponse.isSuccessful) {
                    if (healthResponse.code() == 503) {
                        return Result.failure(Exception("Backend service is unavailable: Database is not connected (HTTP 503)."))
                    } else if (healthResponse.code() == 404) {
                        Log.w(TAG, "[WORKER AUTH] Health endpoint returned 404, proceeding to auth endpoint")
                    }
                }
            } catch (e: Exception) {
                val diagnosticError = resolveNetworkException(e)
                Log.e(TAG, "[WORKER AUTH] Backend connectivity check failed: $diagnosticError", e)
                return Result.failure(Exception(diagnosticError))
            }

            val response = apiService.workerLogin(WorkerLoginRequestDto(identifier, password))
            if (response.isSuccessful && response.body() != null) {
                val res = response.body()!!
                val token = res.token
                val userDto = res.user ?: res.data

                if (token.isNullOrEmpty() || userDto == null) {
                    Log.e(TAG, "[WORKER AUTH] Login failed: Missing token or user in server response")
                    return Result.failure(Exception(res.message ?: "Invalid login response from server"))
                }

                val role = userDto.role ?: "WORKER"
                if (role.uppercase() != "WORKER" && role.uppercase() != "FIELD_WORKER") {
                    Log.e(TAG, "[WORKER AUTH] Access denied: User role is '$role' (Worker role required)")
                    return Result.failure(Exception("Access denied: Worker role required for this application."))
                }

                val workerUser = WorkerUser(
                    id = userDto.id ?: "",
                    workerId = userDto.workerId ?: ("WRK-" + (userDto.id ?: "1001").takeLast(4)),
                    name = userDto.name ?: "Worker",
                    email = userDto.email ?: identifier,
                    phone = userDto.phone,
                    mobileNumber = userDto.mobileNumber ?: userDto.phone,
                    role = role,
                    firebaseUid = userDto.firebaseUid,
                    department = userDto.department,
                    departmentId = userDto.departmentId,
                    employeeId = userDto.employeeId,
                    accountStatus = userDto.accountStatus ?: "ACTIVE",
                    isActive = userDto.isActive ?: true,
                    lastOtpVerifiedAt = userDto.lastOtpVerifiedAt ?: java.time.Instant.now().toString(),
                    tasksCompleted = userDto.tasksCompleted ?: 0,
                    tasksInProgress = userDto.tasksInProgress ?: 0,
                    token = token
                )

                sessionManager.saveSession(token, workerUser)
                Log.i(TAG, "[WORKER AUTH] Worker authorized: ${workerUser.name} (${workerUser.workerId})")
                Result.success(workerUser)
            } else {
                val rawError = response.errorBody()?.string()
                val parsedMessage = try {
                    if (!rawError.isNullOrBlank()) {
                        val json = org.json.JSONObject(rawError)
                        json.optString("message", null)
                    } else null
                } catch (_: Exception) {
                    null
                }

                val finalError = parsedMessage ?: when (response.code()) {
                    401 -> "Invalid Worker ID/Mobile Number or Password"
                    403 -> "Your worker account is inactive. Contact your administrator."
                    404 -> "Authentication endpoint not found (HTTP 404). Check backend API routing."
                    500 -> "Internal server error (HTTP 500). Please try again later."
                    502, 503, 504 -> "Backend service temporarily unavailable (HTTP ${response.code()})."
                    else -> "Login failed (HTTP ${response.code()}). Please try again."
                }
                Log.e(TAG, "[WORKER AUTH] Worker rejected: HTTP ${response.code()} Error: $finalError")
                Result.failure(Exception(finalError))
            }
        } catch (e: Exception) {
            val diagError = resolveNetworkException(e)
            Log.e(TAG, "[WORKER AUTH] Worker network exception: ${e.javaClass.simpleName} - $diagError", e)
            Result.failure(Exception(diagError))
        }
    }

    private fun resolveNetworkException(e: Throwable): String {
        return when (BuildConfig.API_ENVIRONMENT) {
            "LOCAL_DEBUG_USB" -> when (e) {
                is java.net.ConnectException ->
                    "Unable to connect to backend on 127.0.0.1:8000. Ensure the Node backend is running and 'adb reverse tcp:8000 tcp:8000' is executed."
                is java.net.SocketTimeoutException ->
                    "Connection timed out waiting for 127.0.0.1:8000. Check backend responsiveness and ADB reverse tunnel."
                is java.net.UnknownHostException ->
                    "Cannot resolve 127.0.0.1. Please verify USB connection."
                is java.io.IOException ->
                    "USB network error (${e.javaClass.simpleName}). Please check ADB reverse connection."
                else -> e.message ?: "Network error occurred."
            }
            "LOCAL_DEBUG_LAN" -> when (e) {
                is java.net.ConnectException, is java.net.UnknownHostException ->
                    "LAN host unreachable (${BuildConfig.API_BASE_URL}). Ensure phone and computer are on the same Wi-Fi network and check CITIZEN_AI_DEV_LAN_HOST."
                is java.net.SocketTimeoutException ->
                    "Connection timed out connecting to LAN host (${BuildConfig.API_BASE_URL}). Check Wi-Fi connection and firewall rules."
                is java.io.IOException ->
                    "LAN network error (${e.javaClass.simpleName}). Please check your local network connection."
                else -> e.message ?: "Network error occurred."
            }
            else -> when (e) {
                is java.net.ConnectException ->
                    "Unable to connect to server. Please check your internet connection."
                is java.net.SocketTimeoutException ->
                    "Server connection timed out. Please check your network connection and try again."
                is java.net.UnknownHostException ->
                    "Server host not found. Please check your internet connection."
                is java.io.IOException ->
                    "Network error (${e.javaClass.simpleName}). Please check your connection."
                else -> e.message ?: "Authentication failed."
            }
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
        sessionManager.clearSession()
        runCatching { taskDao.clearAll() }
    }
}
