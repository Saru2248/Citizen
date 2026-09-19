package com.citizenai.app.data.remote

import com.citizenai.app.data.remote.dto.admin.AdminCommentRequest
import com.citizenai.app.data.remote.dto.admin.AdminStatsDto
import com.citizenai.app.data.remote.dto.admin.AssignWorkerRequest
import com.citizenai.app.data.remote.dto.admin.EscalateRequest
import com.citizenai.app.data.remote.dto.auth.FcmTokenRequest
import com.citizenai.app.data.remote.dto.auth.LoginRequest
import com.citizenai.app.data.remote.dto.auth.LoginResponse
import com.citizenai.app.data.remote.dto.auth.RegisterRequest
import com.citizenai.app.data.remote.dto.complaint.ComplaintDto
import com.citizenai.app.data.remote.dto.complaint.CommentDto
import com.citizenai.app.data.remote.dto.complaint.TimelineEventDto
import com.citizenai.app.data.remote.dto.complaint.VerifyResolutionRequest
import com.citizenai.app.data.remote.dto.ai.AIAnalysisResponse
import com.citizenai.app.data.remote.dto.notification.NotificationDto
import com.citizenai.app.data.remote.dto.worker.CompleteTaskRequest
import com.citizenai.app.data.remote.dto.worker.ProgressUpdateDto
import com.citizenai.app.data.remote.dto.worker.RejectTaskRequest
import com.citizenai.app.data.remote.dto.worker.WorkerMonitorDto
import com.citizenai.app.data.remote.dto.worker.WorkerTaskDto
import com.citizenai.app.data.remote.dto.worker.UpdateTaskStatusRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * ApiService — Retrofit interface declaring all backend endpoints.
 * All endpoints return Response<T> for explicit status code handling.
 *
 * Backend base URL: BuildConfig.API_BASE_URL (10.0.2.2:8000 for emulator)
 */
interface ApiService {

    // ─── Authentication ───────────────────────────────────────────────────────

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<LoginResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<LoginResponse>

    @POST("auth/fcm-token")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest): Response<Unit>

    // ─── Complaints (Citizen) ─────────────────────────────────────────────────

    @Multipart
    @POST("complaints")
    suspend fun submitComplaint(
        @Part image: MultipartBody.Part,
        @Part("category") category: RequestBody,
        @Part("description") description: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("address") address: RequestBody,
        @Part("issueType") issueType: RequestBody,
        @Part("priority") priority: RequestBody,
        @Part("department") department: RequestBody
    ): Response<ComplaintDto>

    @GET("complaints/my")
    suspend fun getMyComplaints(): Response<List<ComplaintDto>>

    @GET("complaints/{id}")
    suspend fun getComplaintById(@Path("id") id: String): Response<ComplaintDto>

    @GET("complaints/{id}/timeline")
    suspend fun getComplaintTimeline(@Path("id") id: String): Response<List<TimelineEventDto>>

    @GET("complaints/{id}/comments")
    suspend fun getComments(@Path("id") id: String): Response<List<CommentDto>>

    @POST("complaints/{id}/comments")
    suspend fun postComment(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<CommentDto>

    @POST("complaints/{id}/verify")
    suspend fun verifyResolution(
        @Path("id") id: String,
        @Body request: VerifyResolutionRequest
    ): Response<Unit>

    @GET("complaints/map")
    suspend fun getAllComplaintsForMap(): Response<List<ComplaintDto>>

    @GET("complaints/{id}/progress")
    suspend fun getProgressUpdates(@Path("id") id: String): Response<List<ProgressUpdateDto>>

    // ─── AI Analysis ─────────────────────────────────────────────────────────

    @Multipart
    @POST("ai/analyze")
    suspend fun analyzeImage(
        @Part image: MultipartBody.Part
    ): Response<AIAnalysisResponse>

    // ─── Worker Endpoints ─────────────────────────────────────────────────────

    @GET("worker/tasks")
    suspend fun getWorkerTasks(): Response<List<WorkerTaskDto>>

    @GET("worker/tasks/{id}")
    suspend fun getWorkerTaskById(@Path("id") id: String): Response<WorkerTaskDto>

    @POST("worker/tasks/{id}/accept")
    suspend fun acceptTask(@Path("id") id: String): Response<WorkerTaskDto>

    @POST("worker/tasks/{id}/reject")
    suspend fun rejectTask(
        @Path("id") id: String,
        @Body request: RejectTaskRequest
    ): Response<Unit>

    @PATCH("worker/tasks/{id}")
    suspend fun updateTaskStatus(
        @Path("id") id: String,
        @Body request: UpdateTaskStatusRequest
    ): Response<WorkerTaskDto>

    @Multipart
    @POST("worker/tasks/{id}/progress")
    suspend fun submitProgressUpdate(
        @Path("id") id: String,
        @Part("progressPercentage") progressPercentage: RequestBody,
        @Part("note") note: RequestBody,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?,
        @Part photo: MultipartBody.Part?
    ): Response<ProgressUpdateDto>

    @Multipart
    @POST("worker/tasks/{id}/complete")
    suspend fun completeTask(
        @Path("id") id: String,
        @Part afterImage: MultipartBody.Part,
        @Part("notes") notes: RequestBody
    ): Response<Unit>

    @GET("worker/tasks/history")
    suspend fun getTaskHistory(): Response<List<WorkerTaskDto>>

    // ─── Admin Endpoints ──────────────────────────────────────────────────────

    @GET("admin/complaints")
    suspend fun getAllComplaints(
        @Query("status") status: String? = null,
        @Query("priority") priority: String? = null,
        @Query("department") department: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<List<ComplaintDto>>

    @POST("admin/complaints/{id}/assign")
    suspend fun assignWorker(
        @Path("id") id: String,
        @Body request: AssignWorkerRequest
    ): Response<ComplaintDto>

    @PATCH("admin/complaints/{id}/priority")
    suspend fun updateComplaintPriority(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    @PATCH("admin/complaints/{id}/department")
    suspend fun updateComplaintDepartment(
        @Path("id") id: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    @POST("admin/complaints/{id}/comment")
    suspend fun postAdminComment(
        @Path("id") id: String,
        @Body request: AdminCommentRequest
    ): Response<CommentDto>

    @POST("admin/complaints/{id}/escalate")
    suspend fun escalateComplaint(
        @Path("id") id: String,
        @Body request: EscalateRequest
    ): Response<Unit>

    @GET("admin/workers")
    suspend fun getAllWorkers(
        @Query("department") department: String? = null,
        @Query("availability") availability: String? = null
    ): Response<List<WorkerMonitorDto>>

    @GET("admin/workers/active")
    suspend fun getActiveWorkers(): Response<List<WorkerMonitorDto>>

    @GET("admin/workers/{id}")
    suspend fun getWorkerById(@Path("id") id: String): Response<WorkerMonitorDto>

    @GET("admin/stats")
    suspend fun getAdminStats(): Response<AdminStatsDto>

    @GET("admin/audit-logs")
    suspend fun getAuditLogs(): Response<List<com.citizenai.app.data.remote.dto.admin.AuditLogDto>>


    // ─── Notifications ────────────────────────────────────────────────────────

    @GET("notifications")
    suspend fun getNotifications(): Response<List<NotificationDto>>

    @PATCH("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<Unit>

    @PATCH("notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<Unit>
}
