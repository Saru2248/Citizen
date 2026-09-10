package com.citizenai.worker.di

import android.content.Context
import androidx.room.Room
import com.citizenai.worker.data.local.WorkerTaskDao
import com.citizenai.worker.data.local.WorkerTaskDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWorkerTaskDatabase(@ApplicationContext context: Context): WorkerTaskDatabase {
        return Room.databaseBuilder(
            context,
            WorkerTaskDatabase::class.java,
            "worker_tasks_cache.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideWorkerTaskDao(database: WorkerTaskDatabase): WorkerTaskDao {
        return database.workerTaskDao()
    }
}
