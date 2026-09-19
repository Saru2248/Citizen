package com.citizenai.app.data.remote.websocket

import android.util.Log
import com.citizenai.app.data.datastore.UserPreferencesDataStore
import com.citizenai.app.data.local.dao.ComplaintDao
import com.citizenai.app.domain.model.ComplaintStatus
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "WebSocketManager"

/**
 * WebSocketManager — manages persistent WebSocket connection to backend for real-time events.
 * Emits incoming events to UI via SharedFlow.
 * Automatically invalidates/refreshes local Room cache on complaint updates.
 */
@Singleton
class WebSocketManager @Inject constructor(
    private val client: OkHttpClient,
    private val dataStore: UserPreferencesDataStore,
    private val complaintDao: ComplaintDao,
    private val gson: Gson
) {
    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _events = MutableSharedFlow<RealTimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RealTimeEvent> = _events.asSharedFlow()

    private var isConnected = false
    private var reconnectAttempt = 0

    fun connect(wsUrl: String = "wss://api.citizenai.org/ws/events") {
        if (isConnected) return

        scope.launch {
            val token = dataStore.getAccessToken().firstOrNull() ?: return@launch

            val request = Request.Builder()
                .url(wsUrl)
                .addHeader("Authorization", "Bearer $token")
                .build()

            webSocket = client.newWebSocket(request, createListener())
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "User logged out")
        webSocket = null
        isConnected = false
    }

    private fun createListener() = object : WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected successfully")
            isConnected = true
            reconnectAttempt = 0
            scope.launch { _events.emit(RealTimeEvent.Connected) }
        }

        override fun onMessage(ws: WebSocket, text: String) {
            Log.d(TAG, "Received message: $text")
            try {
                val frame = gson.fromJson(text, WebSocketFrameDto::class.java)
                handleFrame(frame)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse frame: ${e.message}")
            }
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure: ${t.message}")
            isConnected = false
            scope.launch {
                _events.emit(RealTimeEvent.Error(t.message ?: "Connection failure"))
                scheduleReconnect()
            }
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $reason")
            isConnected = false
        }
    }

    private fun handleFrame(frame: WebSocketFrameDto) {
        scope.launch {
            val event = when (frame.event.uppercase()) {
                "COMPLAINT_UPDATED" -> {
                    val status = runCatching {
                        ComplaintStatus.valueOf(frame.status?.uppercase() ?: "")
                    }.getOrDefault(ComplaintStatus.REPORTED)

                    // Update room cache status if stored locally
                    frame.complaintId?.let { id ->
                        val cached = complaintDao.getById(id)
                        if (cached != null) {
                            complaintDao.insert(
                                cached.copy(
                                    status = status.name,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    }

                    RealTimeEvent.ComplaintUpdated(
                        complaintId = frame.complaintId ?: "",
                        newStatus   = status,
                        updatedBy   = frame.updatedBy ?: "System",
                        timestamp   = frame.timestamp ?: System.currentTimeMillis()
                    )
                }
                "WORKER_ASSIGNED" -> RealTimeEvent.WorkerAssigned(
                    complaintId = frame.complaintId ?: "",
                    workerId    = frame.workerId ?: "",
                    workerName  = frame.workerName ?: "",
                    timestamp   = frame.timestamp ?: System.currentTimeMillis()
                )
                "PROGRESS_REPORTED" -> RealTimeEvent.ProgressReported(
                    complaintId        = frame.complaintId ?: "",
                    workerId           = frame.workerId ?: "",
                    progressPercentage = frame.progressPercentage ?: 0,
                    note               = frame.note,
                    timestamp          = frame.timestamp ?: System.currentTimeMillis()
                )
                "VERIFICATION_REQUESTED" -> RealTimeEvent.VerificationRequested(
                    complaintId = frame.complaintId ?: "",
                    citizenId   = frame.citizenId ?: "",
                    timestamp   = frame.timestamp ?: System.currentTimeMillis()
                )
                "TASK_COMPLETED" -> RealTimeEvent.TaskCompleted(
                    complaintId = frame.complaintId ?: "",
                    workerId    = frame.workerId ?: "",
                    timestamp   = frame.timestamp ?: System.currentTimeMillis()
                )
                else -> null
            }

            event?.let { _events.emit(it) }
        }
    }

    private fun scheduleReconnect() {
        if (reconnectAttempt > 5) return // Max 5 reconnect attempts
        reconnectAttempt++
        val delayMs = (2000L * reconnectAttempt).coerceAtMost(30000L)
        scope.launch {
            delay(delayMs)
            if (!isConnected) connect()
        }
    }
}
