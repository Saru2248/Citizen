package com.citizenai.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.citizenai.app.data.local.dao.ComplaintDao
import com.citizenai.app.data.local.dao.NotificationDao
import com.citizenai.app.data.local.entity.ComplaintEntity
import com.citizenai.app.data.local.entity.NotificationEntity

/**
 * AppDatabase — Room database for local caching.
 * Exported schema: false (internal app DB, not shared).
 * Version: 1 — increment with migrations for production.
 */
@Database(
    entities = [
        ComplaintEntity::class,
        NotificationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun complaintDao(): ComplaintDao
    abstract fun notificationDao(): NotificationDao
}
