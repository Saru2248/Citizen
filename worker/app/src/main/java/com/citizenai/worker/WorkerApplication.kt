package com.citizenai.worker

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class WorkerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setProjectId("citizen-ai-7d63b")
                .setApplicationId("1:1001811384485:android:01f320021b46965d87154c")
                .setApiKey("AIzaSyB04_noNdFTfw70QDBQjZ-rFF1o-t3DboU")
                .setStorageBucket("citizen-ai-7d63b.firebasestorage.app")
                .build()
            FirebaseApp.initializeApp(this, options)
        }
    }
}
