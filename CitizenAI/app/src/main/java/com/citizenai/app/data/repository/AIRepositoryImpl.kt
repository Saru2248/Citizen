package com.citizenai.app.data.repository

import android.util.Log
import com.citizenai.app.BuildConfig
import com.citizenai.app.data.remote.ApiService
import com.citizenai.app.domain.model.AIAnalysisResult
import com.citizenai.app.domain.repository.AIRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AIRepositoryImpl — Real multimodal AI image analysis backed by Gemini Vision
 * via the CitizenAI Node.js backend POST /api/ai/analyze.
 * Strictly avoids hardcoded Pothole fallbacks.
 * Accurately diagnoses and distinguishes root failure categories (A, B, C, D, E).
 */
@Singleton
class AIRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : AIRepository {

    override suspend fun analyzeImage(imageFile: File): Result<AIAnalysisResult> = withContext(Dispatchers.IO) {
        val targetUrl = "${BuildConfig.API_BASE_URL}ai/analyze"
        try {
            // Category D: Invalid image upload validation
            if (!imageFile.exists()) {
                val msg = "[D: INVALID_IMAGE] Image file does not exist: ${imageFile.absolutePath}"
                Log.e("CitizenAI_AI", msg)
                return@withContext Result.failure(IllegalArgumentException("Captured image file could not be found. Please retake the photo."))
            }

            if (imageFile.length() == 0L) {
                val msg = "[D: INVALID_IMAGE] Image file is empty (0 bytes): ${imageFile.name}"
                Log.e("CitizenAI_AI", msg)
                return@withContext Result.failure(IllegalArgumentException("Captured image is empty. Please retake the photo."))
            }

            val mimeType = when (imageFile.extension.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                "heic" -> "image/heic"
                else -> "image/jpeg"
            }

            Log.d(
                "CitizenAI_AI",
                "[AI_ANALYSIS] Uploading image: ${imageFile.name} (${imageFile.length()} bytes, MIME: $mimeType) to $targetUrl [Variant: ${BuildConfig.API_ENVIRONMENT}]"
            )

            val requestBody = imageFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestBody)

            val response = apiService.analyzeImage(imagePart)
            val code = response.code()

            if (response.isSuccessful && response.body() != null) {
                val apiDto = response.body()!!

                // Category E: Response mapping / parsing validation
                val result = try {
                    apiDto.toDomain()
                } catch (parseEx: Exception) {
                    val msg = "[E: PARSE_ERROR] Failed to parse backend AI response: ${parseEx.message}"
                    Log.e("CitizenAI_AI", msg, parseEx)
                    return@withContext Result.failure(Exception("Failed to interpret AI analysis response from server.", parseEx))
                }

                Log.d(
                    "CitizenAI_AI",
                    "[AI_ANALYSIS] HTTP $code Success: detected=${result.detected}, issue=${result.issueType}, severity=${result.severity}, dept=${result.department}, confidence=${result.confidence}"
                )
                Result.success(result)
            } else {
                val rawError = response.errorBody()?.string() ?: ""
                val serverMsg = runCatching {
                    JSONObject(rawError).optString("message", rawError)
                }.getOrDefault(rawError)

                val (categoryTag, userMessage) = when (code) {
                    404 -> "[B: ENDPOINT_NOT_FOUND]" to "AI analysis endpoint not found on server ($targetUrl). Please verify backend routes."
                    400 -> "[D: INVALID_IMAGE]" to "Server rejected image: ${serverMsg.ifBlank { "Invalid image format" }}"
                    500, 502, 503 -> "[C: GEMINI_PROVIDER_ERROR]" to "AI Service Error: ${serverMsg.ifBlank { "Backend Gemini processing failed." }}"
                    401, 403 -> "[AUTH_ERROR]" to "Authentication error: ${serverMsg.ifBlank { "Session invalid." }}"
                    else -> "[HTTP_ERROR_$code]" to "Server error (HTTP $code): ${serverMsg.ifBlank { "Unknown failure" }}"
                }

                Log.e("CitizenAI_AI", "$categoryTag $userMessage | Server HTTP $code: $rawError")
                Result.failure(Exception(userMessage))
            }
        } catch (e: Exception) {
            // Category A: Backend unreachable (connection refused, timeout, host lookup)
            val userFriendlyMsg = when {
                e is ConnectException || e.message?.contains("Failed to connect", ignoreCase = true) == true ->
                    "[A: BACKEND_UNREACHABLE] Cannot connect to server at $targetUrl. If using USB, run 'adb reverse tcp:8000 tcp:8000'. If using LAN, ensure phone and PC are on the same Wi-Fi."
                e is SocketTimeoutException ->
                    "[A: BACKEND_TIMEOUT] Connection timed out waiting for server ($targetUrl). Please check backend status."
                e is IOException ->
                    "[A: NETWORK_IO_ERROR] Network transmission failed: ${e.message ?: "Connection error"}"
                else ->
                    "AI Analysis error: ${e.message ?: e.javaClass.simpleName}"
            }

            Log.e("CitizenAI_AI", "[AI_ANALYSIS_EXCEPTION] $userFriendlyMsg", e)
            Result.failure(Exception(userFriendlyMsg, e))
        }
    }
}
