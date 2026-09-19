package com.citizenai.app.data.remote.websocket

import com.citizenai.app.domain.model.ComplaintStatus
import com.google.gson.annotations.SerializedName

/**
 * RealTimeEvent — sealed class representing incoming real-time backend events via WebSockets.
 */
sealed class RealTimeEvent {

    data class ComplaintUpdated(
        val complaintId: String,
        val newStatus: ComplaintStatus,
        val updatedBy: String,
        val timestamp: Long
    ) : RealTimeEvent()

    data class WorkerAssigned(
        val complaintId: String,
        val workerId: String,
        val workerName: String,
        val timestamp: Long
    ) : RealTimeEvent()

    data class ProgressReported(
        val complaintId: String,
        val workerId: String,
        val progressPercentage: Int,
        val note: String?,
        val timestamp: Long
    ) : RealTimeEvent()

    data class VerificationRequested(
        val complaintId: String,
        val citizenId: String,
        val timestamp: Long
    ) : RealTimeEvent()

    data class TaskCompleted(
        val complaintId: String,
        val workerId: String,
        val timestamp: Long
    ) : RealTimeEvent()

    object Connected : RealTimeEvent()
    data class Error(val message: String) : RealTimeEvent()
}

/** DTO for parsing raw JSON WebSocket frames. */
data class WebSocketFrameDto(
    @SerializedName("event") val event: String,
    @SerializedName("complaintId") val complaintId: String?,
    @SerializedName("workerId") val workerId: String?,
    @SerializedName("workerName") val workerName: String?,
    @SerializedName("citizenId") val citizenId: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("progressPercentage") val progressPercentage: Int?,
    @SerializedName("note") val note: String?,
    @SerializedName("updatedBy") val updatedBy: String?,
    @SerializedName("timestamp") val timestamp: Long?
)
