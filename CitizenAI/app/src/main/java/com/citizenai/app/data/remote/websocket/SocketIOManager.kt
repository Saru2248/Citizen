package com.citizenai.app.data.remote.websocket

import android.util.Log
import com.citizenai.app.BuildConfig
import com.citizenai.app.data.datastore.UserPreferencesDataStore
import com.citizenai.app.data.local.dao.ComplaintDao
import com.citizenai.app.domain.model.ComplaintStatus
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SocketIOCitizen"

@Singleton
class SocketIOManager @Inject constructor(
    private val dataStore: UserPreferencesDataStore,
    private val complaintDao: ComplaintDao,
    private val gson: Gson
) {
    private var socket: Socket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _events = MutableSharedFlow<RealTimeEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RealTimeEvent> = _events.asSharedFlow()

    private var isConnected = false
    private var currentUserId: String? = null

    fun connect() {
        if (isConnected && socket?.connected() == true) return

        scope.launch {
            val token = dataStore.getAccessToken().firstOrNull()
            val userId = dataStore.getUserId().firstOrNull()
            currentUserId = userId

            val serverUrl = BuildConfig.API_BASE_URL.replace("/api/", "").replace("/api", "")
            Log.d(TAG, "[Socket.IO Citizen] Connecting to: $serverUrl")

            try {
                val opts = IO.Options().apply {
                    transports = arrayOf("websocket", "polling")
                    forceNew = true
                    reconnection = true
                    reconnectionAttempts = 10
                    reconnectionDelay = 2000
                    if (!token.isNull_or_blank()) {
                        query = "token=$token"
                    }
                }

                socket = IO.socket(serverUrl, opts).apply {
                    on(Socket.EVENT_CONNECT) {
                        Log.d(TAG, "[Socket.IO Citizen] Connected")
                        Log.d(TAG, "[Socket.IO Citizen] Authenticated Firebase UID: $userId")
                        isConnected = true

                        // Join user-specific rooms for private targeted updates
                        userId?.let { uid ->
                            val userRoom = "user:$uid"
                            val citizenRoom = "citizen:$uid"
                            emit("join_room", userRoom)
                            emit("join_room", citizenRoom)
                            emit("join", userRoom)
                            emit("join", citizenRoom)
                            Log.d(TAG, "[Socket.IO Citizen] Joined room: $userRoom")
                        }

                        scope.launch { _events.emit(RealTimeEvent.Connected) }
                    }

                    on("complaint_updated") { args ->
                        handleSocketEvent("complaint_updated", args)
                    }

                    on("COMPLAINT_UPDATED") { args ->
                        handleSocketEvent("COMPLAINT_UPDATED", args)
                    }

                    on("complaint_assigned") { args ->
                        handleSocketEvent("complaint_assigned", args)
                    }

                    on("WORKER_ASSIGNED") { args ->
                        handleSocketEvent("WORKER_ASSIGNED", args)
                    }

                    on(Socket.EVENT_DISCONNECT) {
                        Log.d(TAG, "[Socket.IO Citizen] Disconnected")
                        isConnected = false
                    }

                    on(Socket.EVENT_CONNECT_ERROR) { args ->
                        val err = args.firstOrNull()?.toString() ?: "Connection error"
                        Log.e(TAG, "[Socket.IO Citizen] Connection error: $err")
                        isConnected = false
                    }

                    connect()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Socket initialization failed: ${e.message}", e)
            }
        }
    }

    private fun handleSocketEvent(eventName: String, args: Array<Any>) {
        if (args.isEmpty()) return
        val rawData = args[0].toString()
        Log.d(TAG, "[Socket.IO Citizen] $eventName received: $rawData")

        scope.launch {
            try {
                val json = JSONObject(rawData)
                val complaintId = json.optString("complaintId", json.optString("id", ""))
                val statusStr = json.optString("status", json.optString("newStatus", ""))
                val citizenId = json.optString("citizenId", json.optString("userId", ""))
                val firebaseUid = json.optString("firebaseUid", "")
                val workerName = json.optString("assignedWorkerName", json.optString("workerName", ""))

                // Check citizen privacy boundary: if citizenId/firebaseUid is provided, verify against current logged-in user
                if (!currentUserId.isNull_or_blank()) {
                    val matchesFirebaseUid = firebaseUid.isNotBlank() && firebaseUid == currentUserId
                    val matchesCitizenId = citizenId.isNotBlank() && citizenId == currentUserId
                    if (firebaseUid.isNotBlank() && !matchesFirebaseUid) {
                        Log.w(TAG, "[Socket.IO Citizen] Ignored complaint update for another citizen (firebaseUid mismatch): $firebaseUid")
                        return@launch
                    }
                    if (citizenId.isNotBlank() && firebaseUid.isBlank() && !matchesCitizenId && citizenId.length < 20) {
                        Log.w(TAG, "[Socket.IO Citizen] Ignored complaint update for another citizen: $citizenId")
                        return@launch
                    }
                }

                Log.d(TAG, "[Socket.IO Citizen] complaint_updated received")
                Log.d(TAG, "[Socket.IO Citizen] Complaint ID: $complaintId")
                Log.d(TAG, "[Socket.IO Citizen] Status: $statusStr")

                val status = runCatching { ComplaintStatus.valueOf(statusStr.uppercase()) }
                    .getOrDefault(ComplaintStatus.REPORTED)

                // Update Room database cache
                if (complaintId.isNotBlank()) {
                    val cached = complaintDao.getById(complaintId)
                    if (cached != null) {
                        complaintDao.insert(
                            cached.copy(
                                status = status.name,
                                assignedWorkerName = if (workerName.isNotBlank()) workerName else cached.assignedWorkerName,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }

                _events.emit(
                    RealTimeEvent.ComplaintUpdated(
                        complaintId = complaintId,
                        newStatus = status,
                        updatedBy = json.optString("updatedBy", "System"),
                        timestamp = json.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error handling socket payload: ${e.message}", e)
            }
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        isConnected = false
    }

    private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
}
