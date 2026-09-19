package com.citizenai.worker.data.remote

import com.citizenai.worker.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface WorkerApiService {

    @GET("health")
    suspend fun checkHealth(): Response<Map<String, Any>>

    @POST("worker/auth/validate")
    suspend fun validateWorker(
        @Body request: ValidateWorkerRequestDto
    ): Response<ApiResponseDto<UserDto>>

    @POST("worker/auth/verify")
    suspend fun verifyWorkerOtp(
        @Header("Authorization") authHeader: String,
        @Body request: VerifyWorkerOtpRequestDto = VerifyWorkerOtpRequestDto()
    ): Response<ApiResponseDto<UserDto>>

    @POST("auth/worker/login")
    suspend fun workerLogin(@Body request: WorkerLoginRequestDto): Response<AuthResponseDto>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<AuthResponseDto>

    @GET("worker/dashboard")
    suspend fun getWorkerDashboard(): Response<ApiResponseDto<WorkerDashboardDto>>

    @GET("worker/profile")
    suspend fun getWorkerProfile(): Response<ApiResponseDto<UserDto>>

    @GET("worker/tasks")
    suspend fun getWorkerTasks(): Response<List<ComplaintDto>>

    @GET("worker/tasks/history")
    suspend fun getTaskHistory(): Response<List<ComplaintDto>>

    @GET("worker/tasks/{id}")
    suspend fun getTaskById(@Path("id") taskId: String): Response<ComplaintDto>

    @GET("worker/tasks/{id}/report")
    suspend fun getTaskReport(@Path("id") taskId: String): Response<ApiResponseDto<WorkReportDto>>

    @POST("worker/tasks/{id}/accept")
    suspend fun acceptTask(@Path("id") taskId: String, @Body body: Map<String, String> = emptyMap()): Response<ApiResponseDto<ComplaintDto>>

    @PUT("worker/tasks/{id}/accept")
    suspend fun acceptTaskPut(@Path("id") taskId: String, @Body body: Map<String, String> = emptyMap()): Response<ApiResponseDto<ComplaintDto>>

    @POST("worker/tasks/{id}/reject")
    suspend fun rejectTask(@Path("id") taskId: String, @Body body: Map<String, String>): Response<ApiResponseDto<ComplaintDto>>

    @POST("worker/tasks/{id}/start")
    suspend fun startTask(@Path("id") taskId: String, @Body body: Map<String, String> = emptyMap()): Response<ApiResponseDto<ComplaintDto>>

    @PUT("worker/tasks/{id}/start")
    suspend fun startTaskPut(@Path("id") taskId: String, @Body body: Map<String, String> = emptyMap()): Response<ApiResponseDto<ComplaintDto>>

    @Multipart
    @POST("worker/tasks/{id}/progress")
    suspend fun submitProgress(
        @Path("id") taskId: String,
        @Part("progressPercentage") progressPercentage: RequestBody,
        @Part("note") note: RequestBody,
        @Part photo: MultipartBody.Part? = null
    ): Response<ApiResponseDto<ComplaintDto>>

    @Multipart
    @PUT("worker/tasks/{id}/progress")
    suspend fun submitProgressPut(
        @Path("id") taskId: String,
        @Part("progressPercentage") progressPercentage: RequestBody,
        @Part("note") note: RequestBody,
        @Part photo: MultipartBody.Part? = null
    ): Response<ApiResponseDto<ComplaintDto>>

    @Multipart
    @POST("worker/tasks/{id}/complete")
    suspend fun completeTask(
        @Path("id") taskId: String,
        @Part("notes") notes: RequestBody,
        @Part afterImage: MultipartBody.Part? = null
    ): Response<ApiResponseDto<ComplaintDto>>

    @Multipart
    @PUT("worker/tasks/{id}/complete")
    suspend fun completeTaskPut(
        @Path("id") taskId: String,
        @Part("notes") notes: RequestBody,
        @Part afterImage: MultipartBody.Part? = null
    ): Response<ApiResponseDto<ComplaintDto>>
}
