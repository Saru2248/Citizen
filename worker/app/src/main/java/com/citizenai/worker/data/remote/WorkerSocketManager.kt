package com.citizenai.worker.data.remote

import android.util.Log
import com.citizenai.worker.BuildConfig
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

enum class SocketStatus {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR
}

@Singleton
class WorkerSocketManager @Inject constructor() {

    private val TAG = "WorkerSocketManager"
    private var socket: Socket? = null

    private val _status = MutableStateFlow(SocketStatus.DISCONNECTED)
    val status: StateFlow<SocketStatus> = _status.asStateFlow()

    private val _eventFlow = MutableSharedFlow<Pair<String, JSONObject>>(extraBufferCapacity = 64)
    val eventFlow: SharedFlow<Pair<String, JSONObject>> = _eventFlow.asSharedFlow()

    fun connect(token: String?, firebaseUid: String?) {
        if (socket?.connected() == true) return

        try {
            _status.value = SocketStatus.CONNECTING
            Log.d(TAG, "[Worker Socket] Connection requested")

            val options = IO.Options().apply {
                transports = arrayOf("websocket", "polling")
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                timeout = 10000
                if (!token.isNullOrEmpty()) {
                    auth = mapOf("token" to token)
                }
            }

            socket = IO.socket(BuildConfig.SOCKET_URL, options).apply {
                on(Socket.EVENT_CONNECT) {
                    _status.value = SocketStatus.CONNECTED
                    Log.i(TAG, "[WORKER SOCKET] Connected")
                    Log.d(TAG, "[Worker Socket] Authentication successful")
                    Log.d(TAG, "[Worker Socket] Worker UID: ${firebaseUid ?: "Unknown"}")
                    Log.d(TAG, "[Worker Socket] Worker role verified")
                    Log.d(TAG, "[Worker Socket] Joined authorized room: worker:${firebaseUid ?: "authenticated"}")

                    // Re-join server room if needed
                    if (!firebaseUid.isNullOrEmpty()) {
                        emit("join_room", "worker:$firebaseUid")
                    }
                }

                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    _status.value = SocketStatus.ERROR
                    val err = if (args.isNotEmpty()) args[0].toString() else "Unknown error"
                    Log.e(TAG, "[Worker Socket] Connect error: $err")
                }

                on(Socket.EVENT_DISCONNECT) {
                    _status.value = SocketStatus.DISCONNECTED
                    Log.d(TAG, "[Worker Socket] Disconnected")
                }

                on("complaint_updated") { args ->
                    handleIncomingEvent("complaint_updated", args)
                }

                on("worker_task_assigned") { args ->
                    handleIncomingEvent("worker_task_assigned", args)
                }

                on("task_accepted") { args ->
                    handleIncomingEvent("task_accepted", args)
                }

                on("task_started") { args ->
                    handleIncomingEvent("task_started", args)
                }

                on("task_progress") { args ->
                    handleIncomingEvent("task_progress", args)
                }

                on("task_completed") { args ->
                    handleIncomingEvent("task_completed", args)
                }

                connect()
            }
        } catch (e: Exception) {
            _status.value = SocketStatus.ERROR
            Log.e(TAG, "[Worker Socket] Initialization failed: ${e.message}", e)
        }
    }

    private fun handleIncomingEvent(eventName: String, args: Array<Any>) {
        try {
            if (args.isNotEmpty()) {
                val data = when (val obj = args[0]) {
                    is JSONObject -> obj
                    is String -> JSONObject(obj)
                    else -> JSONObject(obj.toString())
                }
                Log.d(TAG, "[Worker Socket] $eventName received: $data")
                _eventFlow.tryEmit(Pair(eventName, data))
            }
        } catch (e: Exception) {
            Log.e(TAG, "[Worker Socket] Error handling event $eventName: ${e.message}")
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _status.value = SocketStatus.DISCONNECTED
    }
}
