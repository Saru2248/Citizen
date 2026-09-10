package com.citizenai.app.di

import com.citizenai.app.data.repository.AIRepositoryImpl
import com.citizenai.app.data.repository.AdminRepositoryImpl
import com.citizenai.app.data.repository.AuthRepositoryImpl
import com.citizenai.app.data.repository.ComplaintRepositoryImpl
import com.citizenai.app.data.repository.NotificationRepositoryImpl
import com.citizenai.app.data.repository.WorkerRepositoryImpl
import com.citizenai.app.domain.repository.AIRepository
import com.citizenai.app.domain.repository.AdminRepository
import com.citizenai.app.domain.repository.AuthRepository
import com.citizenai.app.domain.repository.ComplaintRepository
import com.citizenai.app.domain.repository.NotificationRepository
import com.citizenai.app.domain.repository.WorkerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * RepositoryModule — binds domain repository interfaces to their implementations.
 * Using @Binds instead of @Provides for efficiency (no instantiation cost).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindComplaintRepository(impl: ComplaintRepositoryImpl): ComplaintRepository

    @Binds @Singleton
    abstract fun bindAIRepository(impl: AIRepositoryImpl): AIRepository

    @Binds @Singleton
    abstract fun bindWorkerRepository(impl: WorkerRepositoryImpl): WorkerRepository

    @Binds @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds @Singleton
    abstract fun bindAdminRepository(impl: AdminRepositoryImpl): AdminRepository
}
