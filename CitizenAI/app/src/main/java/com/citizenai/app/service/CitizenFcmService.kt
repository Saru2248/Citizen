package com.citizenai.app.service

import android.util.Log
import com.citizenai.app.data.datastore.UserPreferencesDataStore
import com.citizenai.app.data.remote.firebase.FirestoreService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CitizenFcmService"

/**
 * CitizenFcmService — Firebase Cloud Messaging receiver.
 */
@AndroidEntryPoint
class CitizenFcmService : FirebaseMessagingService() {

    @Inject lateinit var firestoreService: FirestoreService
    @Inject lateinit var dataStore: UserPreferencesDataStore

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        registerTokenWithFirestore(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title       = message.notification?.title
            ?: message.data["title"]
            ?: "Citizen AI Update"
        val body        = message.notification?.body
            ?: message.data["body"]
            ?: "You have a new civic update."
        val complaintId = message.data["complaintId"]
        val taskId      = message.data["taskId"]

        Log.d(TAG, "FCM message received — complaintId=$complaintId taskId=$taskId")

        CivicNotificationService.sendNotification(
            context     = applicationContext,
            title       = title,
            body        = body,
            complaintId = complaintId,
            taskId      = taskId
        )
    }

    private fun registerTokenWithFirestore(token: String) {
        serviceScope.launch {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                ?: dataStore.getUserId().firstOrNull()
            if (!userId.isNullOrBlank()) {
                firestoreService.updateFcmToken(userId, token)
            }
        }
    }

    companion object {
        fun getAndRegisterCurrentToken(
            firestoreService: FirestoreService,
            dataStore: UserPreferencesDataStore,
            scope: CoroutineScope,
            userIdParam: String? = null
        ) {
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        if (!token.isNullOrBlank()) {
                            Log.d(TAG, "Fetched FCM token: $token")
                            scope.launch(Dispatchers.IO) {
                                val userId = userIdParam
                                    ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                    ?: dataStore.getUserId().firstOrNull()
                                if (!userId.isNullOrBlank()) {
                                    firestoreService.updateFcmToken(userId, token)
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to fetch FCM token: ${e.message}")
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Exception during FCM token retrieval: ${e.message}")
            }
        }
    }
}
