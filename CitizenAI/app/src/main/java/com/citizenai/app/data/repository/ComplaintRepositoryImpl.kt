package com.citizenai.app.data.repository

import com.citizenai.app.data.local.dao.ComplaintDao
import com.citizenai.app.data.remote.ApiService
import com.citizenai.app.data.remote.firebase.FirebaseStorageService
import com.citizenai.app.data.remote.firebase.FirestoreService
import com.citizenai.app.domain.model.Comment
import com.citizenai.app.domain.model.Complaint
import com.citizenai.app.domain.model.ComplaintStatus
import com.citizenai.app.domain.model.IssueCategory
import com.citizenai.app.domain.model.TimelineEvent
import com.citizenai.app.domain.model.UserRole
import com.citizenai.app.domain.model.buildTimeline
import com.citizenai.app.domain.repository.ComplaintRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import java.io.File
import java.time.Instant
import javax.inject.Inject

/**
 * ComplaintRepositoryImpl — Node.js REST API + Room DB + Cloud Firestore complaint operations.
 */
class ComplaintRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val complaintDao: ComplaintDao,
    private val firestoreService: FirestoreService,
    private val storageService: FirebaseStorageService,
    private val firebaseAuth: com.google.firebase.auth.FirebaseAuth
) : ComplaintRepository {

    override suspend fun getMyComplaints(): Result<List<Complaint>> {
        val currentUserId = firebaseAuth.currentUser?.uid ?: ""
        val apiUrl = "${com.citizenai.app.BuildConfig.API_BASE_URL}complaints/my"

        return try {
            val apiRes = apiService.getMyComplaints()
            val code = apiRes.code()
            if (apiRes.isSuccessful && apiRes.body() != null) {
                val dtos = apiRes.body()!!
                val domainComplaints = dtos.map { it.toDomain() }
                if (currentUserId.isNotBlank()) {
                    val entities = domainComplaints.map { it.toEntity(localOwnerId = currentUserId) }
                    complaintDao.insertAll(entities)
                }
                Result.success(domainComplaints)
            } else if (code == 401 || code == 403) {
                Log.w("ComplaintRepo", "Authentication failure (HTTP $code) on $apiUrl")
                Result.failure(Exception("Session expired or unauthorized (HTTP $code). Please sign in again."))
            } else {
                val errorBody = runCatching { apiRes.errorBody()?.string() }.getOrNull() ?: apiRes.message()
                Log.w("ComplaintRepo", "Server error (HTTP $code) on $apiUrl: $errorBody")
                val cached = if (currentUserId.isNotBlank()) complaintDao.getByCitizenId(currentUserId) else emptyList()
                if (cached.isNotEmpty()) {
                    Log.i("ComplaintRepo", "Serving ${cached.size} cached complaints after HTTP $code")
                    Result.success(cached.map { it.toDomain() })
                } else {
                    Result.failure(Exception("Server returned HTTP $code: $errorBody"))
                }
            }
        } catch (e: Exception) {
            Log.e("ComplaintRepo", "Network exception accessing $apiUrl: ${e.javaClass.simpleName} - ${e.message}")
            val cached = if (currentUserId.isNotBlank()) complaintDao.getByCitizenId(currentUserId) else emptyList()
            if (cached.isNotEmpty()) {
                Log.i("ComplaintRepo", "Serving ${cached.size} cached complaints after network failure")
                Result.success(cached.map { it.toDomain() })
            } else {
                val errorMsg = when {
                    e is java.net.ConnectException || e.message?.contains("Failed to connect") == true ->
                        "Cannot connect to server at $apiUrl. Please verify backend is running and network is connected."
                    e is java.net.SocketTimeoutException ->
                        "Connection timed out waiting for server ($apiUrl). If connecting over Wi-Fi LAN, ensure port 8000 is open in your PC firewall; for USB, verify 'adb reverse tcp:8000 tcp:8000'."
                    e is java.net.UnknownHostException ->
                        "Could not resolve server host ($apiUrl)."
                    else -> e.message ?: "Network error: ${e.javaClass.simpleName}"
                }
                Result.failure(Exception(errorMsg, e))
            }
        }
    }

    override suspend fun getComplaintById(id: String): Result<Complaint> {
        val currentUserId = firebaseAuth.currentUser?.uid ?: ""
        return try {
            val apiRes = apiService.getComplaintById(id)
            if (apiRes.isSuccessful && apiRes.body() != null) {
                val complaint = apiRes.body()!!.toDomain()
                complaintDao.insert(complaint.toEntity(localOwnerId = currentUserId))
                Result.success(complaint)
            } else {
                val cached = complaintDao.getById(id)
                if (cached != null) {
                    Result.success(cached.toDomain())
                } else {
                    Result.failure(Exception("Complaint not found (HTTP ${apiRes.code()})"))
                }
            }
        } catch (e: Exception) {
            val cached = complaintDao.getById(id)
            if (cached != null) Result.success(cached.toDomain())
            else Result.failure(Exception("Could not load complaint: ${e.message}"))
        }
    }

    override suspend fun getComplaintTimeline(complaintId: String): Result<List<TimelineEvent>> {
        return try {
            val apiRes = apiService.getComplaintTimeline(complaintId)
            if (apiRes.isSuccessful && apiRes.body() != null) {
                val events = apiRes.body()!!.map { it.toDomain() }
                Result.success(events)
            } else {
                val status = getCachedComplaintStatus(complaintId)
                Result.success(buildTimeline(status))
            }
        } catch (e: Exception) {
            val status = getCachedComplaintStatus(complaintId)
            Result.success(buildTimeline(status))
        }
    }

    private suspend fun getCachedComplaintStatus(complaintId: String): ComplaintStatus {
        val cached = complaintDao.getById(complaintId)
        return if (cached != null) {
            runCatching { ComplaintStatus.valueOf(cached.status) }.getOrDefault(ComplaintStatus.REPORTED)
        } else {
            ComplaintStatus.REPORTED
        }
    }

    override suspend fun getComments(complaintId: String): Result<List<Comment>> {
        return Result.success(emptyList())
    }

    override suspend fun postComment(complaintId: String, message: String): Result<Comment> {
        val currentUser = firebaseAuth.currentUser
        val comment = Comment(
            id = "cmt_${System.currentTimeMillis()}",
            complaintId = complaintId,
            authorId = currentUser?.uid ?: "user_current",
            authorName = currentUser?.displayName ?: "User",
            authorRole = UserRole.CITIZEN,
            message = message,
            postedAt = Instant.now()
        )
        return Result.success(comment)
    }

    override suspend fun submitComplaint(
        imagePath: String,
        category: IssueCategory,
        description: String,
        latitude: Double,
        longitude: Double,
        address: String,
        issueType: String,
        priority: String,
        department: String
    ): Result<Complaint> = withContext(Dispatchers.IO) {
        val apiUrl = "${com.citizenai.app.BuildConfig.API_BASE_URL}complaints"
        Log.d("CitizenAI_Submit", "[COMPLAINT SUBMISSION] Request start -> POST $apiUrl")
        Log.d("CitizenAI_Submit", "[COMPLAINT SUBMISSION] category=${category.name}, priority=$priority, dept=$department, coords=($latitude, $longitude), address=$address, imagePath=$imagePath")

        try {
            val file = File(imagePath)
            val imagePart: MultipartBody.Part = if (file.exists() && file.length() > 0) {
                val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", file.name, reqFile)
            } else {
                val emptyReqBody = "".toRequestBody("text/plain".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", "", emptyReqBody)
            }

            val categoryPart = category.name.toRequestBody("text/plain".toMediaTypeOrNull())
            val descPart = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val latPart = latitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val lngPart = longitude.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val addrPart = address.toRequestBody("text/plain".toMediaTypeOrNull())
            val issuePart = issueType.toRequestBody("text/plain".toMediaTypeOrNull())
            val priorityPart = priority.toRequestBody("text/plain".toMediaTypeOrNull())
            val deptPart = department.toRequestBody("text/plain".toMediaTypeOrNull())

            val apiRes = apiService.submitComplaint(
                image = imagePart,
                category = categoryPart,
                description = descPart,
                latitude = latPart,
                longitude = lngPart,
                address = addrPart,
                issueType = issuePart,
                priority = priorityPart,
                department = deptPart
            )

            val code = apiRes.code()
            Log.d("CitizenAI_Submit", "[COMPLAINT SUBMISSION] HTTP Response Code: $code")

            if (apiRes.isSuccessful && apiRes.body() != null) {
                val complaintDto = apiRes.body()!!
                Log.d("CitizenAI_Submit", "[COMPLAINT SUBMISSION] Response body: $complaintDto")
                val domainComplaint = complaintDto.toDomain()
                val currentUserId = firebaseAuth.currentUser?.uid ?: ""
                complaintDao.insert(complaintDto.toEntity(localOwnerId = currentUserId))
                Log.d("CitizenAI_Submit", "[COMPLAINT SUBMISSION] Backend returned HTTP $code, ComplaintID=${domainComplaint.complaintId}")
                Result.success(domainComplaint)
            } else {
                val errorBodyStr = apiRes.errorBody()?.string() ?: "Empty error body"
                val errorMsg = "Backend submission failed (HTTP $code: ${apiRes.message()}) - $errorBodyStr"
                Log.e("CitizenAI_Submit", "[COMPLAINT SUBMISSION] $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val userFriendlyMsg = when {
                e is java.net.ConnectException || e.message?.contains("Failed to connect") == true ->
                    "Cannot connect to server at $apiUrl. Please verify backend is running and reachable on this network."
                e is java.net.SocketTimeoutException ->
                    "Connection timed out waiting for server ($apiUrl). Please check backend status."
                else -> e.message ?: "Submission error: ${e.javaClass.simpleName}"
            }
            Log.e("CitizenAI_Submit", "[COMPLAINT SUBMISSION] Exception: ${e.javaClass.simpleName}: ${e.message} -> User message: $userFriendlyMsg", e)
            Result.failure(Exception(userFriendlyMsg, e))
        }
    }

    override suspend fun verifyResolution(
        complaintId: String,
        isResolved: Boolean,
        reason: String?
    ): Result<Unit> {
        val targetStatus = if (isResolved) ComplaintStatus.RESOLVED else ComplaintStatus.REOPENED
        val cached = complaintDao.getById(complaintId)
        if (cached != null) {
            val updatedEntity = cached.copy(
                status = targetStatus.name,
                updatedAt = System.currentTimeMillis()
            )
            complaintDao.insert(updatedEntity)
            runCatching { firestoreService.saveComplaint(updatedEntity.toDomain()) }
        }
        return Result.success(Unit)
    }

    override fun observeCachedComplaints(): Flow<List<Complaint>> {
        val currentUserId = firebaseAuth.currentUser?.uid ?: ""
        return complaintDao.observeByCitizenId(currentUserId).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getAllComplaintsForMap(): Result<List<Complaint>> {
        return getMyComplaints()
    }
}
