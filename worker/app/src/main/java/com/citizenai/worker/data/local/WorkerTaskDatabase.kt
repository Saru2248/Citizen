package com.citizenai.worker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WorkerTaskEntity::class], version = 1, exportSchema = false)
abstract class WorkerTaskDatabase : RoomDatabase() {
    abstract fun workerTaskDao(): WorkerTaskDao
}
