package com.citizenai.app.di

import android.content.Context
import androidx.room.Room
import com.citizenai.app.data.local.AppDatabase
import com.citizenai.app.data.local.dao.ComplaintDao
import com.citizenai.app.data.local.dao.NotificationDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "citizen_ai_db"
        )
        .fallbackToDestructiveMigration() // Replace with proper migration strategy before production release
        .build()

    @Provides
    @Singleton
    fun provideComplaintDao(db: AppDatabase): ComplaintDao = db.complaintDao()

    @Provides
    @Singleton
    fun provideNotificationDao(db: AppDatabase): NotificationDao = db.notificationDao()
}
