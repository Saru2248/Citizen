package com.citizenai.app.data.remote.firebase

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirebaseStorageService"

/**
 * FirebaseStorageService — manages photo uploads to Firebase Storage.
 */
@Singleton
class FirebaseStorageService @Inject constructor(
    private val storage: FirebaseStorage
) {
    suspend fun uploadImage(file: File, path: String): Result<String> {
        return try {
            val uri = Uri.fromFile(file)
            val ref = storage.reference.child(path)
            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Log.d(TAG, "Uploaded image to Firebase Storage: $path -> $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Storage upload failed for path $path: ${e.message}")
            Result.failure(e)
        }
    }
}
