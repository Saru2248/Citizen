package com.citizenai.app.data.remote.firebase

import android.util.Log
import com.citizenai.app.domain.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirestoreService"

/**
 * FirestoreService — manages database storage and real-time synchronization in Firebase Firestore.
 *
 * Collections schema:
 *   - users/{userId}             : User profiles (role, name, email, phone, adminLevel)
 *   - complaints/{complaintId}   : Civic complaints & status state machine
 *   - workerTasks/{taskId}       : Tasks assigned to municipal workers
 */
@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    // ─── USER DATA OPERATIONS ──────────────────────────────────────────────────

    suspend fun saveUser(user: User): Result<Unit> {
        return try {
            val userMap = mapOf(
                "id" to user.id,
                "name" to user.name,
                "email" to user.email,
                "phone" to user.phone,
                "role" to user.role.name,
                "adminLevel" to user.adminLevel?.name,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("users")
                .document(user.id)
                .set(userMap, SetOptions.merge())
                .await()
            Log.d(TAG, "Saved user ${user.id} to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save user ${user.id} to Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): Result<User?> {
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (snapshot.exists()) {
                val data = snapshot.data ?: return Result.success(null)
                val roleStr = data["role"] as? String ?: UserRole.CITIZEN.name
                val adminLevelStr = data["adminLevel"] as? String
                val role = runCatching { UserRole.valueOf(roleStr.uppercase()) }.getOrDefault(UserRole.CITIZEN)
                val adminLevel = adminLevelStr?.let { runCatching { AdminLevel.valueOf(it.uppercase()) }.getOrNull() }


                val user = User(
                    id = data["id"] as? String ?: userId,
                    name = data["name"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    phone = data["phone"] as? String ?: "",
                    role = role,
                    adminLevel = adminLevel
                )
                Result.success(user)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get user $userId from Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(userId: String, token: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(userId)
                .update("fcmToken", token)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update FCM token for $userId: ${e.message}")
            Result.failure(e)
        }
    }

    // ─── COMPLAINT DATA OPERATIONS ─────────────────────────────────────────────

    suspend fun saveComplaint(complaint: Complaint): Result<Unit> {
        return try {
            val historyMaps = complaint.statusHistory.map {
                mapOf("status" to it.status, "timestamp" to it.timestamp)
            }
            val map = mapOf(
                "id" to complaint.id,
                "complaintId" to complaint.complaintId,
                "citizenId" to complaint.citizenId,
                "citizenName" to complaint.citizenName,
                "issueType" to complaint.issueType,
                "category" to complaint.category.name,
                "description" to complaint.description,
                "imageUrl" to complaint.imageUrl,
                "afterImageUrl" to complaint.afterImageUrl,
                "completionPhotoUrl" to complaint.completionPhotoUrl,
                "latitude" to complaint.latitude,
                "longitude" to complaint.longitude,
                "address" to complaint.address,
                "status" to complaint.status.name,
                "priority" to complaint.priority.name,
                "department" to complaint.department,
                "assignedWorkerId" to complaint.assignedWorkerId,
                "assignedWorkerName" to complaint.assignedWorkerName,
                "aiConfidence" to complaint.aiConfidence,
                "reportedAt" to complaint.reportedAt.toEpochMilli(),
                "updatedAt" to complaint.updatedAt.toEpochMilli(),
                "resolvedAt" to complaint.resolvedAt?.toEpochMilli(),
                "workerNotes" to complaint.workerNotes,
                "statusHistory" to historyMaps
            )
            firestore.collection("complaints")
                .document(complaint.id)
                .set(map, SetOptions.merge())
                .await()
            Log.d(TAG, "Saved complaint ${complaint.id} to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save complaint ${complaint.id} to Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getComplaints(): Result<List<Complaint>> {
        return try {
            val snapshot = firestore.collection("complaints")
                .get()
                .await()

            val complaints = snapshot.documents.mapNotNull { doc ->
                mapDocToComplaint(doc.data ?: return@mapNotNull null)
            }
            Result.success(complaints)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch complaints from Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getComplaintsForUser(userId: String): Result<List<Complaint>> {
        return try {
            val snapshot = firestore.collection("complaints")
                .whereEqualTo("citizenId", userId)
                .get()
                .await()

            val complaints = snapshot.documents.mapNotNull { doc ->
                mapDocToComplaint(doc.data ?: return@mapNotNull null)
            }.sortedByDescending { it.reportedAt }
            Result.success(complaints)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch user complaints for $userId from Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    fun observeComplaints(): Flow<List<Complaint>> = callbackFlow {
        val listener = firestore.collection("complaints")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed on complaints: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val complaints = snapshot.documents.mapNotNull { doc ->
                        mapDocToComplaint(doc.data ?: return@mapNotNull null)
                    }
                    trySend(complaints)
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeComplaintsForUser(userId: String): Flow<List<Complaint>> = callbackFlow {
        val listener = firestore.collection("complaints")
            .whereEqualTo("citizenId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed on complaints for $userId: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val complaints = snapshot.documents.mapNotNull { doc ->
                        mapDocToComplaint(doc.data ?: return@mapNotNull null)
                    }.sortedByDescending { it.reportedAt }
                    trySend(complaints)
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeComplaint(complaintId: String): Flow<Complaint?> = callbackFlow {
        val listener = firestore.collection("complaints")
            .document(complaintId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed on complaint $complaintId: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(mapDocToComplaint(snapshot.data ?: emptyMap()))
                } else {
                    trySend(null)
                }
            }
        awaitClose { listener.remove() }
    }


    // ─── WORKER TASK OPERATIONS ───────────────────────────────────────────────

    suspend fun saveWorkerTask(task: WorkerTask): Result<Unit> {
        return try {
            val map = mapOf(
                "id" to task.id,
                "complaintId" to task.complaintId,
                "issueType" to task.issueType,
                "category" to task.category.name,
                "address" to task.address,
                "latitude" to task.latitude,
                "longitude" to task.longitude,
                "priority" to task.priority.name,
                "status" to task.status.name,
                "beforeImageUrl" to task.beforeImageUrl,
                "afterImageUrl" to task.afterImageUrl,
                "citizenDescription" to task.citizenDescription,
                "workerNotes" to task.workerNotes,
                "assignedAt" to task.assignedAt.toEpochMilli(),
                "startedAt" to task.startedAt?.toEpochMilli(),
                "completedAt" to task.completedAt?.toEpochMilli(),
                "deadline" to task.deadline?.toEpochMilli(),
                "progressPercentage" to task.progressPercentage,
                "assignedBy" to task.assignedBy,
                "assignedByName" to task.assignedByName,
                "distanceKm" to task.distanceKm
            )
            firestore.collection("workerTasks")
                .document(task.id)
                .set(map, SetOptions.merge())
                .await()
            Log.d(TAG, "Saved worker task ${task.id} to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save worker task ${task.id} to Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    // ─── PRIVATE HELPERS ───────────────────────────────────────────────────────

    private fun mapDocToComplaint(data: Map<String, Any>): Complaint? {
        return try {
            val id = data["id"] as? String ?: return null
            val complaintId = data["complaintId"] as? String ?: id
            val citizenId = data["citizenId"] as? String ?: ""
            val citizenName = data["citizenName"] as? String ?: ""
            val issueType = data["issueType"] as? String ?: ""
            val categoryStr = data["category"] as? String ?: IssueCategory.OTHER.name
            val description = data["description"] as? String ?: ""
            val imageUrl = data["imageUrl"] as? String ?: ""
            val afterImageUrl = data["afterImageUrl"] as? String
            val latitude = (data["latitude"] as? Number)?.toDouble() ?: 0.0
            val longitude = (data["longitude"] as? Number)?.toDouble() ?: 0.0
            val address = data["address"] as? String ?: ""
            val statusStr = data["status"] as? String ?: ComplaintStatus.REPORTED.name
            val priorityStr = data["priority"] as? String ?: Priority.NORMAL.name
            val department = data["department"] as? String ?: ""
            val assignedWorkerId = data["assignedWorkerId"] as? String
            val assignedWorkerName = data["assignedWorkerName"] as? String
            val aiConfidence = (data["aiConfidence"] as? Number)?.toFloat() ?: 0.9f
            val reportedAtEpoch = (data["reportedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            val updatedAtEpoch = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            val resolvedAtEpoch = (data["resolvedAt"] as? Number)?.toLong()
            val workerNotes = data["workerNotes"] as? String
            val rawHistory = data["statusHistory"] as? List<Map<String, Any>> ?: emptyList()
            val statusHistory = rawHistory.map { hMap ->
                com.citizenai.app.domain.model.StatusHistoryEntry(
                    status = hMap["status"] as? String ?: "",
                    timestamp = (hMap["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
            }

            Complaint(
                id = id,
                complaintId = complaintId,
                citizenId = citizenId,
                citizenName = citizenName,
                issueType = issueType,
                category = runCatching { IssueCategory.valueOf(categoryStr) }.getOrDefault(IssueCategory.OTHER),
                description = description,
                imageUrl = imageUrl,
                afterImageUrl = afterImageUrl ?: (data["completionPhotoUrl"] as? String),
                latitude = latitude,
                longitude = longitude,
                address = address,
                status = runCatching { ComplaintStatus.valueOf(statusStr) }.getOrDefault(ComplaintStatus.REPORTED),
                priority = runCatching { Priority.valueOf(priorityStr) }.getOrDefault(Priority.NORMAL),
                department = department,
                assignedWorkerId = assignedWorkerId,
                assignedWorkerName = assignedWorkerName,
                aiConfidence = aiConfidence,
                reportedAt = java.time.Instant.ofEpochMilli(reportedAtEpoch),
                updatedAt = java.time.Instant.ofEpochMilli(updatedAtEpoch),
                resolvedAt = resolvedAtEpoch?.let { java.time.Instant.ofEpochMilli(it) },
                workerNotes = workerNotes,
                statusHistory = statusHistory
            )

        } catch (e: Exception) {
            Log.w(TAG, "Error mapping document to Complaint: ${e.message}")
            null
        }
    }
}
