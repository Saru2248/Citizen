package com.citizenai.worker.di

import com.citizenai.worker.data.repository.AuthRepositoryImpl
import com.citizenai.worker.data.repository.WorkerRepositoryImpl
import com.citizenai.worker.domain.repository.AuthRepository
import com.citizenai.worker.domain.repository.WorkerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWorkerRepository(impl: WorkerRepositoryImpl): WorkerRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
